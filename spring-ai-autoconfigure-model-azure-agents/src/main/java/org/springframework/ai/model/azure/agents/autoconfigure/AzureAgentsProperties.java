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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@code spring.ai.azure.agents.*}.
 *
 * @author Viquar Khan
 */
@ConfigurationProperties(AzureAgentsProperties.CONFIG_PREFIX)
public class AzureAgentsProperties {

	public static final String CONFIG_PREFIX = "spring.ai.azure.agents";

	/**
	 * Whether Foundry Agent Service auto-configuration is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Microsoft Foundry project endpoint.
	 */
	private String endpoint;

	/**
	 * Agent name used with {@code createAgentVersion} / {@link com.azure.ai.agents.models.AgentReference}.
	 */
	private String agentName = "spring-ai-agent";

	/**
	 * Optional existing agent version. When set, no agent is created on demand.
	 */
	private String agentVersion;

	/**
	 * Model deployment name for prompt agents (for example {@code gpt-4o}).
	 */
	private String modelDeploymentName;

	/**
	 * Default system instructions for created agents.
	 */
	private String instructions = "You are a helpful assistant.";

	/**
	 * Create a new agent version on first use when {@link #agentVersion} is unset.
	 */
	private boolean createAgentOnDemand = true;

	/**
	 * Attach Azure Code Interpreter to created agents.
	 */
	private boolean codeInterpreterEnabled = false;

	/**
	 * Vector store ids for Azure File Search (native tool).
	 */
	private List<String> fileSearchVectorStoreIds = new ArrayList<>();

	/**
	 * Maximum local tool round-trips per user turn.
	 */
	private int maxToolRounds = 8;

	/**
	 * Use strict JSON schema mode for FunctionTool definitions.
	 */
	private boolean strictFunctionTools = true;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getAgentName() {
		return this.agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getAgentVersion() {
		return this.agentVersion;
	}

	public void setAgentVersion(String agentVersion) {
		this.agentVersion = agentVersion;
	}

	public String getModelDeploymentName() {
		return this.modelDeploymentName;
	}

	public void setModelDeploymentName(String modelDeploymentName) {
		this.modelDeploymentName = modelDeploymentName;
	}

	public String getInstructions() {
		return this.instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public boolean isCreateAgentOnDemand() {
		return this.createAgentOnDemand;
	}

	public void setCreateAgentOnDemand(boolean createAgentOnDemand) {
		this.createAgentOnDemand = createAgentOnDemand;
	}

	public boolean isCodeInterpreterEnabled() {
		return this.codeInterpreterEnabled;
	}

	public void setCodeInterpreterEnabled(boolean codeInterpreterEnabled) {
		this.codeInterpreterEnabled = codeInterpreterEnabled;
	}

	public List<String> getFileSearchVectorStoreIds() {
		return this.fileSearchVectorStoreIds;
	}

	public void setFileSearchVectorStoreIds(List<String> fileSearchVectorStoreIds) {
		this.fileSearchVectorStoreIds = fileSearchVectorStoreIds;
	}

	public int getMaxToolRounds() {
		return this.maxToolRounds;
	}

	public void setMaxToolRounds(int maxToolRounds) {
		this.maxToolRounds = maxToolRounds;
	}

	public boolean isStrictFunctionTools() {
		return this.strictFunctionTools;
	}

	public void setStrictFunctionTools(boolean strictFunctionTools) {
		this.strictFunctionTools = strictFunctionTools;
	}

}
