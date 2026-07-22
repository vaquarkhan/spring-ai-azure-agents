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

import java.util.Map;

import com.openai.client.OpenAIClient;
import com.openai.models.conversations.Conversation;
import com.openai.services.blocking.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.azure.agents.AzureAgentsChatOptions;
import org.springframework.ai.azure.agents.AzureAgentsConstants;
import org.springframework.ai.azure.agents.conversation.ConversationIdRepository;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AzureConversationIdAdvisor}.
 *
 * @author Viquar Khan
 */
@ExtendWith(MockitoExtension.class)
class AzureConversationIdAdvisorTests {

	@Mock
	private OpenAIClient openAIClient;

	@Mock
	private ConversationService conversationService;

	@Mock
	private Conversation conversation;

	@Mock
	private ConversationIdRepository repository;

	@Mock
	private CallAdvisorChain callChain;

	@Mock
	private StreamAdvisorChain streamChain;

	private AzureConversationIdAdvisor advisor;

	@BeforeEach
	void setUp() {
		this.advisor = new AzureConversationIdAdvisor(this.openAIClient, this.repository);
	}

	@Test
	void reusesExistingConversationFromOptions() {
		AzureAgentsChatOptions options = AzureAgentsChatOptions.builder().conversationId("conv-existing").build();
		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(new Prompt("hi", options))
			.context(Map.of())
			.build();
		when(this.callChain.nextCall(any())).thenAnswer(inv -> ChatClientResponse.builder()
			.chatResponse(new ChatResponse(java.util.List.of()))
			.context(Map.of())
			.build());

		this.advisor.adviseCall(request, this.callChain);

		ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
		verify(this.callChain).nextCall(captor.capture());
		AzureAgentsChatOptions enriched = (AzureAgentsChatOptions) captor.getValue().prompt().getOptions();
		assertThat(enriched.getConversationId()).isEqualTo("conv-existing");
		verify(this.repository, never()).save(any(), any());
	}

	@Test
	void createsConversationForNewSession() {
		when(this.repository.findConversationId("session-1")).thenReturn(null);
		when(this.openAIClient.conversations()).thenReturn(this.conversationService);
		when(this.conversationService.create()).thenReturn(this.conversation);
		when(this.conversation.id()).thenReturn("conv-new");
		when(this.callChain.nextCall(any())).thenAnswer(inv -> ChatClientResponse.builder()
			.chatResponse(new ChatResponse(java.util.List.of()))
			.context(Map.of())
			.build());

		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(new Prompt("hi"))
			.context(Map.of(AzureAgentsConstants.SESSION_ID, "session-1"))
			.build();

		this.advisor.adviseCall(request, this.callChain);

		verify(this.repository).save(eq("session-1"), eq("conv-new"));
		ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
		verify(this.callChain).nextCall(captor.capture());
		assertThat(captor.getValue().context()).containsEntry(AzureAgentsConstants.CONVERSATION_ID, "conv-new");
	}

	@Test
	void loadsMappedConversationForKnownSession() {
		when(this.repository.findConversationId("session-2")).thenReturn("conv-mapped");
		when(this.callChain.nextCall(any())).thenAnswer(inv -> ChatClientResponse.builder()
			.chatResponse(new ChatResponse(java.util.List.of()))
			.context(Map.of())
			.build());

		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(new Prompt("hi"))
			.context(Map.of(AzureAgentsConstants.SESSION_ID, "session-2"))
			.build();

		this.advisor.adviseCall(request, this.callChain);
		verify(this.repository, never()).save(any(), any());
		ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
		verify(this.callChain).nextCall(captor.capture());
		AzureAgentsChatOptions options = (AzureAgentsChatOptions) captor.getValue().prompt().getOptions();
		assertThat(options.getConversationId()).isEqualTo("conv-mapped");
	}

	@Test
	void streamAdvisorEnrichesRequest() {
		when(this.repository.findConversationId(any())).thenReturn("conv-stream");
		when(this.streamChain.nextStream(any())).thenReturn(Flux.just(ChatClientResponse.builder()
			.chatResponse(new ChatResponse(java.util.List.of()))
			.context(Map.of())
			.build()));

		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(new Prompt("hi"))
			.context(Map.of(AzureAgentsConstants.SESSION_ID, "s-stream"))
			.build();

		StepVerifier.create(this.advisor.adviseStream(request, this.streamChain)).expectNextCount(1).verifyComplete();
		verify(this.streamChain).nextStream(any());
	}

	@Test
	void metadata() {
		assertThat(this.advisor.getName()).isEqualTo("AzureConversationIdAdvisor");
		assertThat(this.advisor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

}
