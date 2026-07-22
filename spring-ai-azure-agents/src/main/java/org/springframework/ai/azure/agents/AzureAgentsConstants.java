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

/**
 * Shared context / metadata keys for Microsoft Foundry Agent Service.
 *
 * @author Viquar Khan
 */
public final class AzureAgentsConstants {

	/**
	 * Advisor / options key for the Foundry conversation id.
	 */
	public static final String CONVERSATION_ID = "azure.conversation.id";

	/**
	 * Logical Spring session (or client-supplied) id used to resolve a conversation.
	 */
	public static final String SESSION_ID = "azure.agent.session.id";

	/**
	 * Micrometer observation name for an agent response loop.
	 */
	public static final String OBSERVATION_NAME = "azure.agents.response";

	private AzureAgentsConstants() {
	}

}
