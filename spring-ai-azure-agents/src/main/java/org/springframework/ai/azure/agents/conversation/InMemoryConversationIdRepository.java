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

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * In-memory {@link ConversationIdRepository}.
 *
 * @author Viquar Khan
 */
public class InMemoryConversationIdRepository implements ConversationIdRepository {

	private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

	@Override
	public String findConversationId(String sessionId) {
		Assert.hasText(sessionId, "sessionId must not be empty");
		return this.store.get(sessionId);
	}

	@Override
	public void save(String sessionId, String conversationId) {
		Assert.hasText(sessionId, "sessionId must not be empty");
		Assert.hasText(conversationId, "conversationId must not be empty");
		this.store.put(sessionId, conversationId);
	}

	@Override
	public void delete(String sessionId) {
		Assert.hasText(sessionId, "sessionId must not be empty");
		this.store.remove(sessionId);
	}

}
