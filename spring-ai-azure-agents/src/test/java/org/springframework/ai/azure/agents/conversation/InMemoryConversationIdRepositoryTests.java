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

package org.springframework.ai.azure.agents.conversation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link InMemoryConversationIdRepository}.
 *
 * @author Viquar Khan
 */
class InMemoryConversationIdRepositoryTests {

	private final InMemoryConversationIdRepository repository = new InMemoryConversationIdRepository();

	@Test
	void saveFindAndDelete() {
		assertThat(this.repository.findConversationId("s1")).isNull();
		this.repository.save("s1", "conv-1");
		assertThat(this.repository.findConversationId("s1")).isEqualTo("conv-1");
		this.repository.delete("s1");
		assertThat(this.repository.findConversationId("s1")).isNull();
	}

	@Test
	void overwriteMapping() {
		this.repository.save("s1", "conv-1");
		this.repository.save("s1", "conv-2");
		assertThat(this.repository.findConversationId("s1")).isEqualTo("conv-2");
	}

	@Test
	void rejectsBlankSessionOnFind() {
		assertThatThrownBy(() -> this.repository.findConversationId(" "))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsBlankValuesOnSave() {
		assertThatThrownBy(() -> this.repository.save("", "c1")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> this.repository.save("s1", "")).isInstanceOf(IllegalArgumentException.class);
	}

}
