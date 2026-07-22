package com.example.azureagents;

import java.util.Map;
import java.util.UUID;

import org.springframework.ai.azure.agents.AzureAgentsConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample chat endpoint backed by Microsoft Foundry Agent Service.
 *
 * @author Viquar Khan
 */
@RestController
@RequestMapping("/api/chat")
public class AgentChatController {

	private final ChatClient chatClient;

	private final WeatherTools weatherTools;

	public AgentChatController(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {
		this.chatClient = chatClientBuilder.build();
		this.weatherTools = weatherTools;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> chat(@RequestBody ChatRequest request,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
		String resolvedSession = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
		String content = this.chatClient.prompt()
			.user(request.message())
			.tools(this.weatherTools)
			.advisors(a -> a.param(AzureAgentsConstants.SESSION_ID, resolvedSession))
			.call()
			.content();
		return Map.of("sessionId", resolvedSession, "content", content != null ? content : "");
	}

	public record ChatRequest(String message) {
	}

}
