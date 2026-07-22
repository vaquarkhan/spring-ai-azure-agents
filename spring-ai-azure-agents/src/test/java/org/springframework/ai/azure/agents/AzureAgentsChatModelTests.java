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
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseUsage;
import com.openai.services.blocking.ConversationService;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.azure.agents.agent.AzureAgentsReferenceManager;
import org.springframework.ai.azure.agents.tool.AzureAgentsToolExecutor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AzureAgentsChatModel}.
 *
 * @author Viquar Khan
 */
@ExtendWith(MockitoExtension.class)
class AzureAgentsChatModelTests {

	@Mock
	private ResponsesClient responsesClient;

	@Mock
	private OpenAIClient openAIClient;

	@Mock
	private ConversationService conversationService;

	@Mock
	private Conversation conversation;

	@Mock
	private AzureAgentsReferenceManager agentReferenceManager;

	@Mock
	private AzureAgentsToolExecutor toolExecutor;

	@Mock
	private Response response;

	@Mock
	private Response followUpResponse;

	@Mock
	private ResponseOutputItem messageItem;

	@Mock
	private ResponseOutputItem functionItem;

	@Mock
	private ResponseOutputMessage outputMessage;

	@Mock
	private ResponseOutputMessage.Content content;

	@Mock
	private ResponseOutputText outputText;

	@Mock
	private ResponseFunctionToolCall functionCall;

	@Mock
	private ResponseUsage usage;

	private AzureAgentsChatModel chatModel;

	@BeforeEach
	void setUp() {
		AzureAgentsChatOptions defaults = AzureAgentsChatOptions.builder()
			.agentName("test-agent")
			.model("gpt-4o")
			.build();
		this.chatModel = new AzureAgentsChatModel(this.responsesClient, this.openAIClient,
				this.agentReferenceManager, this.toolExecutor, defaults, ObservationRegistry.NOOP);
	}

	@Test
	void extractLatestUserTextPrefersLastUserMessage() {
		Prompt prompt = new Prompt(List.of(new SystemMessage("sys"), new UserMessage("first"), new UserMessage("second")));
		assertThat(AzureAgentsChatModel.extractLatestUserText(prompt)).isEqualTo("second");
	}

	@Test
	void extractLatestUserTextFallsBackToContents() {
		Prompt prompt = new Prompt("hello there");
		assertThat(AzureAgentsChatModel.extractLatestUserText(prompt)).isEqualTo("hello there");
	}

	@Test
	void callCreatesConversationWhenMissingAndReturnsAssistantText() {
		when(this.openAIClient.conversations()).thenReturn(this.conversationService);
		when(this.conversationService.create()).thenReturn(this.conversation);
		when(this.conversation.id()).thenReturn("conv-auto");
		when(this.agentReferenceManager.resolve(any(), any(), any(), anyList()))
			.thenReturn(new AgentReference("test-agent").setVersion("1"));

		stubTextResponse(this.response, "resp-1", "Hello from Foundry");
		when(this.responsesClient.createAzureResponse(any(AzureCreateResponseOptions.class),
				any(ResponseCreateParams.Builder.class))).thenReturn(this.response);

		ChatResponse chatResponse = this.chatModel.call(new Prompt("Hi"));
		assertThat(chatResponse.getResult().getOutput().getText()).isEqualTo("Hello from Foundry");
		assertThat(chatResponse.getMetadata().getId()).isEqualTo("resp-1");
	}

	@Test
	void callUsesExistingConversationIdFromOptions() {
		when(this.agentReferenceManager.resolve(any(), any(), any(), anyList()))
			.thenReturn(new AgentReference("test-agent").setVersion("1"));
		stubTextResponse(this.response, "resp-2", "ok");
		when(this.responsesClient.createAzureResponse(any(AzureCreateResponseOptions.class),
				any(ResponseCreateParams.Builder.class))).thenReturn(this.response);

		AzureAgentsChatOptions options = AzureAgentsChatOptions.builder().conversationId("conv-fixed").build();
		this.chatModel.call(new Prompt(new UserMessage("ping"), options));

		ArgumentCaptor<ResponseCreateParams.Builder> captor = ArgumentCaptor.forClass(ResponseCreateParams.Builder.class);
		verify(this.responsesClient).createAzureResponse(any(AzureCreateResponseOptions.class), captor.capture());
		assertThat(captor.getValue().build().conversation()).isPresent();
	}

	@Test
	void callExecutesToolRoundTrip() {
		when(this.agentReferenceManager.resolve(any(), any(), any(), anyList()))
			.thenReturn(new AgentReference("test-agent").setVersion("1"));

		when(this.response.id()).thenReturn("resp-tool");
		when(this.response.output()).thenReturn(List.of(this.functionItem));
		when(this.functionItem.isFunctionCall()).thenReturn(true);
		when(this.functionItem.asFunctionCall()).thenReturn(this.functionCall);

		stubTextResponse(this.followUpResponse, "resp-final", "Weather is fine");
		when(this.followUpResponse.output()).thenReturn(List.of(this.messageItem));

		when(this.responsesClient.createAzureResponse(any(AzureCreateResponseOptions.class),
				any(ResponseCreateParams.Builder.class))).thenReturn(this.response, this.followUpResponse);

		when(this.toolExecutor.execute(anyList(), anyList(), anyMap())).thenReturn(List.of());

		ToolCallback callback = simpleTool("get_weather");
		AzureAgentsChatOptions options = AzureAgentsChatOptions.builder()
			.conversationId("conv-1")
			.toolCallbacks(List.of(callback))
			.maxToolRounds(3)
			.build();

		ChatResponse chatResponse = this.chatModel.call(new Prompt(new UserMessage("weather?"), options));
		assertThat(chatResponse.getResult().getOutput().getText()).isEqualTo("Weather is fine");
		verify(this.toolExecutor, times(1)).execute(anyList(), anyList(), anyMap());
		verify(this.responsesClient, times(2)).createAzureResponse(any(AzureCreateResponseOptions.class),
				any(ResponseCreateParams.Builder.class));
	}

	@Test
	void callFailsWhenMaxToolRoundsExceeded() {
		when(this.agentReferenceManager.resolve(any(), any(), any(), anyList()))
			.thenReturn(new AgentReference("test-agent").setVersion("1"));
		when(this.response.id()).thenReturn("resp-loop");
		when(this.response.output()).thenReturn(List.of(this.functionItem));
		when(this.functionItem.isFunctionCall()).thenReturn(true);
		when(this.functionItem.asFunctionCall()).thenReturn(this.functionCall);
		when(this.responsesClient.createAzureResponse(any(AzureCreateResponseOptions.class),
				any(ResponseCreateParams.Builder.class))).thenReturn(this.response);
		when(this.toolExecutor.execute(anyList(), anyList(), anyMap())).thenReturn(List.of());

		AzureAgentsChatOptions options = AzureAgentsChatOptions.builder()
			.conversationId("conv-1")
			.maxToolRounds(1)
			.build();

		assertThatThrownBy(() -> this.chatModel.call(new Prompt(new UserMessage("loop"), options)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Exceeded max tool rounds");
	}

	@Test
	void streamEmitsSingleChatResponse() {
		when(this.openAIClient.conversations()).thenReturn(this.conversationService);
		when(this.conversationService.create()).thenReturn(this.conversation);
		when(this.conversation.id()).thenReturn("conv-stream");
		when(this.agentReferenceManager.resolve(any(), any(), any(), anyList()))
			.thenReturn(new AgentReference("test-agent").setVersion("1"));
		stubTextResponse(this.response, "resp-s", "streamed");
		when(this.responsesClient.createAzureResponse(any(AzureCreateResponseOptions.class),
				any(ResponseCreateParams.Builder.class))).thenReturn(this.response);

		StepVerifier.create(this.chatModel.stream(new Prompt("hi")))
			.assertNext(r -> assertThat(r.getResult().getOutput().getText()).isEqualTo("streamed"))
			.verifyComplete();
	}

	@Test
	void toChatResponseMapsUsageAndMetadata() {
		when(this.response.id()).thenReturn("resp-m");
		when(this.response.output()).thenReturn(List.of(this.messageItem));
		when(this.messageItem.isMessage()).thenReturn(true);
		when(this.messageItem.asMessage()).thenReturn(this.outputMessage);
		when(this.outputMessage.content()).thenReturn(List.of(this.content));
		when(this.content.outputText()).thenReturn(Optional.of(this.outputText));
		when(this.outputText.text()).thenReturn("text");
		when(this.usage.inputTokens()).thenReturn(10L);
		when(this.usage.outputTokens()).thenReturn(5L);
		when(this.usage.totalTokens()).thenReturn(15L);
		when(this.response.usage()).thenReturn(Optional.of(this.usage));
		when(this.response.conversation()).thenReturn(Optional.empty());

		AzureAgentsChatOptions options = AzureAgentsChatOptions.builder()
			.model("gpt-4o")
			.conversationId("conv-m")
			.build();
		ChatResponse chatResponse = AzureAgentsChatModel.toChatResponse(this.response, options);
		Integer promptTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
		Integer completionTokens = chatResponse.getMetadata().getUsage().getCompletionTokens();
		Object conversationId = chatResponse.getMetadata().get(AzureAgentsConstants.CONVERSATION_ID);
		assertThat(promptTokens).isEqualTo(10);
		assertThat(completionTokens).isEqualTo(5);
		assertThat(conversationId).isEqualTo("conv-m");
	}

	private void stubTextResponse(Response target, String id, String text) {
		when(target.id()).thenReturn(id);
		when(target.output()).thenReturn(List.of(this.messageItem));
		when(this.messageItem.isFunctionCall()).thenReturn(false);
		when(this.messageItem.isMessage()).thenReturn(true);
		when(this.messageItem.asMessage()).thenReturn(this.outputMessage);
		when(this.outputMessage.content()).thenReturn(List.of(this.content));
		when(this.content.outputText()).thenReturn(Optional.of(this.outputText));
		when(this.outputText.text()).thenReturn(text);
		when(target.usage()).thenReturn(Optional.empty());
		when(target.conversation()).thenReturn(Optional.empty());
	}

	private static ToolCallback simpleTool(String name) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description(name)
					.inputSchema("{\"type\":\"object\",\"properties\":{}}")
					.build();
			}

			@Override
			public String call(String toolInput) {
				return "{}";
			}
		};
	}

}
