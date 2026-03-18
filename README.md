# EasyAgent4j

> A lightweight Java Agent framework inspired by OpenClaw — Build stateful, observable, tool-enabled AI agents with minimal effort.

[![Java 17+](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Overview

EasyAgent4j is a Java framework for building AI agents with multi-turn conversations, tool calling, memory, personality, and autonomous task execution. Inspired by [OpenClaw](https://github.com/zhilei/openclaw), it provides a clean abstraction layer over LLM APIs with Spring Boot integration, enabling developers to focus on business logic rather than API complexity.

## Features

- **Multi-Model Provider Architecture** — Support for OpenAI, Anthropic Claude, Zhipu GLM, and MiniMax with runtime switching
- **Agent Loop** — Autonomous execution with automatic tool calling, parallel/sequential execution modes, and iterative refinement
- **Tool System** — Define tools via interface or annotations with built-in support for parallel execution and result aggregation
- **Memory System** — File-based persistent memory (long-term, short-term, user preferences) with automatic consolidation and prompt injection
- **Personality System** — Configure agent personality (name, role, tone, style, boundaries) via JSON or Markdown (SOUL.md format)
- **Task Planning** — LLM-driven autonomous task decomposition, execution, and review with automatic replanning
- **Event-Driven Architecture** — Complete lifecycle events (start/end/tool/message/turn) for observability and custom hooks
- **Streaming Output** — SSE-based streaming responses for real-time interaction
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
│  │  │      Messages + Attributes + SessionId              │   │   │
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
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐                    │
│  │MemoryStore│  │Personality│  │TaskPlanner   │                    │
│  │ (Memory)  │  │(Soul)    │  │ (Autonomous) │                    │
│  └──────────┘  └──────────┘  └──────────────┘                    │
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

<!-- Add provider(s) as needed -->
<dependency>
    <groupId>com.easyagent4j</groupId>
    <artifactId>easyagent4j-provider-openai</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure

```yaml
easyagent:
  system-prompt: "You are a helpful assistant."
  max-tool-iterations: 10
  tool-execution-mode: PARALLEL  # PARALLEL | SEQUENTIAL
  streaming: true
  autonomous-mode: false
  
  # Multi-model provider configuration
  chat-provider: openai  # Default provider
  provider:
    openai:
      enabled: true
      base-url: https://api.openai.com
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o
      max-tokens: 4096
      temperature: 0.7
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
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "Get current weather for a city";
    }

    @Override
    public ToolResult execute(ToolCall toolCall, ToolContext context) {
        String city = parseArgument(toolCall, "city");
        // Call weather API...
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
    public String chat(@RequestParam String message) {
        return agent.chat(message);
    }
    
    // Runtime provider switching
    @PostMapping("/switch-provider")
    public String switchProvider(@RequestParam String provider) {
        registry.setDefault(provider);
        return "Switched to " + provider;
    }
}
```

## Multi-Model Provider Configuration

EasyAgent4j supports multiple LLM providers through a unified abstraction layer. Enable providers as needed and switch between them at runtime.

### Provider Modules

| Provider | Module | Supported Models | Compatibility |
|----------|--------|------------------|---------------|
| **OpenAI** | `easyagent4j-provider-openai` | GPT-4o, GPT-4, GPT-3.5 | OpenAI API format (compatible with DeepSeek, Qwen, Moonshot, etc.) |
| **Anthropic** | `easyagent4j-provider-anthropic` | Claude 3.5 Sonnet, Claude 3 Opus | Anthropic Messages API |
| **Zhipu GLM** | `easyagent4j-provider-zhipu` | GLM-4, GLM-3 | OpenAI-compatible API |
| **MiniMax** | `easyagent4j-provider-minimax` | abab6.5s, abab6.5 | MiniMax Chat API |

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
```

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
agent.registerTool(myTool);

String response = agent.chat("What's the weather in Beijing?");
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
    public ToolResult execute(ToolCall toolCall, ToolContext context) {
        String expression = parseArgument(toolCall, "expression");
        double result = evaluate(expression);
        return ToolResult.success(String.valueOf(result));
    }
}
```

### Memory System

File-based persistent memory supporting long-term, short-term, and user preferences:

```java
// Configure memory
MemoryConfig memoryConfig = MemoryConfig.builder()
    .basePath("./agent-memory")
    .enabled(true)
    .autoConsolidate(false)
    .build();

AgentConfig config = AgentConfig.builder()
    .memory(memoryConfig)
    .build();

// The agent automatically saves interactions and retrieves relevant memories
String response = agent.chat("Remember that I prefer Python for data analysis");
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
agent.registerTool(myTool);

String result = agent.chat("Analyze recent sales data and generate a report");
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
| **easyagent4j-provider-openai** | OpenAI provider (compatible with OpenAI, DeepSeek, Qwen, Moonshot, etc.) |
| **easyagent4j-provider-anthropic** | Anthropic Claude provider |
| **easyagent4j-provider-zhipu** | Zhipu GLM provider |
| **easyagent4j-provider-minimax** | MiniMax provider |
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