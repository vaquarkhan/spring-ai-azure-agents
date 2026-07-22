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

import com.azure.ai.agents.models.CodeInterpreterTool;
import com.azure.ai.agents.models.FileSearchTool;
import com.azure.ai.agents.models.Tool;
import com.azure.ai.agents.models.ToolType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AzureNativeToolFactory}.
 *
 * @author Viquar Khan
 */
class AzureNativeToolFactoryTests {

	@Test
	void codeInterpreterCreatesTool() {
		CodeInterpreterTool tool = AzureNativeToolFactory.codeInterpreter();
		assertThat(tool.getType()).isEqualTo(ToolType.CODE_INTERPRETER);
	}

	@Test
	void fileSearchCreatesTool() {
		FileSearchTool tool = AzureNativeToolFactory.fileSearch(List.of("vs-1", "vs-2"));
		assertThat(tool.getVectorStoreIds()).containsExactly("vs-1", "vs-2");
	}

	@Test
	void fromFlagsEmptyWhenDisabled() {
		assertThat(AzureNativeToolFactory.fromFlags(false, List.of())).isEmpty();
	}

	@Test
	void fromFlagsIncludesBoth() {
		List<Tool> tools = AzureNativeToolFactory.fromFlags(true, List.of("vs-9"));
		assertThat(tools).hasSize(2);
		assertThat(tools.get(0)).isInstanceOf(CodeInterpreterTool.class);
		assertThat(tools.get(1)).isInstanceOf(FileSearchTool.class);
	}

	@Test
	void fromFlagsIgnoresNullVectorStores() {
		assertThat(AzureNativeToolFactory.fromFlags(false, null)).isEmpty();
	}

}
