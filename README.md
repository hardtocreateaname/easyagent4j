# EasyAgent4j

> 基于 Spring AI 的轻量级 Java Agent 框架 — 让 Java 开发者以最小成本构建有状态、可观测、支持工具调用的 AI Agent 应用。

[![Java 17+](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI 1.1](https://img.shields.io/badge/Spring%20AI-1.1.2-blue.svg)](https://spring.io/projects/spring-ai)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ 特性

- 🤖 **有状态 Agent 运行时** — 内置会话管理、消息历史维护、多轮对话支持
- 🔧 **灵活的工具系统** — 支持 Function Calling，可通过接口或注解定义工具，支持并行/串行执行
- 📡 **事件驱动架构** — 完整的 Agent 生命周期事件（start/end/tool/message/turn），可插拔的事件监听器
- 🔀 **流式输出** — 支持 SSE 流式响应，实时返回 LLM 生成内容
- 🛡️ **Steering 机制** — 运行时动态注入系统提示词，引导 Agent 行为方向
- 📊 **可观测性** — 内置指标采集接口，零依赖默认实现，可无缝接入 Micrometer/Prometheus
- 🎯 **注解式工具定义** — 使用 `@AgentTool` 注解快速将 Spring Bean 方法暴露为 Agent 工具
- 🧠 **上下文管理** — 可插拔的消息转换器，支持滑动窗口、Token 预算等策略
- 🚀 **Spring Boot Starter** — 开箱即用的自动配置，引入依赖即可使用

## 🚀 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.easyagent4j</groupId>
    <artifactId>easyagent4j-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

在 `application.yml` 中添加配置（可选，以下为默认值）：

```yaml
easyagent:
  system-prompt: "You are a helpful assistant."
  max-tool-iterations: 10
  tool-execution-mode: PARALLEL  # PARALLEL | SEQUENTIAL
  streaming: true
  context:
    max-messages: 50
```

确保项目中已配置 Spring AI 的 ChatModel（如 OpenAI、通义千问等）：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
```

### 3. 定义工具

通过实现 `AgentTool` 接口：

```java
@Component
public class WeatherTool extends AbstractAgentTool {

    @Override
    public String getName() { return "get_weather"; }

    @Override
    public String getDescription() { return "Get current weather for a city"; }

    @Override
    public ToolResult execute(ToolCall toolCall, ToolContext context) {
        String city = parseArgument(toolCall, "city");
        // 调用天气API...
        return ToolResult.success("Weather in " + city + ": Sunny, 25°C");
    }
}
```

### 4. 启动并运行

```java
@RestController
public class ChatController {

    @Autowired
    private Agent agent;

    @Autowired
    private AgentEventPublisher eventPublisher;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // 订阅事件（如指标采集）
        // eventPublisher.subscribe(metricsEventListener);

        String response = agent.chat(message);
        return response;
    }
}
```

## 📦 模块说明

| 模块 | 说明 |
|------|------|
| **easyagent4j-core** | 核心模块：Agent 运行时、工具系统、事件模型、消息抽象、上下文管理 |
| **easyagent4j-spring** | Spring AI 集成层：ChatModel 适配器、自动配置、可观测性 |
| **easyagent4j-spring-boot-starter** | Spring Boot Starter：开箱即用的自动装配 |
| **easyagent4j-examples** | 示例项目：基础对话、工具调用等 |

```
easyagent4j/
├── easyagent4j-core/                    # 核心模块
│   ├── agent/                           # Agent 运行时（Agent, AgentLoop, AgentConfig, AgentState）
│   ├── chat/                            # Chat 模型抽象（ChatModel, ChatRequest, ChatResponse）
│   ├── context/                         # 上下文与消息转换
│   ├── event/                           # 事件系统（Publisher, Listener, 各类事件）
│   ├── message/                         # 消息模型（User, Assistant, System, ToolResult）
│   ├── tool/                            # 工具系统（AgentTool, ToolDefinition, ToolHook）
│   └── exception/                       # 异常体系
├── easyagent4j-spring/                  # Spring AI 集成
│   ├── autoconfigure/                   # 自动配置
│   ├── chat/                            # Spring AI ChatModel 适配
│   ├── message/                         # 消息格式转换
│   ├── observability/                   # 可观测性（指标采集）
│   ├── properties/                      # 配置属性
│   └── tool/                            # 工具适配
├── easyagent4j-spring-boot-starter/     # Starter 模块
└── easyagent4j-examples/                # 示例项目
```

## 🧩 核心概念

### Agent

`Agent` 是框架的核心。它封装了 LLM 对话循环，自动处理工具调用、消息管理和多轮交互：

```java
AgentConfig config = AgentConfig.builder()
    .systemPrompt("You are a helpful assistant.")
    .maxToolIterations(10)
    .streamingEnabled(true)
    .build();

Agent agent = new Agent(config, chatModel);
agent.registerTool(myTool);

String response = agent.chat("What's the weather in Beijing?");
```

### 工具（Tools）

Agent 可以调用外部工具来扩展能力。框架提供两种工具定义方式：

**接口方式：** 继承 `AbstractAgentTool`，实现 `getName()`、`getDescription()`、`execute()`

**注解方式（规划中）：** 使用 `@AgentTool` 注解标注 Spring Bean 方法

### 事件系统

完整的 Agent 生命周期事件，可用于日志、监控、审计等：

| 事件 | 说明 |
|------|------|
| `agent_start` | Agent 开始运行 |
| `agent_end` | Agent 运行结束 |
| `turn_start` | 新一轮对话开始 |
| `turn_end` | 一轮对话结束 |
| `message_start` | 消息开始生成 |
| `message_update` | 流式消息更新 |
| `message_end` | 消息生成完成 |
| `tool_execution_start` | 工具开始执行 |
| `tool_execution_update` | 工具执行进度更新 |
| `tool_execution_end` | 工具执行结束 |
| `error` | 错误事件 |

### 可观测性

内置 `AgentMetrics` 接口和 `DefaultAgentMetrics` 内存实现，自动通过 `MetricsEventListener` 采集指标：

```java
@Autowired
private DefaultAgentMetrics metrics;

// 获取指标快照
Map<String, Number> snapshot = metrics.getMetricsSnapshot();
// => {easyagent.agent.runs=5, easyagent.tokens.input=1200, easyagent.tool.executions=12, ...}
```

当项目引入 Micrometer 时，可实现 `AgentMetrics` 接口桥接到 Prometheus/Grafana。

### Steering 机制

运行时动态修改 Agent 行为方向：

```java
agent.applySteering(AgentSteering.of(
    "请用简洁的中文回答后续所有问题。"
));
```

## ⚙️ 配置参考

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `easyagent.system-prompt` | `You are a helpful assistant.` | 系统提示词 |
| `easyagent.max-tool-iterations` | `10` | 单轮最大工具迭代次数 |
| `easyagent.tool-execution-mode` | `PARALLEL` | 工具执行模式：`PARALLEL`（并行）/ `SEQUENTIAL`（串行） |
| `easyagent.streaming` | `true` | 是否启用流式输出 |
| `easyagent.context.max-messages` | `50` | 上下文最大消息数（滑动窗口） |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────┐
│                  Application                      │
│                                                   │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ REST API  │  │ WebSocket│  │  Message Queue│  │
│  └────┬─────┘  └────┬─────┘  └──────┬────────┘  │
│       └──────────────┼───────────────┘            │
│                      ▼                            │
│  ┌───────────────────────────────────────────┐   │
│  │              Agent (core)                  │   │
│  │  ┌─────────┐  ┌──────────┐  ┌──────────┐  │   │
│  │  │AgentLoop│  │AgentState│  │ Steering │  │   │
│  │  └────┬────┘  └──────────┘  └──────────┘  │   │
│  │       ▼                                     │   │
│  │  ┌─────────────────────────────────────┐   │   │
│  │  │         AgentContext                 │   │   │
│  │  │  Messages + Attributes + SessionId   │   │   │
│  │  └─────────────────────────────────────┘   │   │
│  └───────────────────────────────────────────┘   │
│                      │                            │
│       ┌──────────────┼──────────────┐             │
│       ▼              ▼              ▼             │
│  ┌─────────┐  ┌──────────┐  ┌──────────────┐    │
│  │ChatModel│  │  Tools   │  │EventPublisher│    │
│  │(SpringAI)│  │(Tools)   │  │  (Events)   │    │
│  └────┬────┘  └──────────┘  └──────┬───────┘    │
│       ▼                            ▼             │
│  ┌──────────┐              ┌────────────────┐    │
│  │ LLM API  │              │ EventListeners │    │
│  │(OpenAI等)│              │  (Metrics等)   │    │
│  └──────────┘              └────────────────┘    │
└─────────────────────────────────────────────────┘
```

**设计理念：**

- **分层解耦** — core 层零 Spring 依赖，spring 层负责桥接
- **事件驱动** — 所有生命周期关键节点发布事件，支持可插拔监听
- **可扩展** — 工具、消息转换器、事件监听器均通过接口定义，易于扩展
- **轻量优先** — 核心模块无外部依赖（除 SLF4J），按需引入 Spring AI

## 📄 License

[MIT License](LICENSE) © 2026 陈佳铭
