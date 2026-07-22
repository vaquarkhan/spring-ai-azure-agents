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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ToolCallingChatOptions} for Microsoft Foundry Agent Service invocations.
 *
 * @author Viquar Khan
 */
public class AzureAgentsChatOptions implements ToolCallingChatOptions {

	@Nullable
	private String model;

	@Nullable
	private Double frequencyPenalty;

	@Nullable
	private Integer maxTokens;

	@Nullable
	private Double presencePenalty;

	@Nullable
	private List<String> stopSequences;

	@Nullable
	private Double temperature;

	@Nullable
	private Integer topK;

	@Nullable
	private Double topP;

	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private Set<String> toolNames = new HashSet<>();

	private Map<String, Object> toolContext = new HashMap<>();

	private Boolean internalToolExecutionEnabled = false;

	@Nullable
	private String agentName;

	@Nullable
	private String agentVersion;

	@Nullable
	private String conversationId;

	@Nullable
	private String instructions;

	@Nullable
	private Integer maxToolRounds;

	@Nullable
	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(@Nullable String model) {
		this.model = model;
	}

	@Nullable
	@Override
	public Double getFrequencyPenalty() {
		return this.frequencyPenalty;
	}

	public void setFrequencyPenalty(@Nullable Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	@Nullable
	@Override
	public Integer getMaxTokens() {
		return this.maxTokens;
	}

	public void setMaxTokens(@Nullable Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	@Nullable
	@Override
	public Double getPresencePenalty() {
		return this.presencePenalty;
	}

	public void setPresencePenalty(@Nullable Double presencePenalty) {
		this.presencePenalty = presencePenalty;
	}

	@Nullable
	@Override
	public List<String> getStopSequences() {
		return this.stopSequences;
	}

	public void setStopSequences(@Nullable List<String> stopSequences) {
		this.stopSequences = stopSequences;
	}

	@Nullable
	@Override
	public Double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(@Nullable Double temperature) {
		this.temperature = temperature;
	}

	@Nullable
	@Override
	public Integer getTopK() {
		return this.topK;
	}

	public void setTopK(@Nullable Integer topK) {
		this.topK = topK;
	}

	@Nullable
	@Override
	public Double getTopP() {
		return this.topP;
	}

	public void setTopP(@Nullable Double topP) {
		this.topP = topP;
	}

	@Override
	public List<ToolCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	@Override
	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks must not be null");
		this.toolCallbacks = new ArrayList<>(toolCallbacks);
	}

	@Override
	public Set<String> getToolNames() {
		return this.toolNames;
	}

	@Override
	public void setToolNames(Set<String> toolNames) {
		Assert.notNull(toolNames, "toolNames must not be null");
		this.toolNames = new HashSet<>(toolNames);
	}

	@Override
	public Map<String, Object> getToolContext() {
		return this.toolContext;
	}

	@Override
	public void setToolContext(Map<String, Object> toolContext) {
		Assert.notNull(toolContext, "toolContext must not be null");
		this.toolContext = new HashMap<>(toolContext);
	}

	@Override
	public Boolean getInternalToolExecutionEnabled() {
		return this.internalToolExecutionEnabled;
	}

	@Override
	public void setInternalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
		// Always false: Foundry Agent Service owns the function-call round-trip.
		this.internalToolExecutionEnabled = false;
	}

	@Nullable
	public String getAgentName() {
		return this.agentName;
	}

	public void setAgentName(@Nullable String agentName) {
		this.agentName = agentName;
	}

	@Nullable
	public String getAgentVersion() {
		return this.agentVersion;
	}

	public void setAgentVersion(@Nullable String agentVersion) {
		this.agentVersion = agentVersion;
	}

	@Nullable
	public String getConversationId() {
		return this.conversationId;
	}

	public void setConversationId(@Nullable String conversationId) {
		this.conversationId = conversationId;
	}

	@Nullable
	public String getInstructions() {
		return this.instructions;
	}

	public void setInstructions(@Nullable String instructions) {
		this.instructions = instructions;
	}

	@Nullable
	public Integer getMaxToolRounds() {
		return this.maxToolRounds;
	}

	public void setMaxToolRounds(@Nullable Integer maxToolRounds) {
		this.maxToolRounds = maxToolRounds;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends org.springframework.ai.chat.prompt.ChatOptions> T copy() {
		return (T) fromOptions(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static AzureAgentsChatOptions fromOptions(AzureAgentsChatOptions fromOptions) {
		Assert.notNull(fromOptions, "fromOptions must not be null");
		AzureAgentsChatOptions options = new AzureAgentsChatOptions();
		options.setModel(fromOptions.getModel());
		options.setFrequencyPenalty(fromOptions.getFrequencyPenalty());
		options.setMaxTokens(fromOptions.getMaxTokens());
		options.setPresencePenalty(fromOptions.getPresencePenalty());
		options.setStopSequences(fromOptions.getStopSequences() != null
				? new ArrayList<>(fromOptions.getStopSequences()) : null);
		options.setTemperature(fromOptions.getTemperature());
		options.setTopK(fromOptions.getTopK());
		options.setTopP(fromOptions.getTopP());
		options.setToolCallbacks(new ArrayList<>(fromOptions.getToolCallbacks()));
		options.setToolNames(new HashSet<>(fromOptions.getToolNames()));
		options.setToolContext(new HashMap<>(fromOptions.getToolContext()));
		options.internalToolExecutionEnabled = false;
		options.setAgentName(fromOptions.getAgentName());
		options.setAgentVersion(fromOptions.getAgentVersion());
		options.setConversationId(fromOptions.getConversationId());
		options.setInstructions(fromOptions.getInstructions());
		options.setMaxToolRounds(fromOptions.getMaxToolRounds());
		return options;
	}

	/**
	 * Merge runtime prompt options onto default AutoConfiguration options.
	 */
	public static AzureAgentsChatOptions merge(@Nullable org.springframework.ai.chat.prompt.ChatOptions runtime,
			AzureAgentsChatOptions defaults) {
		Assert.notNull(defaults, "defaults must not be null");
		AzureAgentsChatOptions merged = fromOptions(defaults);
		if (runtime == null) {
			return merged;
		}
		if (runtime instanceof AzureAgentsChatOptions azureOptions) {
			if (azureOptions.getAgentName() != null) {
				merged.setAgentName(azureOptions.getAgentName());
			}
			if (azureOptions.getAgentVersion() != null) {
				merged.setAgentVersion(azureOptions.getAgentVersion());
			}
			if (azureOptions.getConversationId() != null) {
				merged.setConversationId(azureOptions.getConversationId());
			}
			if (azureOptions.getInstructions() != null) {
				merged.setInstructions(azureOptions.getInstructions());
			}
			if (azureOptions.getMaxToolRounds() != null) {
				merged.setMaxToolRounds(azureOptions.getMaxToolRounds());
			}
		}
		if (runtime.getModel() != null) {
			merged.setModel(runtime.getModel());
		}
		if (runtime.getTemperature() != null) {
			merged.setTemperature(runtime.getTemperature());
		}
		if (runtime.getTopP() != null) {
			merged.setTopP(runtime.getTopP());
		}
		if (runtime.getMaxTokens() != null) {
			merged.setMaxTokens(runtime.getMaxTokens());
		}
		if (runtime instanceof ToolCallingChatOptions toolOptions) {
			if (toolOptions.getToolCallbacks() != null && !toolOptions.getToolCallbacks().isEmpty()) {
				merged.setToolCallbacks(new ArrayList<>(toolOptions.getToolCallbacks()));
			}
			if (toolOptions.getToolNames() != null && !toolOptions.getToolNames().isEmpty()) {
				merged.setToolNames(new HashSet<>(toolOptions.getToolNames()));
			}
			if (toolOptions.getToolContext() != null && !toolOptions.getToolContext().isEmpty()) {
				merged.setToolContext(new HashMap<>(toolOptions.getToolContext()));
			}
		}
		merged.internalToolExecutionEnabled = false;
		return merged;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AzureAgentsChatOptions that)) {
			return false;
		}
		return Objects.equals(this.model, that.model) && Objects.equals(this.temperature, that.temperature)
				&& Objects.equals(this.topP, that.topP) && Objects.equals(this.maxTokens, that.maxTokens)
				&& Objects.equals(this.agentName, that.agentName)
				&& Objects.equals(this.agentVersion, that.agentVersion)
				&& Objects.equals(this.conversationId, that.conversationId)
				&& Objects.equals(this.instructions, that.instructions)
				&& Objects.equals(this.maxToolRounds, that.maxToolRounds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.model, this.temperature, this.topP, this.maxTokens, this.agentName, this.agentVersion,
				this.conversationId, this.instructions, this.maxToolRounds);
	}

	public static final class Builder implements ToolCallingChatOptions.Builder {

		private final AzureAgentsChatOptions options = new AzureAgentsChatOptions();

		public Builder agentName(String agentName) {
			this.options.setAgentName(agentName);
			return this;
		}

		public Builder agentVersion(String agentVersion) {
			this.options.setAgentVersion(agentVersion);
			return this;
		}

		public Builder conversationId(String conversationId) {
			this.options.setConversationId(conversationId);
			return this;
		}

		public Builder instructions(String instructions) {
			this.options.setInstructions(instructions);
			return this;
		}

		public Builder maxToolRounds(Integer maxToolRounds) {
			this.options.setMaxToolRounds(maxToolRounds);
			return this;
		}

		@Override
		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.options.setToolCallbacks(toolCallbacks);
			return this;
		}

		@Override
		public Builder toolCallbacks(ToolCallback... toolCallbacks) {
			this.options.setToolCallbacks(Arrays.asList(toolCallbacks));
			return this;
		}

		@Override
		public Builder toolNames(Set<String> toolNames) {
			this.options.setToolNames(toolNames);
			return this;
		}

		@Override
		public Builder toolNames(String... toolNames) {
			this.options.setToolNames(new HashSet<>(Arrays.asList(toolNames)));
			return this;
		}

		@Override
		public Builder toolContext(Map<String, Object> toolContext) {
			this.options.setToolContext(toolContext);
			return this;
		}

		@Override
		public Builder toolContext(String key, Object value) {
			Assert.notNull(key, "key must not be null");
			this.options.getToolContext().put(key, value);
			return this;
		}

		@Override
		public Builder internalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
			this.options.setInternalToolExecutionEnabled(false);
			return this;
		}

		@Override
		public Builder model(@Nullable String model) {
			this.options.setModel(model);
			return this;
		}

		@Override
		public Builder frequencyPenalty(@Nullable Double frequencyPenalty) {
			this.options.setFrequencyPenalty(frequencyPenalty);
			return this;
		}

		@Override
		public Builder maxTokens(@Nullable Integer maxTokens) {
			this.options.setMaxTokens(maxTokens);
			return this;
		}

		@Override
		public Builder presencePenalty(@Nullable Double presencePenalty) {
			this.options.setPresencePenalty(presencePenalty);
			return this;
		}

		@Override
		public Builder stopSequences(@Nullable List<String> stopSequences) {
			this.options.setStopSequences(stopSequences);
			return this;
		}

		@Override
		public Builder temperature(@Nullable Double temperature) {
			this.options.setTemperature(temperature);
			return this;
		}

		@Override
		public Builder topK(@Nullable Integer topK) {
			this.options.setTopK(topK);
			return this;
		}

		@Override
		public Builder topP(@Nullable Double topP) {
			this.options.setTopP(topP);
			return this;
		}

		@Override
		public AzureAgentsChatOptions build() {
			return AzureAgentsChatOptions.fromOptions(this.options);
		}

	}

}
