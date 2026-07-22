# spring-ai-azure-agents

**Author:** Viquar Khan and Pete Tian


Spring AI integration for **Microsoft Foundry Agent Service** (also called Azure AI Agents) using the Conversations / Responses APIs.

> **Naming note:** This project is intentionally **not** called “AgentCore”. *Amazon Bedrock AgentCore* is an AWS service. The Microsoft product name is **Foundry Agent Service** / **Azure AI Agents** (`com.azure:azure-ai-agents`).

| | |
|---|---|
| **Repository name** | `spring-ai-azure-agents` |
| **Short description** | Spring AI `ChatModel` + Boot starter for Microsoft Foundry Agent Service, with local `@Tool` virtualization and conversation-id memory mapping. |
| **Long description** | Bridges Spring AI’s declarative `ChatClient` / `ChatModel` APIs to Microsoft Foundry Agent Service (`com.azure:azure-ai-agents` Conversations & Responses). Offloads chat history to Foundry conversations, executes Spring tools locally on function calls, and emits Micrometer observations—without using the deprecated classic Threads/Runs Assistants SDK. |

## Modules (Spring AI naming)

| Artifact | Role |
|---|---|
| `spring-ai-azure-agents` | Core `ChatModel`, advisor, tool bridge |
| `spring-ai-autoconfigure-model-azure-agents` | Boot auto-configuration |
| `spring-ai-starter-model-azure-agents` | Starter dependency |
| `spring-ai-azure-agents-sample` | Sample Web app |

## Packages

```
org.springframework.ai.azure.agents
org.springframework.ai.azure.agents.advisor
org.springframework.ai.azure.agents.agent
org.springframework.ai.azure.agents.conversation
org.springframework.ai.azure.agents.tool
org.springframework.ai.model.azure.agents.autoconfigure
```

## Architecture

1. **`AzureConversationIdAdvisor`** — maps a session id → Foundry conversation id (server-side memory).
2. **`AzureAgentsChatModel`** — creates/resolves an agent version, calls `ResponsesClient.createAzureResponse`, executes local `@Tool` / `ToolCallback`s on function calls, submits outputs with `previousResponseId`.
3. **Micrometer** — observation `azure.agents.response` with agent name + conversation/response ids.

## Quick start

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-azure-agents</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
spring:
  ai:
    azure:
      agents:
        endpoint: ${FOUNDRY_PROJECT_ENDPOINT}
        model-deployment-name: ${FOUNDRY_MODEL_NAME}
        agent-name: my-agent
        instructions: You are a helpful assistant.
```

Authenticate with Azure CLI / Managed Identity (`DefaultAzureCredential`).

```java
@RestController
class ChatController {
  private final ChatClient chatClient;

  ChatController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  @PostMapping("/chat")
  String chat(@RequestBody String message) {
    return chatClient.prompt().user(message).call().content();
  }
}
```

Pass a stable session id so turns share one Foundry conversation:

```java
chatClient.prompt()
  .user(message)
  .advisors(a -> a.param(AzureAgentsConstants.SESSION_ID, sessionId))
  .call()
  .content();
```

## Build & test

Requires **JDK 21** (`JAVA_HOME` pointing at JDK 21).

```bash
mvn clean test
mvn clean install
```

Sample:

```bash
export FOUNDRY_PROJECT_ENDPOINT="https://<resource>.services.ai.azure.com/api/projects/<project>"
export FOUNDRY_MODEL_NAME="gpt-4o"
az login
mvn -pl spring-ai-azure-agents-sample spring-boot:run
```

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: demo-1" \
  -d "{\"message\":\"What is the weather in Seattle?\"}"
```

## Configuration properties

Prefix: `spring.ai.azure.agents`

| Property | Description |
|---|---|
| `endpoint` | Foundry project endpoint (required) |
| `model-deployment-name` | Model for `createAgentVersion` |
| `agent-name` | Agent name |
| `agent-version` | Use existing version (skip create) |
| `instructions` | Default agent instructions |
| `create-agent-on-demand` | Create version when missing |
| `code-interpreter-enabled` | Attach Code Interpreter |
| `file-search-vector-store-ids` | File Search vector stores |
| `max-tool-rounds` | Local tool loop limit |

## Requirements

- Java 21+
- Spring Boot 3.5.x
- Spring AI 1.1.x
- `com.azure:azure-ai-agents` 2.2.0+

## License

Apache License 2.0
