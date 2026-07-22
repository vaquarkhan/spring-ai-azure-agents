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

import java.util.List;
import java.util.Map;

import com.azure.ai.agents.models.FunctionTool;
import com.azure.ai.agents.models.Tool;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AzureFunctionToolFactory}.
 *
 * @author Viquar Khan
 */
class AzureFunctionToolFactoryTests {

	private final AzureFunctionToolFactory factory = new AzureFunctionToolFactory(new ObjectMapper());

	@Test
	void parsesObjectSchema() {
		String schema = """
				{
				  "type": "object",
				  "properties": {
				    "location": { "type": "string" }
				  },
				  "required": ["location"]
				}
				""";

		Map<String, BinaryData> parameters = this.factory.toParameters(schema);
		assertThat(parameters).containsKeys("type", "properties", "required");
		assertThat(parameters.get("type").toObject(String.class)).isEqualTo("object");
	}

	@Test
	void emptySchemaDefaultsToObject() {
		Map<String, BinaryData> parameters = this.factory.toParameters("");
		assertThat(parameters).containsKeys("type", "properties");
		assertThat(parameters.get("type").toObject(String.class)).isEqualTo("object");
	}

	@Test
	void nullSchemaDefaultsToObject() {
		Map<String, BinaryData> parameters = this.factory.toParameters(null);
		assertThat(parameters).containsKeys("type", "properties");
	}

	@Test
	void invalidJsonThrows() {
		assertThatThrownBy(() -> this.factory.toParameters("{not-json"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Failed to parse tool input schema JSON");
	}

	@Test
	void toFunctionToolMapsNameDescriptionAndSchema() {
		ToolCallback callback = callback("get_weather", "Weather lookup",
				"""
						{"type":"object","properties":{"city":{"type":"string"}}}
						""");

		FunctionTool tool = this.factory.toFunctionTool(callback);
		assertThat(tool.getName()).isEqualTo("get_weather");
		assertThat(tool.getDescription()).isEqualTo("Weather lookup");
		assertThat(tool.getParameters()).containsKey("properties");
		assertThat(tool.isStrict()).isTrue();
	}

	@Test
	void toAzureToolsConvertsList() {
		ToolCallback a = callback("a", "A", "{\"type\":\"object\",\"properties\":{}}");
		ToolCallback b = callback("b", "B", "{\"type\":\"object\",\"properties\":{}}");
		List<Tool> tools = this.factory.toAzureTools(java.util.List.of(a, b));
		assertThat(tools).hasSize(2);
		assertThat(((FunctionTool) tools.get(0)).getName()).isEqualTo("a");
		assertThat(((FunctionTool) tools.get(1)).getName()).isEqualTo("b");
	}

	@Test
	void nonStrictFactory() {
		AzureFunctionToolFactory nonStrict = new AzureFunctionToolFactory(new ObjectMapper(), false);
		FunctionTool tool = nonStrict.toFunctionTool(callback("x", "X", "{\"type\":\"object\",\"properties\":{}}"));
		assertThat(tool.isStrict()).isFalse();
	}

	private static ToolCallback callback(String name, String description, String schema) {
		ToolDefinition definition = ToolDefinition.builder()
			.name(name)
			.description(description)
			.inputSchema(schema)
			.build();
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return definition;
			}

			@Override
			public String call(String toolInput) {
				return "{}";
			}
		};
	}

}
