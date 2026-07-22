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
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AzureAgentsChatOptions}.
 *
 * @author Viquar Khan
 */
class AzureAgentsChatOptionsTests {

	@Test
	void builderSetsAgentFields() {
		AzureAgentsChatOptions options = AzureAgentsChatOptions.builder()
			.agentName("agent-a")
			.agentVersion("1")
			.conversationId("conv-1")
			.instructions("Be helpful")
			.maxToolRounds(3)
			.model("gpt-4o")
			.temperature(0.2)
			.build();

		assertThat(options.getAgentName()).isEqualTo("agent-a");
		assertThat(options.getAgentVersion()).isEqualTo("1");
		assertThat(options.getConversationId()).isEqualTo("conv-1");
		assertThat(options.getInstructions()).isEqualTo("Be helpful");
		assertThat(options.getMaxToolRounds()).isEqualTo(3);
		assertThat(options.getModel()).isEqualTo("gpt-4o");
		assertThat(options.getTemperature()).isEqualTo(0.2);
		assertThat(options.getInternalToolExecutionEnabled()).isFalse();
	}

	@Test
	void copyIsIndependent() {
		AzureAgentsChatOptions original = AzureAgentsChatOptions.builder().agentName("a").build();
		AzureAgentsChatOptions copy = original.copy();
		copy.setAgentName("b");
		assertThat(original.getAgentName()).isEqualTo("a");
		assertThat(copy.getAgentName()).isEqualTo("b");
	}

	@Test
	void mergePrefersRuntimeValues() {
		AzureAgentsChatOptions defaults = AzureAgentsChatOptions.builder()
			.agentName("default-agent")
			.instructions("default")
			.model("gpt-4o")
			.build();

		AzureAgentsChatOptions runtime = AzureAgentsChatOptions.builder()
			.agentName("runtime-agent")
			.conversationId("c-9")
			.build();

		AzureAgentsChatOptions merged = AzureAgentsChatOptions.merge(runtime, defaults);
		assertThat(merged.getAgentName()).isEqualTo("runtime-agent");
		assertThat(merged.getConversationId()).isEqualTo("c-9");
		assertThat(merged.getInstructions()).isEqualTo("default");
		assertThat(merged.getModel()).isEqualTo("gpt-4o");
	}

	@Test
	void mergePullsToolCallbacksFromToolCallingOptions() {
		ToolCallback callback = new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name("t1")
					.description("d")
					.inputSchema("{\"type\":\"object\",\"properties\":{}}")
					.build();
			}

			@Override
			public String call(String toolInput) {
				return "{}";
			}
		};

		ToolCallingChatOptions runtime = DefaultToolCallingChatOptions.builder()
			.toolCallbacks(List.of(callback))
			.toolNames(Set.of("t1"))
			.build();

		AzureAgentsChatOptions merged = AzureAgentsChatOptions.merge(runtime,
				AzureAgentsChatOptions.builder().agentName("a").build());
		assertThat(merged.getToolCallbacks()).hasSize(1);
		assertThat(merged.getToolNames()).contains("t1");
		assertThat(merged.getInternalToolExecutionEnabled()).isFalse();
	}

	@Test
	void equalsAndHashCode() {
		AzureAgentsChatOptions a = AzureAgentsChatOptions.builder().agentName("x").conversationId("c").build();
		AzureAgentsChatOptions b = AzureAgentsChatOptions.builder().agentName("x").conversationId("c").build();
		AzureAgentsChatOptions c = AzureAgentsChatOptions.builder().agentName("y").conversationId("c").build();
		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
	}

}
