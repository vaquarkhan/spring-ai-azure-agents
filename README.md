# spring-ai-azure-agents

Spring AI `ChatModel` and Boot starter for **Microsoft Foundry Agent Service** (Azure AI Agents).

**Authors:** Viquar Khan, Pete Tian

## Requirements

- JDK 21+
- Spring Boot 3.5.x
- Spring AI 1.1.x
- Azure CLI login or Managed Identity (`DefaultAzureCredential`)
- Foundry project endpoint and model deployment

## Modules

| Module | Description |
| --- | --- |
| `spring-ai-azure-agents` | Core `ChatModel`, advisor, tool bridge |
| `spring-ai-autoconfigure-model-azure-agents` | Spring Boot auto-configuration |
| `spring-ai-starter-model-azure-agents` | Starter dependency |
| `spring-ai-azure-agents-sample` | Sample REST app |

## Add the dependency

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-azure-agents</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
spring:
  ai:
    azure:
      agents:
        enabled: true
        endpoint: ${FOUNDRY_PROJECT_ENDPOINT}
        model-deployment-name: ${FOUNDRY_MODEL_NAME}
        agent-name: my-agent
        instructions: You are a helpful assistant.
        create-agent-on-demand: true
        code-interpreter-enabled: false
        max-tool-rounds: 8
```

| Property | Description |
| --- | --- |
| `spring.ai.azure.agents.endpoint` | Foundry project endpoint (required) |
| `spring.ai.azure.agents.model-deployment-name` | Model deployment for agent creation |
| `spring.ai.azure.agents.agent-name` | Agent name |
| `spring.ai.azure.agents.agent-version` | Existing agent version (skips create) |
| `spring.ai.azure.agents.instructions` | Default agent instructions |
| `spring.ai.azure.agents.create-agent-on-demand` | Create agent version when missing |
| `spring.ai.azure.agents.code-interpreter-enabled` | Attach Code Interpreter |
| `spring.ai.azure.agents.file-search-vector-store-ids` | File Search vector store ids |
| `spring.ai.azure.agents.max-tool-rounds` | Max local tool round-trips per turn |

## Usage

```java
chatClient.prompt()
    .user(message)
    .tools(weatherTools)
    .advisors(a -> a.param(AzureAgentsConstants.SESSION_ID, sessionId))
    .call()
    .content();
```

Pass a stable `SESSION_ID` so turns reuse the same Foundry conversation.

## Build

```bash
# Windows PowerShell: set JAVA_HOME to JDK 21 first
mvn clean test
mvn clean install
```

## Run the sample

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

## License

Apache License 2.0
