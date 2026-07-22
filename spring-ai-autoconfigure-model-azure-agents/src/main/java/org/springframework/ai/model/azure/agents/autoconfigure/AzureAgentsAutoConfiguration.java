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

import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.AgentsClientBuilder;
import com.azure.ai.agents.ResponsesClient;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.azure.agents.AzureAgentsChatModel;
import org.springframework.ai.azure.agents.AzureAgentsChatOptions;
import org.springframework.ai.azure.agents.advisor.AzureConversationIdAdvisor;
import org.springframework.ai.azure.agents.agent.AzureAgentsReferenceManager;
import org.springframework.ai.azure.agents.conversation.ConversationIdRepository;
import org.springframework.ai.azure.agents.conversation.InMemoryConversationIdRepository;
import org.springframework.ai.azure.agents.tool.AzureAgentsToolExecutor;
import org.springframework.ai.azure.agents.tool.AzureFunctionToolFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for Microsoft Foundry Agent Service + Spring AI.
 *
 * @author Viquar Khan
 */
@AutoConfiguration
@ConditionalOnClass({ AgentsClientBuilder.class, AzureAgentsChatModel.class })
@EnableConfigurationProperties(AzureAgentsProperties.class)
@ConditionalOnProperty(prefix = AzureAgentsProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class AzureAgentsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AgentsClientBuilder azureAgentsClientBuilder(AzureAgentsProperties properties) {
		Assert.hasText(properties.getEndpoint(), "spring.ai.azure.agents.endpoint must be set");
		return new AgentsClientBuilder().endpoint(properties.getEndpoint())
			.credential(new DefaultAzureCredentialBuilder().build());
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentsClient azureAgentsClient(AgentsClientBuilder builder) {
		return builder.buildAgentsClient();
	}

	@Bean
	@ConditionalOnMissingBean
	public ResponsesClient azureAgentsResponsesClient(AgentsClientBuilder builder) {
		return builder.buildResponsesClient();
	}

	@Bean
	@ConditionalOnMissingBean
	public OpenAIClient azureAgentsOpenAIClient(AgentsClientBuilder builder) {
		return builder.buildOpenAIClient();
	}

	@Bean
	@ConditionalOnMissingBean
	public ConversationIdRepository conversationIdRepository() {
		return new InMemoryConversationIdRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public AzureFunctionToolFactory azureFunctionToolFactory(ObjectProvider<ObjectMapper> objectMapperProvider,
			AzureAgentsProperties properties) {
		ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
		return new AzureFunctionToolFactory(objectMapper, properties.isStrictFunctionTools());
	}

	@Bean
	@ConditionalOnMissingBean
	public AzureAgentsToolExecutor azureAgentsToolExecutor() {
		return new AzureAgentsToolExecutor();
	}

	@Bean
	@ConditionalOnMissingBean
	public AzureAgentsReferenceManager azureAgentsReferenceManager(AgentsClient agentsClient,
			AzureFunctionToolFactory functionToolFactory, AzureAgentsProperties properties) {
		if (!StringUtils.hasText(properties.getAgentVersion()) && !StringUtils.hasText(properties.getModelDeploymentName())) {
			throw new IllegalStateException(
					"Set spring.ai.azure.agents.model-deployment-name (to create agents) or spring.ai.azure.agents.agent-version (to use an existing agent).");
		}
		String model = StringUtils.hasText(properties.getModelDeploymentName()) ? properties.getModelDeploymentName()
				: "unused";
		return new AzureAgentsReferenceManager(agentsClient, functionToolFactory, properties.getAgentName(), model,
				properties.getInstructions(), properties.isCodeInterpreterEnabled(),
				properties.getFileSearchVectorStoreIds(), properties.isCreateAgentOnDemand(),
				properties.getAgentVersion());
	}

	@Bean
	@ConditionalOnMissingBean
	public AzureAgentsChatOptions azureAgentsChatOptions(AzureAgentsProperties properties) {
		AzureAgentsChatOptions options = new AzureAgentsChatOptions();
		options.setAgentName(properties.getAgentName());
		options.setAgentVersion(properties.getAgentVersion());
		options.setInstructions(properties.getInstructions());
		options.setModel(properties.getModelDeploymentName());
		options.setMaxToolRounds(properties.getMaxToolRounds());
		options.setInternalToolExecutionEnabled(false);
		return options;
	}

	@Bean
	@ConditionalOnMissingBean
	public AzureAgentsChatModel azureAgentsChatModel(ResponsesClient responsesClient, OpenAIClient openAIClient,
			AzureAgentsReferenceManager agentReferenceManager, AzureAgentsToolExecutor toolExecutor,
			AzureAgentsChatOptions defaultOptions, ObjectProvider<ObservationRegistry> observationRegistry) {
		return new AzureAgentsChatModel(responsesClient, openAIClient, agentReferenceManager, toolExecutor,
				defaultOptions, observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP));
	}

	@Bean
	@ConditionalOnMissingBean
	public AzureConversationIdAdvisor azureConversationIdAdvisor(OpenAIClient openAIClient,
			ConversationIdRepository conversationIdRepository) {
		return new AzureConversationIdAdvisor(openAIClient, conversationIdRepository);
	}

	@Bean
	@ConditionalOnMissingBean
	public ChatClient.Builder azureAgentsChatClientBuilder(AzureAgentsChatModel chatModel,
			AzureConversationIdAdvisor conversationIdAdvisor) {
		return ChatClient.builder(chatModel).defaultAdvisors(conversationIdAdvisor);
	}

}
