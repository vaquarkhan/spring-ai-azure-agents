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
import java.util.List;

import com.azure.ai.agents.models.CodeInterpreterTool;
import com.azure.ai.agents.models.FileSearchTool;
import com.azure.ai.agents.models.Tool;
import org.springframework.util.CollectionUtils;

/**
 * Helpers for attaching Azure-hosted (native) tools to an agent definition.
 *
 * @author Viquar Khan
 */
public final class AzureNativeToolFactory {

	private AzureNativeToolFactory() {
	}

	public static CodeInterpreterTool codeInterpreter() {
		return new CodeInterpreterTool();
	}

	public static FileSearchTool fileSearch(List<String> vectorStoreIds) {
		return new FileSearchTool(vectorStoreIds);
	}

	public static List<Tool> fromFlags(boolean codeInterpreterEnabled, List<String> fileSearchVectorStoreIds) {
		List<Tool> tools = new ArrayList<>();
		if (codeInterpreterEnabled) {
			tools.add(codeInterpreter());
		}
		if (!CollectionUtils.isEmpty(fileSearchVectorStoreIds)) {
			tools.add(fileSearch(fileSearchVectorStoreIds));
		}
		return tools;
	}

}
