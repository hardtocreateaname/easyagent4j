# EasyAgent4j

> A lightweight Java Agent framework inspired by OpenClaw — Build stateful, observable, tool-enabled AI agents with minimal effort.

[![Java 17+](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI 1.1](https://img.shields.io/badge/Spring%20AI-1.1.2-blue.svg)](https://spring.io/projects/spring-ai)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Overview

EasyAgent4j is a Java framework for building AI agents with multi-turn conversations, tool calling, memory, personality, and autonomous task execution. Inspired by [OpenClaw](https://github.com/zhilei/openclaw), it provides a clean abstraction layer over LLM APIs with Spring Boot and Spring AI integration, enabling developers to focus on business logic rather than API complexity.

## Features

- **Dual Provider Architecture** — Choose between HTTP implementation or Spring AI integration for each LLM provider
- **Multi-Model Support** — OpenAI, Anthropic Claude, Zhipu GLM, and MiniMax with runtime switching
- **Spring AI Integration** — Seamless integration with Spring AI starters (`spring-ai-starter-model-*`)
- **Agent Loop** — Autonomous execution with automatic tool calling, parallel/sequential execution modes, and iterative refinement
- **Tool System** — Define tools via interface or annotations with built-in support for parallel execution and result aggregation
- **Memory System** — File-based persistent memory (long-term, short-term, user preferences) with automatic consolidation and prompt injection
- **Session Tree & Sub-Agents** — Spawn isolated child agents with inherited snapshots, session tree tracking, and forked memory stores
- **Personality System** — Configure agent personality (name, role, tone, style, boundaries) via JSON or Markdown (SOUL.md format)
- **Task Planning** — LLM-driven autonomous task decomposition, execution, and review with automatic replanning
- **Event-Driven Architecture** — Complete lifecycle events (start/end/tool/message/turn) for observability and custom hooks
- **Streaming Output** — SSE-based streaming responses for real-time interaction with delta calculation
- **Observability** — Built-in metrics collection with Micrometer/Prometheus integration support
- **Retry Policy** — Exponential backoff retry mechanism for resilient tool execution
- **Spring Boot Integration** — Auto-configuration with starter, property-based configuration, and seamless ChatModel adaptation

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                         │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐                  │
│  │ REST API │  │WebSocket │  │ Message Queue │                  │
│  └────┬─────┘  └────┬─────┘  └───────┬───────┘                  │
│       └──────────────┼───────────────────┘                      │
│                      ▼                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                      Agent (Core)                        │   │
│  │  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │   │
│  │  │AgentLoop│  │AgentState│  │ Steering │  │RetryPolicy│ │   │
│  │  └────┬────┘  └──────────┘  └──────────┘  └──────────┘ │   │
│  │       ▼                                                     │   │
│  │  ┌─────────────────────────────────────────────────────┐   │   │
│  │  │                 AgentContext                        │   │   │
│  │  │ Messages + Attributes + Session Tree Metadata       │   │   │
│  │  └─────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                      │                                            │
│       ┌──────────────┼──────────────┐                             │
│       ▼              ▼              ▼                             │
│  ┌─────────┐  ┌──────────┐  ┌──────────────┐                     │
│  │ChatModel│  │  Tools   │  │EventPublisher│                     │
│  └────┬────┘  └──────────┘  └──────┬───────┘                     │
│       ▼                            ▼                              │
│  ┌──────────────────────┐  ┌─────────────────┐                   │
│  │ LlmProviderRegistry  │  │ EventListeners  │                   │
│  │  ┌────┬────┬────┬───┐│  │  (Metrics, etc) │                   │
│  │  │Open│Anth│Zhip│Mini││  └─────────────────┘                   │
│  │  │ AI │ropic│ pu │Max ││                                       │
│  │  └────┴────┴────┴───┘│                                       │
│  └──────────────────────┘                                       │
│                      │                                            │
│       ┌──────────────┼──────────────┐                             │
│       ▼              ▼              ▼                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │MemoryStore│  │Personality│  │TaskPlanner   │  │ SessionTree  │  │
│  │ (Memory)  │  │(Soul)    │  │ (Autonomous) │  │ + SubAgent   │  │
│  └──────────┘  └──────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**Design Principles:**

- **Layered Decoupling** — Core layer has zero Spring dependencies; Spring layer handles bridging
- **Provider Abstraction** — Unified `LlmProvider` interface for all LLM providers
- **Event-Driven** — Lifecycle events at all key points for extensibility
- **Extensible** — Tools, transformers, and listeners defined via interfaces
- **Markdown-First** — Memory and personality stored as Markdown for LLM/human readability
- **Runtime Flexibility** — Switch providers at runtime without code changes

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.easyagent4j</groupId>
    <artifactId>easyagent4j-spring-boot-starter</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</dependency>

<!-- Choose your provider implementation -->

<!-- Option A: Spring AI Integration (Recommended) -->
<dependency>
    <groupId>com.easyagent4j</groupId>
    <artifactId>provider-springai-zhipu</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</dependency>

<!-- Option B: HTTP Implementation -->
<dependency>
    <groupId>com.easyagent4j</groupId>
    <artifactId>provider-http-zhipu</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure

**Spring AI Integration:**

```yaml
spring:
  ai:
    model:
      chat: zhipuai  # Required for Spring AI auto-configuration
    zhipuai:
      api-key: ${ZHIPU_API_KEY}
      chat:
        options:
          model: glm-4

easyagent:
  system-prompt: "You are a helpful assistant."
  max-tool-iterations: 10
  tool-execution-mode: PARALLEL  # PARALLEL | SEQUENTIAL
  streaming: true
  autonomous-mode: false
  chat-provider: zhipu
  
  # Memory system
  memory:
    enabled: true
    base-path: ./agent-memory
    auto-consolidate: false
  
  # Personality
  personality:
    name: "Assistant"
    personality-path: classpath:personality.md
```

**HTTP Implementation:**

```yaml
easyagent:
  system-prompt: "You are a helpful assistant."
  max-tool-iterations: 10
  tool-execution-mode: PARALLEL  # PARALLEL | SEQUENTIAL
  streaming: true
  autonomous-mode: false
  chat-provider: zhipu
  
  provider:
    zhipu:
      enabled: true
      base-url: https://open.bigmodel.cn/api/paas/v4
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
      max-tokens: 4096
      temperature: 0.7
  
  # Memory system
  memory:
    enabled: true
    base-path: ./agent-memory
    auto-consolidate: false
  
  # Personality
  personality:
    name: "Assistant"
    personality-path: classpath:personality.md
  
  # Context
  context:
    max-messages: 50
```

### 3. Define Tools

```java
@Component
public class WeatherTool extends AbstractAgentTool {
    @Override
    public String getName() { return "get_weather"; }

    @Override
    public String getDescription() { return "Get current weather for a city"; }

    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "city": { "type": "string" }
              },
              "required": ["city"]
            }
            """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String city = context.getStringArg("city");
        return ToolResult.success("Weather in " + city + ": Sunny, 25°C");
    }
}
```

### 4. Use Agent

```java
@RestController
public class ChatController {

    @Autowired
    private Agent agent;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) throws Exception {
        AgentContext context = agent.prompt(message).get();
        return context.getLastMessage().getContent().toString();
    }
    
    // Runtime provider switching
    @PostMapping("/switch-provider")
    public String switchProvider(@RequestParam String provider) {
        registry.setDefault(provider);
        return "Switched to " + provider;
    }
}
```

## Session Tree And Sub-Agent

EasyAgent4j now supports session branching and isolated sub-agents.

```java
Agent root = new Agent(
    AgentConfig.builder().sessionId("root-session").build(),
    chatModel
);

root.setMemoryStore(new FileMemoryStore("./agent-memory", root.getContext().getSessionId()));
root.addTool(new SubAgentTool(root));

Agent child = root.spawnSubAgent("root-session/research-1", true);
child.prompt("请在隔离上下文里完成技术调研").get();

System.out.println(child.getContext().getParentSessionId()); // root-session
System.out.println(child.getContext().getRootSessionId());   // root-session
System.out.println(root.getSessionTree().getChildren("root-session"));
```

Built-in `sub_agent` tool:

```json
{
  "task": "请单独分析这个模块的重构方案",
  "childSessionId": "root-session/refactor-1",
  "inheritMessages": true
}
```

Related example:
- `easyagent4j-example-tools/src/main/java/com/easyagent4j/example/SessionTreeExample.java`

Web example endpoints:
- `POST /api/chat` with `session_id`
- `POST /api/sessions/sub-agent` with `parent_session_id`, `session_id`, `inherit_messages`
- `GET /api/sessions/{sessionId}/tree`

### Web Session Tree API Example

Start the web example first:

```bash
mvn -pl easyagent4j-examples/easyagent4j-example-web spring-boot:run
```

Open the built-in demo page:

```text
http://localhost:8080/
```

Create or continue a root session:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "session_id": "demo-root",
    "message": "请先总结主任务，再给我一个下一步建议"
  }'
```

Spawn a sub-agent with isolated context:

```bash
curl -X POST http://localhost:8080/api/sessions/sub-agent \
  -H 'Content-Type: application/json' \
  -d '{
    "parent_session_id": "demo-root",
    "session_id": "demo-root/research-1",
    "inherit_messages": true,
    "message": "请在隔离上下文中只做技术调研，不要改写主任务结论"
  }'
```

Query the current session tree metadata:

```bash
curl http://localhost:8080/api/sessions/demo-root/tree
```

SSE streaming with a fixed session:

```bash
curl -N 'http://localhost:8080/api/chat/stream?query=%E8%AF%B7%E6%B5%81%E5%BC%8F%E5%9B%9E%E5%A4%8D%E5%BD%93%E5%89%8D%E8%BF%9B%E5%BA%A6&session_id=demo-root'
```

Typical response fields:
- `session_id`: current node id
- `parent_session_id`: parent node id, null for root
- `root_session_id`: root node id
- `child_session_ids`: current node's direct children

## Multi-Model Provider Configuration

EasyAgent4j supports multiple LLM providers with **dual implementation options**:

1. **HTTP Implementation** (`provider-http-*`) — Direct HTTP API calls, lightweight with no Spring AI dependency
2. **Spring AI Integration** (`provider-springai-*`) — Leverages Spring AI starters for managed configuration and features

### Provider Modules

| Provider | HTTP Module | Spring AI Module | Supported Models |
|----------|-------------|------------------|------------------|
| **OpenAI** | `provider-http-openai` | `provider-springai-openai` | GPT-4o, GPT-4, GPT-3.5 (also DeepSeek, Qwen, Moonshot) |
| **Anthropic** | `provider-http-anthropic` | `provider-springai-anthropic` | Claude 3.5 Sonnet, Claude 3 Opus |
| **Zhipu GLM** | `provider-http-zhipu` | `provider-springai-zhipu` | GLM-4, GLM-3 |
| **MiniMax** | `provider-http-minimax` | `provider-springai-minimax` | abab6.5s, abab6.5 |

### Choose Your Implementation

**HTTP Implementation** — Best for:
- Minimal dependencies
- Custom HTTP configurations
- Direct API control

**Spring AI Integration** — Best for:
- Spring AI ecosystem features
- Managed connection pooling
- Built-in observability and tracing

### Using Spring AI Integration

```xml
<!-- Add Spring AI provider -->
<dependency>
    <groupId>com.easyagent4j</groupId>
    <artifactId>provider-springai-zhipu</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</dependency>
```

```yaml
spring:
  ai:
    model:
      chat: zhipuai  # Required for Spring AI auto-configuration
    zhipuai:
      api-key: ${ZHIPU_API_KEY}
      chat:
        options:
          model: glm-4

easyagent:
  chat-provider: zhipu
```

### Using HTTP Implementation

```xml
<dependency>
    <groupId>com.easyagent4j</groupId>
    <artifactId>provider-http-zhipu</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</dependency>
```

```yaml
easyagent:
  chat-provider: zhipu
  provider:
    zhipu:
      enabled: true
      base-url: https://open.bigmodel.cn/api/paas/v4
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
```

### Runtime Provider Switching

```java
@Autowired
private LlmProviderRegistry registry;

// Switch to Anthropic
registry.setDefault("anthropic");

// Switch to Zhipu
registry.setDefault("zhipu");

// Switch back to OpenAI
registry.setDefault("openai");
```

### Provider-Specific Configuration

Each provider can be configured independently:

```yaml
easyagent:
  chat-provider: openai  # Default provider
  
  provider:
    openai:
      enabled: true
      base-url: https://api.openai.com  # Change for OpenAI-compatible APIs
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o
      max-tokens: 4096
      temperature: 0.7
      timeout-seconds: 60
    anthropic:
      enabled: false
      base-url: https://api.anthropic.com
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-sonnet-4-20250514
    zhipu:
      enabled: false
      base-url: https://open.bigmodel.cn/api/paas/v4
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
    minimax:
      enabled: false
      api-key: ${MINIMAX_API_KEY}
      model: abab6.5s-chat
```

**Note:** The OpenAI provider is compatible with OpenAI-format APIs (DeepSeek, Qwen, Moonshot, etc.). Simply change the `base-url` to the target API endpoint.

## Core Concepts

### Agent

The `Agent` is the core abstraction. It encapsulates the LLM conversation loop, handling tool calls, message management, and multi-turn interactions:

```java
AgentConfig config = AgentConfig.builder()
    .systemPrompt("You are a helpful assistant.")
    .maxToolIterations(10)
    .streamingEnabled(true)
    .toolExecutionMode(ToolExecutionMode.PARALLEL)
    .build();

Agent agent = new Agent(config, chatModel);
agent.addTool(myTool);

AgentContext context = agent.prompt("What's the weather in Beijing?").get();
String response = context.getLastMessage().getContent().toString();
```

### Tools

Define tools to extend agent capabilities:

**Interface-based:**

```java
@Component
public class CalculatorTool extends AbstractAgentTool {
    @Override
    public String getName() { return "calculate"; }
    
    @Override
    public String getDescription() { return "Perform mathematical calculations"; }
    
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "expression": { "type": "string" }
              },
              "required": ["expression"]
            }
            """;
    }
    
    @Override
    protected ToolResult doExecute(ToolContext context) {
        String expression = context.getStringArg("expression");
        double result = evaluate(expression);
        return ToolResult.success(String.valueOf(result));
    }
}
```

### Memory System

File-based persistent memory supporting long-term, short-term, and user preferences:

```java
AgentConfig config = AgentConfig.builder()
    .memory(new AgentConfig.MemoryConfig("./agent-memory", true, false))
    .build();

Agent agent = new Agent(config, chatModel);
agent.setMemoryStore(new FileMemoryStore("./agent-memory", agent.getContext().getSessionId()));

AgentContext context = agent.prompt("Remember that I prefer Python for data analysis").get();
String response = context.getLastMessage().getContent().toString();
```

Memory types:
- **Long-term**: Persistent facts and preferences
- **Short-term**: Temporary context for current session
- **User Preferences**: User-specific settings and choices

### Personality System

Define agent personality via JSON or Markdown:

**personality.md:**
```markdown
# Agent Personality

## Name
Claude Assistant

## Role
Helpful AI assistant

## Tone
Professional, friendly, concise

## Style
- Use clear, simple language
- Provide actionable advice
- Ask clarifying questions when needed

## Boundaries
- Do not provide medical or legal advice
- Do not engage in harmful activities
- Respect user privacy
```

```yaml
easyagent:
  personality:
    personality-path: classpath:personality.md
```

### Autonomous Task Loop

Enable autonomous mode for LLM-driven task planning and execution:

```yaml
easyagent:
  autonomous-mode: true
```

The agent will:
1. Decompose tasks into subtasks
2. Execute tools in sequence
3. Monitor progress and replan if needed
4. Review results and provide final output

```java
AgentConfig config = AgentConfig.builder()
    .autonomousMode(true)
    .build();

Agent agent = new Agent(config, chatModel);
agent.addTool(myTool);

AgentContext result = agent.executeAutonomous("Analyze recent sales data and generate a report").get();
// Agent automatically breaks down the task, executes tools, and generates report
```

### Event System

Subscribe to agent lifecycle events for logging, monitoring, and custom logic:

```java
@Component
public class LoggingEventListener implements AgentEventListener {
    
    @Override
    public void onEvent(AgentEvent event) {
        switch (event.getType()) {
            case AGENT_START:
                log.info("Agent started: {}", event.getSessionId());
                break;
            case TOOL_EXECUTION_START:
                log.info("Tool execution started: {}", event.getToolName());
                break;
            case ERROR:
                log.error("Agent error: {}", event.getError().getMessage());
                break;
        }
    }
}
```

**Event Types:**
- `AGENT_START` / `AGENT_END`
- `TURN_START` / `TURN_END`
- `MESSAGE_START` / `MESSAGE_UPDATE` / `MESSAGE_END`
- `TOOL_EXECUTION_START` / `TOOL_EXECUTION_UPDATE` / `TOOL_EXECUTION_END`
- `ERROR`

**Note:** When subscribing to events, clear previous listeners to avoid duplicate processing:

```java
// Clear old listeners before subscribing new ones
agent.clearListeners();
agent.subscribe(event -> {
    if (event instanceof MessageUpdateEvent e) {
        System.out.print(e.getDelta());
    }
});
```

### Observability

Built-in metrics collection with Micrometer/Prometheus support:

```java
@Autowired
private AgentMetrics metrics;

// Get metrics snapshot
Map<String, Number> snapshot = metrics.getMetricsSnapshot();
// => {easyagent.agent.runs=5, easyagent.tokens.input=1200, easyagent.tool.executions=12, ...}
```

Metrics collected:
- Agent runs
- Turn counts
- Token usage (input/output)
- Tool executions
- Tool failures
- Response times

## Modules

| Module | Description |
|--------|-------------|
| **easyagent4j-core** | Core runtime: Agent, AgentLoop, Provider abstraction, Tools, Events, Memory, Personality, Task planning |
| **easyagent4j-providers** | Parent module for all provider implementations |
| **provider-http-*** | HTTP-based provider implementations (openai, anthropic, zhipu, minimax) |
| **provider-springai-*** | Spring AI integration providers (openai, anthropic, zhipu, minimax) |
| **provider-springai-core** | Core adapter for Spring AI ChatModel to LlmProvider interface |
| **easyagent4j-spring** | Spring integration: AutoConfiguration, ChatModel adaptation, Observability |
| **easyagent4j-spring-boot-starter** | Spring Boot starter with auto-configuration |
| **easyagent4j-examples** | Example applications |

## Configuration Reference

### Core Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `easyagent.system-prompt` | `You are a helpful assistant.` | System prompt for the agent |
| `easyagent.max-tool-iterations` | `10` | Maximum tool iterations per turn |
| `easyagent.tool-execution-mode` | `PARALLEL` | Tool execution mode: `PARALLEL` or `SEQUENTIAL` |
| `easyagent.streaming` | `true` | Enable streaming output |
| `easyagent.autonomous-mode` | `false` | Enable autonomous task loop |
| `easyagent.chat-provider` | `openai` | Default LLM provider |

### Provider Configuration

| Property | Description |
|----------|-------------|
| `easyagent.provider.openai.enabled` | Enable OpenAI provider |
| `easyagent.provider.openai.base-url` | OpenAI API base URL |
| `easyagent.provider.openai.api-key` | OpenAI API key |
| `easyagent.provider.openai.model` | Model name (e.g., `gpt-4o`) |
| `easyagent.provider.openai.max-tokens` | Maximum output tokens |
| `easyagent.provider.openai.temperature` | Sampling temperature |
| `easyagent.provider.openai.timeout-seconds` | Request timeout |

Similar configuration blocks for `anthropic`, `zhipu`, and `minimax`.

### Memory Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `easyagent.memory.enabled` | `false` | Enable memory system |
| `easyagent.memory.base-path` | `./agent-memory` | Memory storage directory |
| `easyagent.memory.auto-consolidate` | `false` | Auto-consolidate memories |

### Personality Configuration

| Property | Description |
|----------|-------------|
| `easyagent.personality.name` | Agent name |
| `easyagent.personality.personality-path` | Path to personality file (JSON or Markdown) |

### Context Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `easyagent.context.max-messages` | `50` | Maximum messages in context (sliding window) |

## Advanced Usage

### Custom Message Transformer

Implement custom context management:

```java
@Component
public class TokenBudgetTransformer implements MessageTransformer {
    
    private final int maxTokens;
    
    public TokenBudgetTransformer(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    @Override
    public List<Message> transform(List<Message> messages) {
        // Implement token budget logic
        List<Message> filtered = new ArrayList<>();
        int currentTokens = 0;
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            int tokens = estimateTokens(messages.get(i));
            if (currentTokens + tokens > maxTokens) break;
            filtered.add(0, messages.get(i));
            currentTokens += tokens;
        }
        
        return filtered;
    }
}
```

### Custom Retry Policy

```java
@Bean
public RetryPolicy customRetryPolicy() {
    return RetryPolicy.builder()
        .maxRetries(5)
        .initialBackoffMillis(1000)
        .maxBackoffMillis(30000)
        .backoffMultiplier(2.0)
        .retryableExceptions(IOException.class, TimeoutException.class)
        .build();
}
```

### Streaming with SSE

EasyAgent4j handles both cumulative and incremental streaming modes automatically:

- **Cumulative mode** — Each chunk contains all content from start (e.g., Zhipu GLM)
- **Incremental mode** — Each chunk contains only new content (e.g., OpenAI)

The framework automatically detects and converts to incremental output.

```java
@RestController
public class StreamingController {
    
    @Autowired
    private Agent agent;
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        return agent.stream(message)
            .map(chunk -> "data: " + chunk.getContent() + "\n\n");
    }
}
```

## Examples

See the `easyagent4j-examples` module for complete examples:

- **basic** — Simple chat with agent
- **tools** — Tool calling example
- **web** — REST API with streaming

## Testing

The project includes comprehensive unit tests. Run tests with:

```bash
mvn test
```

## License

[MIT License](LICENSE) © 2026 Chen Jiaming

## Acknowledgments

Inspired by [OpenClaw](https://github.com/zhilei/openclaw) — Agent-driven development framework.
