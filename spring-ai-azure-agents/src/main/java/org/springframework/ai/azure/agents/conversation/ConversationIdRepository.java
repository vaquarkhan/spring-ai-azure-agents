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

/**
 * Maps a local session identifier to a Foundry conversation id.
 *
 * @author Viquar Khan
 */
public interface ConversationIdRepository {

	/**
	 * @return existing conversation id for {@code sessionId}, or {@code null}
	 */
	String findConversationId(String sessionId);

	/**
	 * Persist a mapping from {@code sessionId} to {@code conversationId}.
	 */
	void save(String sessionId, String conversationId);

	/**
	 * Remove a session mapping if present.
	 */
	void delete(String sessionId);

}
