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

package org.springframework.ai.azure.agents.advisor;

import java.util.UUID;

import com.openai.client.OpenAIClient;
import com.openai.models.conversations.Conversation;
import org.springframework.ai.azure.agents.AzureAgentsChatOptions;
import org.springframework.ai.azure.agents.AzureAgentsConstants;
import org.springframework.ai.azure.agents.conversation.ConversationIdRepository;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Ensures each ChatClient turn is bound to a Foundry conversation id.
 * <p>
 * Resolves {@link AzureAgentsConstants#SESSION_ID} (or generates one), maps it through
 * {@link ConversationIdRepository}, creates a remote conversation when needed, and injects
 * the id into {@link AzureAgentsChatOptions} before the {@code ChatModel} runs.
 * <p>
 * Does <strong>not</strong> hijack model execution, unlike classic thread-polling advisors.
 *
 * @author Viquar Khan
 */
public class AzureConversationIdAdvisor implements CallAdvisor, StreamAdvisor {

	private final OpenAIClient openAIClient;

	private final ConversationIdRepository conversationIdRepository;

	public AzureConversationIdAdvisor(OpenAIClient openAIClient,
			ConversationIdRepository conversationIdRepository) {
		Assert.notNull(openAIClient, "openAIClient must not be null");
		Assert.notNull(conversationIdRepository, "conversationIdRepository must not be null");
		this.openAIClient = openAIClient;
		this.conversationIdRepository = conversationIdRepository;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		return chain.nextCall(enrich(request));
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
		return Mono.fromCallable(() -> enrich(request))
			.subscribeOn(Schedulers.boundedElastic())
			.flatMapMany(chain::nextStream);
	}

	private ChatClientRequest enrich(ChatClientRequest request) {
		Prompt prompt = request.prompt();
		ChatOptions options = prompt.getOptions();

		AzureAgentsChatOptions azureOptions;
		if (options instanceof AzureAgentsChatOptions existing) {
			azureOptions = AzureAgentsChatOptions.fromOptions(existing);
		}
		else {
			azureOptions = AzureAgentsChatOptions.builder().build();
			if (options != null) {
				azureOptions = AzureAgentsChatOptions.merge(options, azureOptions);
			}
		}

		if (StringUtils.hasText(azureOptions.getConversationId())) {
			return request.mutate()
				.prompt(withOptions(prompt, azureOptions))
				.context(AzureAgentsConstants.CONVERSATION_ID, azureOptions.getConversationId())
				.build();
		}

		Object existingConversation = request.context().get(AzureAgentsConstants.CONVERSATION_ID);
		if (existingConversation instanceof String conversationId && StringUtils.hasText(conversationId)) {
			azureOptions.setConversationId(conversationId);
			return request.mutate().prompt(withOptions(prompt, azureOptions)).build();
		}

		String sessionId = resolveSessionId(request);
		String conversationId = this.conversationIdRepository.findConversationId(sessionId);
		if (!StringUtils.hasText(conversationId)) {
			Conversation conversation = this.openAIClient.conversations().create();
			conversationId = conversation.id();
			this.conversationIdRepository.save(sessionId, conversationId);
		}

		azureOptions.setConversationId(conversationId);
		return request.mutate()
			.prompt(withOptions(prompt, azureOptions))
			.context(AzureAgentsConstants.SESSION_ID, sessionId)
			.context(AzureAgentsConstants.CONVERSATION_ID, conversationId)
			.build();
	}

	private static Prompt withOptions(Prompt prompt, AzureAgentsChatOptions options) {
		return new Prompt(prompt.getInstructions(), options);
	}

	private static String resolveSessionId(ChatClientRequest request) {
		Object session = request.context().get(AzureAgentsConstants.SESSION_ID);
		if (session instanceof String sessionId && StringUtils.hasText(sessionId)) {
			return sessionId;
		}
		return UUID.randomUUID().toString();
	}

	@Override
	public String getName() {
		return "AzureConversationIdAdvisor";
	}

	@Override
	public int getOrder() {
		// Run after ChatMemory advisors so we can still inject conversation id just before
		// the model; AzureAgentsChatModel only sends the latest user turn.
		return Ordered.LOWEST_PRECEDENCE;
	}

}
