### Project Name

spring-ai-azure-agents

### Project Description

Spring AI ChatModel and Boot starter for Microsoft Foundry Agent Service (Azure AI Agents).

Microsoft Agent Framework prioritizes Python and .NET. Java teams today must use the low-level `com.azure:azure-ai-agents` SDK and hand-wire conversations, responses, local tool round-trips, and observability. Spring AI already has strong Azure OpenAI / Foundry chat model support, but not a first-class Foundry Agent Service orchestration layer.

This project fills that gap with Spring-idiomatic components:

- `AzureAgentsChatModel` - delegates reasoning to Foundry Conversations / Responses APIs
- `AzureConversationIdAdvisor` - maps a local session id to a Foundry conversation id (server-side memory)
- Tool virtualization - Spring `@Tool` / `ToolCallback` schemas become Azure `FunctionTool`s; local execution on function calls; results submitted with `previousResponseId`
- Native Azure tools - optional Code Interpreter and File Search attachment
- Micrometer observations - `azure.agents.response` with agent and conversation cardinality
- Spring Boot auto-configuration under `spring.ai.azure.agents.*`

Important naming note: this is intentionally **not** called AgentCore. Amazon Bedrock AgentCore is an AWS runtime product. This library targets Microsoft Foundry Agent Service / Azure AI Agents.

### Existing Repository URL with POC implementation

https://github.com/vaquarkhan/spring-ai-azure-agents

### Integration with Spring AI

Implements Spring AI `ChatModel` and integrates with `ChatClient`, Advisors, and `ToolCallback` / `@Tool`.

Developers use familiar Spring AI APIs:

```java
chatClient.prompt()
  .user(message)
  .tools(weatherTools)
  .advisors(a -> a.param(AzureAgentsConstants.SESSION_ID, sessionId))
  .call()
  .content();
```

Modules follow Spring AI starter conventions:

- `spring-ai-azure-agents` (core)
- `spring-ai-autoconfigure-model-azure-agents`
- `spring-ai-starter-model-azure-agents`
- `spring-ai-azure-agents-sample`

Baseline: Java 21, Spring Boot 3.5.x, Spring AI 1.1.x, `com.azure:azure-ai-agents` 2.2.0+.

Anti-goals:

- Do not wrap the deprecated classic Threads/Runs Assistants SDK (`azure-ai-agents-persistent`)
- Do not replace Spring AI tool calling; reuse `ToolCallback`
- Do not invent a competing ChatClient API

### Existing Documentation

https://github.com/vaquarkhan/spring-ai-azure-agents/blob/main/README.md

### Project Requirements

- [x] Has working proof of concept that demonstrates integration with Spring AI
- [x] Includes unit and integration tests
- [x] Uses or will use Apache 2 license
- [x] All development will occur in a public repository
- [x] Agrees to follow the Spring AI code of conduct
- [x] Will provide clear contribution guidelines
- [x] Will follow semantic versioning (MAJOR.MINOR.PATCH)

### Preferred Packaging Method

Using GitHub's process with `io.github.spring-ai-community` as the groupId once incubated.
