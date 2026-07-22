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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.models.AgentReference;
import com.azure.ai.agents.models.AgentVersionDetails;
import com.azure.ai.agents.models.PromptAgentDefinition;
import com.azure.ai.agents.models.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.azure.agents.tool.AzureFunctionToolFactory;
import org.springframework.ai.azure.agents.tool.AzureNativeToolFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Creates and caches Foundry agent versions used by {@code AzureAgentsChatModel}.
 *
 * @author Viquar Khan
 */
public class AzureAgentsReferenceManager {

	private static final Logger logger = LoggerFactory.getLogger(AzureAgentsReferenceManager.class);

	private final AgentsClient agentsClient;

	private final AzureFunctionToolFactory functionToolFactory;

	private final String defaultAgentName;

	private final String modelDeploymentName;

	private final String defaultInstructions;

	private final boolean codeInterpreterEnabled;

	private final List<String> fileSearchVectorStoreIds;

	private final boolean createOnDemand;

	private final String configuredAgentVersion;

	private final AtomicReference<CachedAgent> cache = new AtomicReference<>();

	public AzureAgentsReferenceManager(AgentsClient agentsClient, AzureFunctionToolFactory functionToolFactory,
			String defaultAgentName, String modelDeploymentName, String defaultInstructions,
			boolean codeInterpreterEnabled, List<String> fileSearchVectorStoreIds, boolean createOnDemand,
			String configuredAgentVersion) {
		Assert.notNull(agentsClient, "agentsClient must not be null");
		Assert.notNull(functionToolFactory, "functionToolFactory must not be null");
		Assert.hasText(defaultAgentName, "defaultAgentName must not be empty");
		Assert.hasText(modelDeploymentName, "modelDeploymentName must not be empty");
		this.agentsClient = agentsClient;
		this.functionToolFactory = functionToolFactory;
		this.defaultAgentName = defaultAgentName;
		this.modelDeploymentName = modelDeploymentName;
		this.defaultInstructions = defaultInstructions;
		this.codeInterpreterEnabled = codeInterpreterEnabled;
		this.fileSearchVectorStoreIds = fileSearchVectorStoreIds != null ? List.copyOf(fileSearchVectorStoreIds)
				: List.of();
		this.createOnDemand = createOnDemand;
		this.configuredAgentVersion = configuredAgentVersion;
	}

	public AgentReference resolve(String agentName, String agentVersion, String instructions,
			List<ToolCallback> toolCallbacks) {
		String resolvedName = StringUtils.hasText(agentName) ? agentName : this.defaultAgentName;
		String resolvedInstructions = StringUtils.hasText(instructions) ? instructions : this.defaultInstructions;

		if (StringUtils.hasText(agentVersion) || StringUtils.hasText(this.configuredAgentVersion)) {
			String version = StringUtils.hasText(agentVersion) ? agentVersion : this.configuredAgentVersion;
			return new AgentReference(resolvedName).setVersion(version);
		}

		CachedAgent cached = this.cache.get();
		String toolFingerprint = fingerprint(toolCallbacks, resolvedInstructions, resolvedName);
		if (cached != null && Objects.equals(cached.fingerprint(), toolFingerprint)) {
			return cached.reference();
		}

		if (!this.createOnDemand) {
			throw new IllegalStateException(
					"No agent version configured and create-on-demand is disabled. Set spring.ai.azure.agents.agent-version or enable create-agent-on-demand.");
		}

		List<Tool> tools = new ArrayList<>();
		tools.addAll(AzureNativeToolFactory.fromFlags(this.codeInterpreterEnabled, this.fileSearchVectorStoreIds));
		if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
			tools.addAll(this.functionToolFactory.toAzureTools(toolCallbacks));
		}

		PromptAgentDefinition definition = new PromptAgentDefinition(this.modelDeploymentName);
		if (StringUtils.hasText(resolvedInstructions)) {
			definition.setInstructions(resolvedInstructions);
		}
		if (!tools.isEmpty()) {
			definition.setTools(tools);
		}

		logger.info("Creating Foundry agent version name={} model={} tools={}", resolvedName,
				this.modelDeploymentName, tools.size());
		AgentVersionDetails details = this.agentsClient.createAgentVersion(resolvedName, definition);
		AgentReference reference = new AgentReference(details.getName()).setVersion(details.getVersion());
		this.cache.set(new CachedAgent(toolFingerprint, reference));
		return reference;
	}

	private static String fingerprint(List<ToolCallback> toolCallbacks, String instructions, String agentName) {
		StringBuilder sb = new StringBuilder(agentName).append('|').append(instructions == null ? "" : instructions);
		if (toolCallbacks != null) {
			for (ToolCallback callback : toolCallbacks) {
				sb.append('|')
					.append(callback.getToolDefinition().name())
					.append(':')
					.append(callback.getToolDefinition().inputSchema());
			}
		}
		return Integer.toHexString(sb.toString().hashCode());
	}

	private record CachedAgent(String fingerprint, AgentReference reference) {
	}

}
