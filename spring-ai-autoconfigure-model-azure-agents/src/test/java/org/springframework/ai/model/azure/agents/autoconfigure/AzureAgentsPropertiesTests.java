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

package org.springframework.ai.model.azure.agents.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AzureAgentsProperties}.
 *
 * @author Viquar Khan
 */
class AzureAgentsPropertiesTests {

	@Test
	void defaults() {
		AzureAgentsProperties properties = new AzureAgentsProperties();
		assertThat(properties.isEnabled()).isTrue();
		assertThat(properties.getAgentName()).isEqualTo("spring-ai-agent");
		assertThat(properties.getInstructions()).contains("helpful");
		assertThat(properties.isCreateAgentOnDemand()).isTrue();
		assertThat(properties.isCodeInterpreterEnabled()).isFalse();
		assertThat(properties.getFileSearchVectorStoreIds()).isEmpty();
		assertThat(properties.getMaxToolRounds()).isEqualTo(8);
		assertThat(properties.isStrictFunctionTools()).isTrue();
		assertThat(AzureAgentsProperties.CONFIG_PREFIX).isEqualTo("spring.ai.azure.agents");
	}

	@Test
	void setters() {
		AzureAgentsProperties properties = new AzureAgentsProperties();
		properties.setEnabled(false);
		properties.setEndpoint("https://example.services.ai.azure.com/api/projects/p");
		properties.setAgentName("demo");
		properties.setAgentVersion("3");
		properties.setModelDeploymentName("gpt-4o");
		properties.setInstructions("Custom");
		properties.setCreateAgentOnDemand(false);
		properties.setCodeInterpreterEnabled(true);
		properties.setFileSearchVectorStoreIds(java.util.List.of("vs-1"));
		properties.setMaxToolRounds(2);
		properties.setStrictFunctionTools(false);

		assertThat(properties.isEnabled()).isFalse();
		assertThat(properties.getEndpoint()).contains("example");
		assertThat(properties.getAgentName()).isEqualTo("demo");
		assertThat(properties.getAgentVersion()).isEqualTo("3");
		assertThat(properties.getModelDeploymentName()).isEqualTo("gpt-4o");
		assertThat(properties.getInstructions()).isEqualTo("Custom");
		assertThat(properties.isCreateAgentOnDemand()).isFalse();
		assertThat(properties.isCodeInterpreterEnabled()).isTrue();
		assertThat(properties.getFileSearchVectorStoreIds()).containsExactly("vs-1");
		assertThat(properties.getMaxToolRounds()).isEqualTo(2);
		assertThat(properties.isStrictFunctionTools()).isFalse();
	}

}
