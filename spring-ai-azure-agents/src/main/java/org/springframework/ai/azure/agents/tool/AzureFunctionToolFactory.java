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

package org.springframework.ai.azure.agents.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.azure.ai.agents.models.FunctionTool;
import com.azure.ai.agents.models.Tool;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Translates Spring AI {@link ToolCallback} definitions into Azure {@link FunctionTool}s.
 *
 * @author Viquar Khan
 */
public class AzureFunctionToolFactory {

	private final ObjectMapper objectMapper;

	private final boolean strict;

	public AzureFunctionToolFactory(ObjectMapper objectMapper) {
		this(objectMapper, true);
	}

	public AzureFunctionToolFactory(ObjectMapper objectMapper, boolean strict) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
		this.strict = strict;
	}

	public List<Tool> toAzureTools(List<ToolCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks must not be null");
		List<Tool> tools = new ArrayList<>(toolCallbacks.size());
		for (ToolCallback callback : toolCallbacks) {
			tools.add(toFunctionTool(callback));
		}
		return tools;
	}

	public FunctionTool toFunctionTool(ToolCallback callback) {
		Assert.notNull(callback, "callback must not be null");
		ToolDefinition definition = callback.getToolDefinition();
		Map<String, BinaryData> parameters = toParameters(definition.inputSchema());
		FunctionTool tool = new FunctionTool(definition.name(), parameters, this.strict);
		if (StringUtils.hasText(definition.description())) {
			tool.setDescription(definition.description());
		}
		return tool;
	}

	@SuppressWarnings("unchecked")
	Map<String, BinaryData> toParameters(String inputSchemaJson) {
		if (!StringUtils.hasText(inputSchemaJson)) {
			Map<String, BinaryData> empty = new LinkedHashMap<>();
			empty.put("type", BinaryData.fromObject("object"));
			empty.put("properties", BinaryData.fromObject(Map.of()));
			return empty;
		}
		try {
			Map<String, Object> schema = this.objectMapper.readValue(inputSchemaJson,
					new TypeReference<Map<String, Object>>() {
					});
			Map<String, BinaryData> parameters = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : schema.entrySet()) {
				parameters.put(entry.getKey(), BinaryData.fromObject(entry.getValue()));
			}
			return parameters;
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to parse tool input schema JSON", ex);
		}
	}

}
