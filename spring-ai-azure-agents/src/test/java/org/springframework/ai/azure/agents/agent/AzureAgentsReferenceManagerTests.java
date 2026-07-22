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

package org.springframework.ai.azure.agents.agent;

import java.util.List;

import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.models.AgentReference;
import com.azure.ai.agents.models.AgentVersionDetails;
import com.azure.ai.agents.models.PromptAgentDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.azure.agents.tool.AzureFunctionToolFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AzureAgentsReferenceManager}.
 *
 * @author Viquar Khan
 */
@ExtendWith(MockitoExtension.class)
class AzureAgentsReferenceManagerTests {

	@Mock
	private AgentsClient agentsClient;

	@Mock
	private AgentVersionDetails versionDetails;

	private AzureFunctionToolFactory functionToolFactory;

	@BeforeEach
	void setUp() {
		this.functionToolFactory = new AzureFunctionToolFactory(new ObjectMapper());
	}

	@Test
	void usesConfiguredVersionWithoutCreating() {
		AzureAgentsReferenceManager manager = new AzureAgentsReferenceManager(this.agentsClient, this.functionToolFactory,
				"my-agent", "gpt-4o", "hi", false, List.of(), true, "v42");

		AgentReference reference = manager.resolve(null, null, null, List.of());
		assertThat(reference.getName()).isEqualTo("my-agent");
		assertThat(reference.getVersion()).isEqualTo("v42");
	}

	@Test
	void runtimeVersionOverridesConfigured() {
		AzureAgentsReferenceManager manager = new AzureAgentsReferenceManager(this.agentsClient, this.functionToolFactory,
				"my-agent", "gpt-4o", "hi", false, List.of(), true, "v1");

		AgentReference reference = manager.resolve("other", "v9", null, List.of());
		assertThat(reference.getName()).isEqualTo("other");
		assertThat(reference.getVersion()).isEqualTo("v9");
	}

	@Test
	void createsAndCachesAgentVersion() {
		when(this.versionDetails.getName()).thenReturn("my-agent");
		when(this.versionDetails.getVersion()).thenReturn("created-1");
		when(this.agentsClient.createAgentVersion(eq("my-agent"), any(PromptAgentDefinition.class)))
			.thenReturn(this.versionDetails);

		AzureAgentsReferenceManager manager = new AzureAgentsReferenceManager(this.agentsClient, this.functionToolFactory,
				"my-agent", "gpt-4o", "Be helpful", true, List.of("vs-1"), true, null);

		ToolCallback tool = tool("t1");
		AgentReference first = manager.resolve(null, null, null, List.of(tool));
		AgentReference second = manager.resolve(null, null, null, List.of(tool));

		assertThat(first.getVersion()).isEqualTo("created-1");
		assertThat(second.getVersion()).isEqualTo("created-1");
		verify(this.agentsClient, times(1)).createAgentVersion(eq("my-agent"), any(PromptAgentDefinition.class));

		ArgumentCaptor<PromptAgentDefinition> definitionCaptor = ArgumentCaptor.forClass(PromptAgentDefinition.class);
		verify(this.agentsClient).createAgentVersion(eq("my-agent"), definitionCaptor.capture());
		assertThat(definitionCaptor.getValue().getInstructions()).isEqualTo("Be helpful");
		assertThat(definitionCaptor.getValue().getTools()).hasSize(3);
	}

	@Test
	void failsWhenCreateDisabledAndNoVersion() {
		AzureAgentsReferenceManager manager = new AzureAgentsReferenceManager(this.agentsClient, this.functionToolFactory,
				"my-agent", "gpt-4o", "hi", false, List.of(), false, null);

		assertThatThrownBy(() -> manager.resolve(null, null, null, List.of()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("create-on-demand is disabled");
	}

	private static ToolCallback tool(String name) {
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
