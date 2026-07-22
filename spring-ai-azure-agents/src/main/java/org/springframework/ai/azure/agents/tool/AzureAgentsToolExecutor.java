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
import java.util.function.Function;
import java.util.stream.Collectors;

import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

/**
 * Executes Spring AI {@link ToolCallback}s for Foundry function-call requests.
 *
 * @author Viquar Khan
 */
public class AzureAgentsToolExecutor {

	private static final Logger logger = LoggerFactory.getLogger(AzureAgentsToolExecutor.class);

	public List<ResponseInputItem> execute(List<ResponseFunctionToolCall> functionCalls,
			List<ToolCallback> toolCallbacks, Map<String, Object> toolContext) {
		Assert.notNull(functionCalls, "functionCalls must not be null");
		Assert.notNull(toolCallbacks, "toolCallbacks must not be null");

		Map<String, ToolCallback> byName = toolCallbacks.stream()
			.collect(Collectors.toMap(cb -> cb.getToolDefinition().name(), Function.identity(), (a, b) -> a,
					LinkedHashMap::new));

		List<ResponseInputItem> outputs = new ArrayList<>(functionCalls.size());
		ToolContext springToolContext = (toolContext == null || toolContext.isEmpty()) ? null
				: new ToolContext(toolContext);

		for (ResponseFunctionToolCall call : functionCalls) {
			String name = call.name();
			ToolCallback callback = byName.get(name);
			String output;
			if (callback == null) {
				logger.warn("No local ToolCallback registered for function '{}'", name);
				output = "{\"error\":\"Unknown tool: " + name + "\"}";
			}
			else {
				try {
					if (springToolContext != null) {
						output = callback.call(call.arguments(), springToolContext);
					}
					else {
						output = callback.call(call.arguments());
					}
				}
				catch (Exception ex) {
					logger.error("Tool '{}' execution failed", name, ex);
					output = "{\"error\":\"" + sanitize(ex.getMessage()) + "\"}";
				}
			}
			ResponseInputItem.FunctionCallOutput functionCallOutput = ResponseInputItem.FunctionCallOutput.builder()
				.callId(call.callId())
				.output(output != null ? output : "")
				.build();
			outputs.add(ResponseInputItem.ofFunctionCallOutput(functionCallOutput));
		}
		return outputs;
	}

	private static String sanitize(String message) {
		if (message == null) {
			return "tool execution failed";
		}
		return message.replace("\"", "'");
	}

}
