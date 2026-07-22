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

import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AzureAgentsToolExecutor}.
 *
 * @author Viquar Khan
 */
@ExtendWith(MockitoExtension.class)
class AzureAgentsToolExecutorTests {

	private final AzureAgentsToolExecutor executor = new AzureAgentsToolExecutor();

	@Mock
	private ResponseFunctionToolCall functionCall;

	@Test
	void executesMatchingTool() {
		when(this.functionCall.name()).thenReturn("echo");
		when(this.functionCall.callId()).thenReturn("call_1");
		when(this.functionCall.arguments()).thenReturn("{\"value\":\"hi\"}");

		ToolCallback callback = callback("echo", input -> "{\"ok\":true,\"echo\":" + input + "}");

		List<ResponseInputItem> outputs = this.executor.execute(List.of(this.functionCall), List.of(callback),
				Map.of());

		assertThat(outputs).hasSize(1);
		assertThat(outputs.get(0).isFunctionCallOutput()).isTrue();
		ResponseInputItem.FunctionCallOutput output = outputs.get(0).asFunctionCallOutput();
		assertThat(output.callId()).isEqualTo("call_1");
		assertThat(output.output().asString()).contains("ok");
	}

	@Test
	void unknownToolReturnsErrorJson() {
		when(this.functionCall.name()).thenReturn("missing");
		when(this.functionCall.callId()).thenReturn("call_2");

		List<ResponseInputItem> outputs = this.executor.execute(List.of(this.functionCall), List.of(), Map.of());

		assertThat(outputs.get(0).asFunctionCallOutput().output().asString()).contains("Unknown tool");
	}

	@Test
	void toolExceptionIsCaptured() {
		when(this.functionCall.name()).thenReturn("boom");
		when(this.functionCall.callId()).thenReturn("call_3");
		when(this.functionCall.arguments()).thenReturn("{}");

		ToolCallback callback = callback("boom", input -> {
			throw new IllegalStateException("kaboom");
		});

		List<ResponseInputItem> outputs = this.executor.execute(List.of(this.functionCall), List.of(callback), null);
		assertThat(outputs.get(0).asFunctionCallOutput().output().asString()).contains("kaboom");
	}

	private static ToolCallback callback(String name, java.util.function.Function<String, String> fn) {
		ToolDefinition definition = ToolDefinition.builder()
			.name(name)
			.description(name)
			.inputSchema("{\"type\":\"object\",\"properties\":{}}")
			.build();
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return definition;
			}

			@Override
			public String call(String toolInput) {
				return fn.apply(toolInput);
			}
		};
	}

}
