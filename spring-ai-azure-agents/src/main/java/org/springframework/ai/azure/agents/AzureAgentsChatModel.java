/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.azure.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.AgentReference;
import com.azure.ai.agents.models.AzureCreateResponseOptions;
import com.openai.client.OpenAIClient;
import com.openai.models.conversations.Conversation;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseUsage;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.azure.agents.agent.AzureAgentsReferenceManager;
import org.springframework.ai.azure.agents.tool.AzureAgentsToolExecutor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * {@link ChatModel} that delegates reasoning to Microsoft Foundry Agent Service via the
 * Conversations / Responses APIs.
 * <p>
 * Server-side conversations hold history; this model sends only the latest user turn when
 * a conversation id is present, then locally executes Spring {@link ToolCallback}s when
 * the agent returns function calls.
 *
 * @author Viquar Khan
 */
public class AzureAgentsChatModel implements ChatModel {

	private static final Logger logger = LoggerFactory.getLogger(AzureAgentsChatModel.class);

	private static final int DEFAULT_MAX_TOOL_ROUNDS = 8;

	private final ResponsesClient responsesClient;

	private final OpenAIClient openAIClient;

	private final AzureAgentsReferenceManager agentReferenceManager;

	private final AzureAgentsToolExecutor toolExecutor;

	private final AzureAgentsChatOptions defaultOptions;

	private final ObservationRegistry observationRegistry;

	public AzureAgentsChatModel(ResponsesClient responsesClient, OpenAIClient openAIClient,
			AzureAgentsReferenceManager agentReferenceManager, AzureAgentsToolExecutor toolExecutor,
			AzureAgentsChatOptions defaultOptions, ObservationRegistry observationRegistry) {
		Assert.notNull(responsesClient, "responsesClient must not be null");
		Assert.notNull(openAIClient, "openAIClient must not be null");
		Assert.notNull(agentReferenceManager, "agentReferenceManager must not be null");
		Assert.notNull(toolExecutor, "toolExecutor must not be null");
		Assert.notNull(defaultOptions, "defaultOptions must not be null");
		this.responsesClient = responsesClient;
		this.openAIClient = openAIClient;
		this.agentReferenceManager = agentReferenceManager;
		this.toolExecutor = toolExecutor;
		this.defaultOptions = defaultOptions;
		this.observationRegistry = observationRegistry != null ? observationRegistry : ObservationRegistry.NOOP;
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		Assert.notNull(prompt, "prompt must not be null");
		AzureAgentsChatOptions options = AzureAgentsChatOptions.merge(prompt.getOptions(), this.defaultOptions);

		Observation observation = Observation.createNotStarted(AzureAgentsConstants.OBSERVATION_NAME,
				this.observationRegistry)
			.lowCardinalityKeyValue("agent.name",
					StringUtils.hasText(options.getAgentName()) ? options.getAgentName()
							: this.defaultOptions.getAgentName() != null ? this.defaultOptions.getAgentName()
									: "default");

		try {
			return observation.observe(() -> doCall(prompt, options, observation));
		}
		catch (RuntimeException ex) {
			throw ex;
		}
	}

	private ChatResponse doCall(Prompt prompt, AzureAgentsChatOptions options, Observation observation) {
		String userText = extractLatestUserText(prompt);
		Assert.hasText(userText, "Prompt must contain a user message");

		String conversationId = resolveConversationId(options);
		if (StringUtils.hasText(conversationId)) {
			observation.highCardinalityKeyValue(AzureAgentsConstants.CONVERSATION_ID, conversationId);
		}

		List<ToolCallback> toolCallbacks = options.getToolCallbacks() != null ? options.getToolCallbacks()
				: List.of();
		AgentReference agentReference = this.agentReferenceManager.resolve(options.getAgentName(),
				options.getAgentVersion(), options.getInstructions(), toolCallbacks);

		AzureCreateResponseOptions azureOptions = new AzureCreateResponseOptions()
			.setAgentReference(agentReference);

		ResponseCreateParams.Builder params = ResponseCreateParams.builder().input(userText);
		if (StringUtils.hasText(conversationId)) {
			params.conversation(conversationId);
		}
		if (options.getTemperature() != null) {
			params.temperature(options.getTemperature());
		}
		if (options.getTopP() != null) {
			params.topP(options.getTopP());
		}
		if (options.getMaxTokens() != null) {
			params.maxOutputTokens(options.getMaxTokens().longValue());
		}

		Response response = this.responsesClient.createAzureResponse(azureOptions, params);
		observation.highCardinalityKeyValue("azure.response.id", response.id());

		int maxRounds = options.getMaxToolRounds() != null ? options.getMaxToolRounds() : DEFAULT_MAX_TOOL_ROUNDS;
		int round = 0;
		while (round < maxRounds) {
			List<ResponseFunctionToolCall> functionCalls = extractFunctionCalls(response);
			if (functionCalls.isEmpty()) {
				break;
			}
			logger.debug("Executing {} local tool call(s) for response {}", functionCalls.size(), response.id());
			List<ResponseInputItem> toolOutputs = this.toolExecutor.execute(functionCalls, toolCallbacks,
					options.getToolContext());

			ResponseCreateParams.Builder followUp = ResponseCreateParams.builder()
				.previousResponseId(response.id())
				.inputOfResponse(toolOutputs);
			if (StringUtils.hasText(conversationId)) {
				followUp.conversation(conversationId);
			}
			response = this.responsesClient.createAzureResponse(azureOptions, followUp);
			round++;
		}

		if (round >= maxRounds && !extractFunctionCalls(response).isEmpty()) {
			throw new IllegalStateException("Exceeded max tool rounds (" + maxRounds + ") for Azure agent response");
		}

		observation.lowCardinalityKeyValue("tools.invoked.rounds", String.valueOf(round));
		return toChatResponse(response, options);
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {
		// Foundry streaming + local tool loops are handled as a single reactive unit for
		// MVP; token-level SSE can be added later via createStreamingAzureResponse.
		return Flux.defer(() -> Flux.just(call(prompt))).subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public ChatOptions getDefaultOptions() {
		return this.defaultOptions.copy();
	}

	private String resolveConversationId(AzureAgentsChatOptions options) {
		if (StringUtils.hasText(options.getConversationId())) {
			return options.getConversationId();
		}
		Conversation conversation = this.openAIClient.conversations().create();
		return conversation.id();
	}

	static String extractLatestUserText(Prompt prompt) {
		List<UserMessage> userMessages = prompt.getUserMessages();
		if (!CollectionUtils.isEmpty(userMessages)) {
			return userMessages.get(userMessages.size() - 1).getText();
		}
		List<Message> instructions = prompt.getInstructions();
		if (!CollectionUtils.isEmpty(instructions)) {
			Message last = instructions.get(instructions.size() - 1);
			if (last instanceof UserMessage userMessage) {
				return userMessage.getText();
			}
			return last.getText();
		}
		return prompt.getContents();
	}

	static List<ResponseFunctionToolCall> extractFunctionCalls(Response response) {
		List<ResponseFunctionToolCall> calls = new ArrayList<>();
		for (ResponseOutputItem item : response.output()) {
			if (item.isFunctionCall()) {
				calls.add(item.asFunctionCall());
			}
		}
		return calls;
	}

	static String extractAssistantText(Response response) {
		StringBuilder sb = new StringBuilder();
		for (ResponseOutputItem item : response.output()) {
			if (item.isMessage()) {
				ResponseOutputMessage message = item.asMessage();
				for (ResponseOutputMessage.Content content : message.content()) {
					Optional<ResponseOutputText> text = content.outputText();
					text.ifPresent(outputText -> {
						if (!sb.isEmpty()) {
							sb.append('\n');
						}
						sb.append(outputText.text());
					});
				}
			}
		}
		return sb.toString();
	}

	static ChatResponse toChatResponse(Response response, AzureAgentsChatOptions options) {
		String text = extractAssistantText(response);
		AssistantMessage assistantMessage = new AssistantMessage(text);
		Generation generation = new Generation(assistantMessage);

		ChatResponseMetadata.Builder metadata = ChatResponseMetadata.builder().id(response.id());
		if (options.getModel() != null) {
			metadata.model(options.getModel());
		}
		else {
			try {
				metadata.model(response.model().toString());
			}
			catch (Exception ignored) {
				// model may be a complex union type
			}
		}
		response.usage().ifPresent(usage -> metadata.usage(toUsage(usage)));
		if (StringUtils.hasText(options.getConversationId())) {
			metadata.keyValue(AzureAgentsConstants.CONVERSATION_ID, options.getConversationId());
		}
		response.conversation()
			.ifPresent(conversation -> metadata.keyValue(AzureAgentsConstants.CONVERSATION_ID, conversation.id()));

		return new ChatResponse(List.of(generation), metadata.build());
	}

	private static DefaultUsage toUsage(ResponseUsage usage) {
		return new DefaultUsage((int) usage.inputTokens(), (int) usage.outputTokens(), (int) usage.totalTokens(),
				usage);
	}

}
