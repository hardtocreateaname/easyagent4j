# EasyAgent4j — 详细架构设计文档

> 版本: v1.0 | 作者: 小爪 | 日期: 2026-03-16

---

## 1. 项目概述

### 1.1 项目名称
**EasyAgent4j** — 基于 Spring AI 的简易 Java Agent 框架

### 1.2 项目定位
模拟 pi-mono（badlogic/pi-mono, ⭐24k）的核心设计理念，用 Java + Spring AI 生态重新实现。目标是让 Java 开发者能以最小成本构建有状态、可观测、支持工具调用的 AI Agent 应用。

### 1.3 核心原则
- **分层解耦**: core层纯Java，不依赖Spring框架；spring层做适配
- **接口驱动**: 核心抽象全部通过接口定义，便于扩展和替换
- **事件驱动**: 完整的生命周期事件流，支持同步/异步监听
- **流式优先**: 所有LLM交互默认支持流式输出
- **简洁实用**: 简易版，不过度设计，覆盖80%的使用场景

### 1.4 技术选型

| 项目 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 17+ |
| 构建工具 | Maven | 3.9+ |
| 框架 | Spring Boot | 3.3.x |
| AI框架 | Spring AI | 1.1.x |
| 默认LLM | OpenAI (兼容任何Spring AI支持的模型) | - |
| 测试 | JUnit 5 + Mockito | 5.10+ |
| JSON | Jackson | 2.17+ |
| 日志 | SLF4J + Logback | - |
| 模型参数校验 | JSON Schema (手动校验) | - |

---

## 2. 模块结构

```
easyagent4j/
├── pom.xml                                    # 父POM（依赖管理、插件管理）
├── README.md
├── LICENSE
│
├── easyagent4j-core/                          # 核心框架模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/easyagent4j/core/
│       │   │
│       │   ├── agent/                         # Agent运行时
│       │   │   ├── Agent.java                     # Agent主类（有状态运行时）
│       │   │   ├── AgentConfig.java               # Agent配置（Builder模式）
│       │   │   ├── AgentLoop.java                 # 核心循环逻辑
│       │   │   └── AgentState.java                # Agent运行状态枚举
│       │   │
│       │   ├── message/                       # 消息模型
│       │   │   ├── AgentMessage.java              # 消息顶层接口
│       │   │   ├── MessageRole.java               # 消息角色枚举
│       │   │   ├── ContentPart.java               # 内容片段（文本/图片/工具调用）
│       │   │   ├── UserMessage.java                # 用户消息
│       │   │   ├── AssistantMessage.java           # 助手消息（含工具调用请求）
│       │   │   ├── ToolResultMessage.java          # 工具执行结果消息
│       │   │   ├── SystemMessage.java              # 系统消息
│       │   │   └── MessageUtils.java               # 消息工具类
│       │   │
│       │   ├── tool/                           # 工具系统
│       │   │   ├── AgentTool.java                  # 工具定义接口
│       │   │   ├── AbstractAgentTool.java          # 工具抽象基类（简化实现）
│       │   │   ├── ToolDefinition.java             # 工具元数据（不可变，Builder模式）
│       │   │   ├── ToolContext.java                # 工具执行上下文
│       │   │   ├── ToolResult.java                 # 工具执行结果
│       │   │   ├── ToolExecutionMode.java          # 执行模式枚举（PARALLEL/SEQUENTIAL）
│       │   │   └── ToolHook.java                   # 工具钩子接口
│       │   │
│       │   ├── event/                          # 事件系统
│       │   │   ├── AgentEvent.java                 # 事件基类
│       │   │   ├── AgentEventListener.java         # 事件监听器接口
│       │   │   ├── AgentEventPublisher.java        # 事件发布器（同步）
│       │   │   └── events/
│       │   │       ├── AgentStartEvent.java
│       │   │       ├── AgentEndEvent.java
│       │   │       ├── TurnStartEvent.java
│       │   │       ├── TurnEndEvent.java
│       │   │       ├── MessageStartEvent.java
│       │   │       ├── MessageUpdateEvent.java     # 流式文本增量
│       │   │       ├── MessageEndEvent.java
│       │   │       ├── ToolExecutionStartEvent.java
│       │   │       ├── ToolExecutionUpdateEvent.java
│       │   │       ├── ToolExecutionEndEvent.java
│       │   │       └── ErrorEvent.java
│       │   │
│       │   ├── context/                        # 上下文管理
│       │   │   ├── AgentContext.java               # Agent上下文（消息历史+状态）
│       │   │   ├── MessageTransformer.java         # 上下文变换接口
│       │   │   ├── MessageConverter.java           # 消息格式转换接口
│       │   │   ├── transform/
│       │   │   │   ├── SlidingWindowTransformer.java   # 滑动窗口裁剪
│       │   │   │   ├── CompositeTransformer.java       # 组合变换器
│       │   │   │   └── TokenBudgetTransformer.java     # Token预算裁剪（Phase 2）
│       │   │   └── converter/
│       │   │       └── DefaultMessageConverter.java     # 默认转换器
│       │   │
│       │   ├── chat/                           # LLM调用抽象
│       │   │   ├── ChatModel.java                  # LLM调用接口（自己的抽象）
│       │   │   ├── ChatRequest.java                # 请求模型
│       │   │   ├── ChatResponse.java               # 响应模型
│       │   │   └── ChatResponseChunk.java          # 流式响应块
│       │   │
│       │   ├── callback/                       # 回调机制
│       │   │   ├── StreamCallback.java             # 流式输出回调
│       │   │   └── SteeringCallback.java           # 运行中干预回调
│       │   │
│       │   └── exception/                      # 异常体系
│       │       ├── AgentException.java
│       │       ├── ToolExecutionException.java
│       │       ├── ToolNotFoundException.java
│       │       ├── AgentAbortException.java
│       │       └── MaxIterationsExceededException.java
│       │
│       └── test/java/com/easyagent4j/core/
│           ├── agent/
│           │   └── AgentLoopTest.java           # 核心循环单元测试
│           ├── message/
│           │   └── MessageTest.java
│           └── tool/
│               └── ToolResultTest.java
│
├── easyagent4j-spring/                        # Spring AI集成模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/easyagent4j/spring/
│       │   │
│       │   ├── chat/                           # Spring AI ChatModel适配
│       │   │   ├── SpringAiChatModelAdapter.java     # 实现core层的ChatModel接口
│       │   │   └── SpringAiChatRequestBuilder.java   # 请求构建器
│       │   │
│       │   ├── message/                        # 消息格式转换
│       │   │   ├── SpringAiMessageConverter.java     # AgentMessage ↔ Spring AI Message
│       │   │   └── SpringAiContentMapper.java        # ContentPart ↔ Spring AI Media
│       │   │
│       │   ├── tool/                           # 工具适配
│       │   │   └── SpringAiToolAdapter.java          # AgentTool → Spring AI FunctionCallback
│       │   │
│       │   ├── autoconfigure/                  # 自动配置
│       │   │   └── EasyAgentAutoConfiguration.java
│       │   │
│       │   └── properties/                     # 配置属性
│       │       └── EasyAgentProperties.java
│       │
│       └── main/resources/
│           └── META-INF/spring/
│               ├── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
├── easyagent4j-spring-boot-starter/           # Spring Boot Starter
│   ├── pom.xml
│   └── src/main/resources/
│       └── META-INF/
│           └── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── easyagent4j-examples/                      # 示例项目
    ├── easyagent4j-example-basic/             # 基础对话示例
    │   ├── pom.xml
    │   └── src/main/java/com/easyagent4j/example/
    │       └── BasicChatExample.java
    │
    ├── easyagent4j-example-tools/             # 工具调用示例
    │   ├── pom.xml
    │   └── src/main/java/com/easyagent4j/example/
    │       ├── tools/
    │       │   ├── WeatherTool.java
    │       │   ├── FileReadTool.java
    │       │   └── CalculatorTool.java
    │       └── ToolCallExample.java
    │
    └── easyagent4j-example-web/               # Web API示例（流式SSE）
        ├── pom.xml
        └── src/main/java/com/easyagent4j/example/
            ├── controller/
            │   └── ChatController.java
            ├── config/
            │   └── AppConfig.java
            └── Application.java
```

---

## 3. 核心类详细设计

### 3.1 Agent — 有状态运行时

```java
package com.easyagent4j.core.agent;

/**
 * Agent主类 — 有状态的AI Agent运行时。
 * 
 * 职责：
 * 1. 维护消息历史和Agent状态
 * 2. 管理工具注册和生命周期
 * 3. 编排核心循环（AgentLoop）
 * 4. 发布生命周期事件
 * 5. 支持流式输出和中断
 * 
 * 线程安全：非线程安全，设计为单线程使用。
 *          多线程场景由外部加锁。
 */
public class Agent {

    // === 配置 ===
    private AgentConfig config;
    private ChatModel chatModel;              // LLM调用接口
    private List<AgentTool> tools;            // 注册的工具列表
    private MessageTransformer transformer;   // 上下文变换器
    private MessageConverter converter;       // 消息格式转换器
    private ToolHook toolHook;               // 工具钩子
    
    // === 状态 ===
    private AgentContext context;             // 消息历史
    private AgentState state;                 // IDLE / RUNNING / ABORTED
    private AbortController abortController;  // 中断控制器
    
    // === 事件 ===
    private AgentEventPublisher eventPublisher;
    
    // === 构造 ===
    public Agent(AgentConfig config);
    public Agent(AgentConfig config, ChatModel chatModel);
    
    // === 核心方法 ===
    
    /**
     * 发送用户消息并执行Agent循环。
     * 异步执行，返回CompletableFuture。
     * 事件通过subscribe监听。
     */
    public CompletableFuture<AgentContext> prompt(String userMessage);
    
    /**
     * 发送带图片附件的消息。
     */
    public CompletableFuture<AgentContext> prompt(String text, List<ContentPart> attachments);
    
    /**
     * 发送AgentMessage（支持自定义消息类型）。
     */
    public CompletableFuture<AgentContext> prompt(AgentMessage message);
    
    /**
     * 从当前上下文继续执行（用于错误恢复/重试）。
     * 前提：最后一条消息必须是user或tool_result。
     */
    public CompletableFuture<AgentContext> continueExecution();
    
    /**
     * 中断当前执行。
     */
    public void abort();
    
    /**
     * 等待执行完成。
     */
    public void waitForIdle() throws InterruptedException;
    
    // === 状态管理 ===
    
    public void setSystemPrompt(String prompt);
    public void setChatModel(ChatModel model);
    public void setTools(List<AgentTool> tools);
    public void addTool(AgentTool tool);
    public void removeTool(String toolName);
    public void setTransformer(MessageTransformer transformer);
    public void setConverter(MessageConverter converter);
    public void setToolHook(ToolHook hook);
    
    public void clearMessages();
    public void reset();
    
    // === 查询 ===
    public AgentContext getContext();
    public AgentState getState();
    public AgentConfig getConfig();
    public List<AgentMessage> getMessages();
    
    // === 事件 ===
    public void subscribe(AgentEventListener listener);
    public void unsubscribe(AgentEventListener listener);
}
```

### 3.2 AgentConfig — 配置（Builder模式）

```java
package com.easyagent4j.core.agent;

/**
 * Agent配置项。
 */
public class AgentConfig {
    
    private String systemPrompt;               // 系统提示词
    private int maxToolIterations;             // 最大工具调用轮数（防止无限循环）
    private ToolExecutionMode toolExecutionMode; // PARALLEL 或 SEQUENTIAL
    private boolean streamingEnabled;          // 是否启用流式输出
    private String sessionId;                  // 会话ID（用于缓存）
    
    // Builder
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private String systemPrompt = "You are a helpful assistant.";
        private int maxToolIterations = 10;
        private ToolExecutionMode toolExecutionMode = ToolExecutionMode.PARALLEL;
        private boolean streamingEnabled = true;
        private String sessionId;
        
        public Builder systemPrompt(String prompt) { ... }
        public Builder maxToolIterations(int n) { ... }
        public Builder toolExecutionMode(ToolExecutionMode mode) { ... }
        public Builder streamingEnabled(boolean enabled) { ... }
        public Builder sessionId(String id) { ... }
        public AgentConfig build() { ... }
    }
}
```

### 3.3 AgentLoop — 核心循环

```java
package com.easyagent4j.core.agent;

/**
 * Agent核心执行循环。
 * 
 * 循环逻辑：
 * 1. 将用户消息加入上下文
 * 2. transformContext() — 变换消息历史（裁剪、注入）
 * 3. convertToLlm() — 转换为LLM消息格式
 * 4. 调用ChatModel — 获取LLM响应
 * 5. 如果响应包含工具调用请求：
 *    a. beforeToolCall() 钩子 — 可阻止执行
 *    b. 执行工具（并行或串行）
 *    c. afterToolCall() 钩子 — 可修改结果
 *    d. 将工具结果加入上下文
 *    e. 回到步骤1，继续循环
 * 6. 如果响应是纯文本，循环结束
 * 
 * 终止条件：
 * - LLM返回纯文本（无工具调用）
 * - 达到maxToolIterations上限
 * - 用户调用abort()
 * - 抛出异常
 */
class AgentLoop {

    private final ChatModel chatModel;
    private final AgentConfig config;
    private final List<AgentTool> tools;
    private final MessageTransformer transformer;
    private final MessageConverter converter;
    private final ToolHook toolHook;
    private final AgentEventPublisher eventPublisher;
    private volatile boolean aborted;

    /**
     * 执行一轮完整的Agent循环。
     * 
     * @param userMessage 用户消息（如果是从continue调用则为null）
     * @param context 当前上下文（会被修改）
     * @return 最终的上下文状态
     */
    AgentContext execute(AgentMessage userMessage, AgentContext context) {
        
        // 1. 添加用户消息
        if (userMessage != null) {
            context.addMessage(userMessage);
        }
        
        // 2. 主循环
        int iteration = 0;
        while (iteration < config.getMaxToolIterations() && !aborted) {
            
            // 2.1 Turn开始
            eventPublisher.publish(new TurnStartEvent(context));
            
            // 2.2 变换上下文
            List<AgentMessage> transformed = transformer.transform(context.getMessages());
            
            // 2.3 转换为LLM格式
            List<?> llmMessages = converter.convert(transformed);
            
            // 2.4 构建请求
            ChatRequest request = ChatRequest.builder()
                .messages(llmMessages)
                .systemPrompt(config.getSystemPrompt())
                .tools(tools)  // 转换为LLM工具定义
                .streaming(config.isStreamingEnabled())
                .build();
            
            // 2.5 调用LLM
            AssistantMessage assistantMessage;
            if (config.isStreamingEnabled()) {
                assistantMessage = callWithStreaming(request);
            } else {
                assistantMessage = callWithoutStreaming(request);
            }
            context.addMessage(assistantMessage);
            
            // 2.6 检查是否有工具调用
            List<ToolCall> toolCalls = assistantMessage.getToolCalls();
            
            if (toolCalls == null || toolCalls.isEmpty()) {
                // 没有工具调用，循环结束
                eventPublisher.publish(new TurnEndEvent(context, List.of()));
                break;
            }
            
            // 2.7 执行工具
            List<ToolResultMessage> toolResults = executeTools(toolCalls, context);
            
            // 2.8 将工具结果加入上下文
            for (ToolResultMessage result : toolResults) {
                context.addMessage(result);
            }
            
            eventPublisher.publish(new TurnEndEvent(context, toolResults));
            iteration++;
        }
        
        if (iteration >= config.getMaxToolIterations()) {
            eventPublisher.publish(new ErrorEvent(
                "Max tool iterations (" + config.getMaxToolIterations() + ") exceeded"));
        }
        
        return context;
    }
    
    /**
     * 流式调用LLM，逐步发出MessageUpdateEvent。
     */
    private AssistantMessage callWithStreaming(ChatRequest request) {
        StringBuilder content = new StringBuilder();
        AssistantMessage message = new AssistantMessage();
        
        // 发出message_start
        eventPublisher.publish(new MessageStartEvent(message));
        
        // 流式读取
        chatModel.stream(request, chunk -> {
            content.append(chunk.getText());
            message.setContent(content.toString());
            eventPublisher.publish(new MessageUpdateEvent(message, chunk.getText()));
        });
        
        // 发出message_end
        eventPublisher.publish(new MessageEndEvent(message));
        return message;
    }
    
    /**
     * 执行工具列表。
     * PARALLEL模式：并行执行所有工具，beforeToolCall串行，实际执行并行。
     * SEQUENTIAL模式：逐个执行。
     */
    private List<ToolResultMessage> executeTools(
            List<ToolCall> toolCalls, AgentContext context) {
        
        if (config.getToolExecutionMode() == ToolExecutionMode.PARALLEL) {
            return executeToolsParallel(toolCalls, context);
        } else {
            return executeToolsSequential(toolCalls, context);
        }
    }
    
    /**
     * 并行执行工具：
     * 1. 串行执行beforeToolCall（钩子）
     * 2. 并行执行通过钩子的工具
     * 3. 串行执行afterToolCall（钩子）
     */
    private List<ToolResultMessage> executeToolsParallel(
            List<ToolCall> toolCalls, AgentContext context) {
        
        // Phase 1: beforeToolCall（串行）
        List<ToolExecution> executions = new ArrayList<>();
        for (ToolCall call : toolCalls) {
            ToolExecution exec = new ToolExecution(call);
            
            eventPublisher.publish(new ToolExecutionStartEvent(call));
            
            if (toolHook != null) {
                ToolHookResult hookResult = toolHook.beforeToolCall(call, context);
                if (hookResult.isBlocked()) {
                    exec.setBlocked(hookResult.getReason());
                    continue;
                }
            }
            
            AgentTool tool = findTool(call.getName());
            if (tool == null) {
                exec.setError(new ToolNotFoundException(call.getName()));
                continue;
            }
            
            exec.setTool(tool);
            executions.add(exec);
        }
        
        // Phase 2: 并行执行
        List<CompletableFuture<ToolExecution>> futures = executions.stream()
            .filter(e -> !e.isBlocked() && e.getTool() != null)
            .map(e -> CompletableFuture.supplyAsync(() -> {
                try {
                    ToolContext tc = new ToolContext(e.getToolCall(), context);
                    ToolResult result = e.getTool().execute(tc);
                    e.setResult(result);
                } catch (Exception ex) {
                    e.setError(ex);
                }
                return e;
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Phase 3: afterToolCall（串行）+ 构建结果消息
        List<ToolResultMessage> results = new ArrayList<>();
        for (ToolExecution exec : executions) {
            if (exec.isBlocked()) {
                results.add(ToolResultMessage.blocked(exec.getToolCall(), exec.getReason()));
            } else if (exec.getError() != null) {
                results.add(ToolResultMessage.error(exec.getToolCall(), exec.getError()));
                eventPublisher.publish(new ToolExecutionEndEvent(exec.getToolCall(), true));
            } else {
                if (toolHook != null) {
                    toolHook.afterToolCall(exec.getToolCall(), exec.getResult(), false, context);
                }
                results.add(ToolResultMessage.success(exec.getToolCall(), exec.getResult()));
                eventPublisher.publish(new ToolExecutionEndEvent(exec.getToolCall(), false));
            }
        }
        
        return results;
    }
}
```

### 3.4 AgentMessage — 统一消息模型

```java
package com.easyagent4j.core.message;

/**
 * 消息顶层接口。
 * 所有消息类型都实现此接口。
 * 
 * 设计参考pi-mono的AgentMessage，但使用Java的接口继承体系。
 */
public interface AgentMessage {
    
    /** 消息角色 */
    MessageRole getRole();
    
    /** 消息内容 */
    Object getContent();
    
    /** 时间戳 */
    long getTimestamp();
    
    /** 元数据（用于扩展） */
    Map<String, Object> getMetadata();
}

/**
 * 消息角色枚举。
 */
public enum MessageRole {
    USER,           // 用户消息
    ASSISTANT,      // 助手消息
    TOOL_RESULT,    // 工具执行结果
    SYSTEM          // 系统消息（通常不进入历史，作为配置）
}

/**
 * 内容片段 — 支持多模态。
 */
public class ContentPart {
    private ContentPartType type;    // TEXT, IMAGE, TOOL_CALL, TOOL_RESULT
    private String text;             // 文本内容
    private byte[] imageData;        // 图片数据
    private String mimeType;         // MIME类型
    private ToolCall toolCall;       // 工具调用请求
    // getters, builders...
}

/**
 * 用户消息。
 */
public class UserMessage implements AgentMessage {
    private final String content;              // 文本内容
    private final List<ContentPart> parts;     // 多模态内容（可选）
    private final long timestamp;
    private final Map<String, Object> metadata;
}

/**
 * 助手消息 — 可能包含文本和工具调用请求。
 */
public class AssistantMessage implements AgentMessage {
    private String textContent;                    // 文本回复
    private List<ToolCall> toolCalls;              // 工具调用请求列表
    private long timestamp;
    private Map<String, Object> metadata;
    
    public boolean hasToolCalls() { ... }
    public boolean isTextOnly() { ... }
}

/**
 * 工具调用请求 — 嵌套在AssistantMessage中。
 */
public class ToolCall {
    private String id;          // 工具调用ID（由LLM生成）
    private String name;        // 工具名称
    private String arguments;   // JSON格式的参数
    // getters...
}

/**
 * 工具执行结果消息。
 */
public class ToolResultMessage implements AgentMessage {
    private final ToolCall toolCall;           // 对应的ToolCall
    private final ToolResult result;           // 执行结果
    private final boolean isError;             // 是否是错误结果
    private final long timestamp;
}
```

### 3.5 AgentTool — 工具定义

```java
package com.easyagent4j.core.tool;

/**
 * 工具定义接口。
 * 
 * 使用者需要实现此接口来定义Agent可调用的工具。
 * 每个工具有名称、描述、参数JSON Schema和执行逻辑。
 */
public interface AgentTool {
    
    /** 工具名称（唯一标识，LLM通过此名称调用） */
    String getName();
    
    /** 工具描述（LLM通过此描述理解工具用途） */
    String getDescription();
    
    /** 
     * 参数JSON Schema。
     * 使用JSON Schema描述参数结构。
     * 例: {"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}
     */
    String getParameterSchema();
    
    /**
     * 执行工具。
     * @param context 工具执行上下文（包含参数、Agent上下文等）
     * @return 工具执行结果
     * @throws ToolExecutionException 执行出错时抛出
     */
    ToolResult execute(ToolContext context) throws ToolExecutionException;
}

/**
 * 工具抽象基类 — 提供便捷的实现方式。
 * 
 * 使用示例：
 * new AbstractAgentTool("read_file", "读取文件") {
 *     @Override
 *     protected String getParameterSchema() {
 *         return "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}";
 *     }
 *     
 *     @Override
 *     protected ToolResult doExecute(ToolContext ctx) {
 *         String path = ctx.getStringArg("path");
 *         return ToolResult.success(Files.readString(Path.of(path)));
 *     }
 * };
 */
public abstract class AbstractAgentTool implements AgentTool {
    
    private final String name;
    private final String description;
    
    protected AbstractAgentTool(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDescription() { return description; }
    
    @Override
    public final ToolResult execute(ToolContext context) {
        try {
            return doExecute(context);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    protected abstract ToolResult doExecute(ToolContext context);
    
    @Override
    public String getParameterSchema() {
        return "{}";  // 默认无参数
    }
}

/**
 * 工具执行上下文。
 */
public class ToolContext {
    private final ToolCall toolCall;          // 关联的ToolCall
    private final AgentContext agentContext;   // Agent上下文（可访问消息历史）
    private final Map<String, Object> arguments;  // 解析后的参数
    
    public String getStringArg(String name) { ... }
    public Integer getIntArg(String name) { ... }
    public Boolean getBoolArg(String name) { ... }
    public <T> T getArg(String name, Class<T> type) { ... }
    public String getRawArguments() { ... }  // 原始JSON
}

/**
 * 工具执行结果。
 */
public class ToolResult {
    private final boolean success;
    private final String content;             // 文本结果
    private final Map<String, Object> details; // 结构化详情
    
    public static ToolResult success(String content) { ... }
    public static ToolResult success(String content, Map<String, Object> details) { ... }
    public static ToolResult error(String errorMessage) { ... }
}

/**
 * 工具钩子接口 — 在工具执行前后插入自定义逻辑。
 */
public interface ToolHook {
    
    /**
     * 工具执行前调用。返回null表示允许执行，返回blocked结果表示阻止。
     */
    default ToolHookResult beforeToolCall(ToolCall toolCall, AgentContext context) {
        return null;
    }
    
    /**
     * 工具执行后调用。可修改结果。
     */
    default void afterToolCall(ToolCall toolCall, ToolResult result, 
                                boolean isError, AgentContext context) {
    }
}

public class ToolHookResult {
    private final boolean blocked;
    private final String reason;
    
    public static ToolHookResult block(String reason) { ... }
    public static ToolHookResult allow() { ... }
}
```

### 3.6 事件系统

```java
package com.easyagent4j.core.event;

/**
 * 事件基类。
 */
public class AgentEvent {
    private final String type;       // 事件类型标识
    private final long timestamp;    // 事件时间戳
    private final AgentContext context; // 当前Agent上下文（只读快照）
}

/**
 * 事件监听器接口。
 */
public interface AgentEventListener {
    void onEvent(AgentEvent event);
    
    // 便捷默认方法
    default void onMessageUpdate(MessageUpdateEvent event) {}
    default void onToolExecution(ToolExecutionStartEvent event) {}
    default void onError(ErrorEvent event) {}
}

/**
 * 事件发布器 — 同步发布。
 */
public class AgentEventPublisher {
    private final List<AgentEventListener> listeners = new CopyOnWriteArrayList<>();
    
    public void subscribe(AgentEventListener listener) { ... }
    public void unsubscribe(AgentEventListener listener) { ... }
    public void publish(AgentEvent event) {
        for (AgentEventListener l : listeners) {
            l.onEvent(event);
        }
    }
}
```

**事件类型枚举：**

| 事件类型 | 触发时机 | 关键数据 |
|---------|---------|---------|
| `AgentStartEvent` | prompt()开始执行 | - |
| `AgentEndEvent` | 全部循环结束 | 最终上下文 |
| `TurnStartEvent` | 每轮循环开始 | 当前上下文 |
| `TurnEndEvent` | 每轮循环结束 | 本轮工具结果 |
| `MessageStartEvent` | 消息开始（user/assistant） | 消息对象 |
| `MessageUpdateEvent` | 流式token到达 | 增量文本delta |
| `MessageEndEvent` | 消息完成 | 完整消息 |
| `ToolExecutionStartEvent` | 工具开始执行 | ToolCall |
| `ToolExecutionUpdateEvent` | 工具进度更新 | 进度信息 |
| `ToolExecutionEndEvent` | 工具执行完成 | ToolCall + 是否错误 |
| `ErrorEvent` | 发生错误 | 错误信息 |

### 3.7 ChatModel抽象 — core层的LLM接口

```java
package com.easyagent4j.core.chat;

/**
 * LLM调用接口 — core层定义自己的抽象。
 * spring层提供Spring AI的实现。
 * 用户也可以自行实现（如对接自定义后端）。
 */
public interface ChatModel {
    
    /**
     * 同步调用。
     */
    ChatResponse call(ChatRequest request);
    
    /**
     * 流式调用。
     * @param callback 每收到一个chunk回调一次。
     */
    void stream(ChatRequest request, Consumer<ChatResponseChunk> callback);
}

public class ChatRequest {
    private List<?> messages;              // LLM消息列表（由MessageConverter转换）
    private String systemPrompt;
    private List<ToolDefinition> tools;    // 工具定义列表
    private boolean streaming;
    private Map<String, Object> options;   // 额外选项（temperature, maxTokens等）
}

public class ChatResponse {
    private String text;
    private List<ToolCall> toolCalls;      // LLM请求的工具调用
    private Usage usage;                    // token使用量
}

public class ChatResponseChunk {
    private String text;                   // 增量文本
    private List<ToolCall> toolCalls;      // chunk中可能包含工具调用
    private boolean finished;
}
```

### 3.8 MessageTransformer — 上下文管理

```java
package com.easyagent4j.core.context;

/**
 * 消息变换器 — 在发送给LLM前对消息历史进行变换。
 * 
 * 典型用途：
 * - 裁剪过长的历史（滑动窗口）
 * - 基于token预算裁剪
 * - 注入外部上下文信息
 * - 对消息进行压缩/摘要
 */
public interface MessageTransformer {
    List<AgentMessage> transform(List<AgentMessage> messages);
}

/**
 * 滑动窗口变换器 — 保留最近N条消息。
 * 始终保留第一条SystemMessage（如果有）。
 */
public class SlidingWindowTransformer implements MessageTransformer {
    private final int maxMessages;
    
    public SlidingWindowTransformer(int maxMessages) {
        this.maxMessages = maxMessages;
    }
    
    @Override
    public List<AgentMessage> transform(List<AgentMessage> messages) {
        if (messages.size() <= maxMessages) return messages;
        
        // 保留系统消息 + 最近的N-1条
        List<AgentMessage> result = new ArrayList<>();
        for (AgentMessage msg : messages) {
            if (msg.getRole() == MessageRole.SYSTEM) {
                result.add(msg);
            }
        }
        
        int remaining = maxMessages - result.size();
        List<AgentMessage> tail = messages.subList(
            Math.max(0, messages.size() - remaining), messages.size());
        result.addAll(tail);
        return result;
    }
}

/**
 * 组合变换器 — 按顺序执行多个变换器。
 */
public class CompositeTransformer implements MessageTransformer {
    private final List<MessageTransformer> transformers;
    
    @Override
    public List<AgentMessage> transform(List<AgentMessage> messages) {
        List<AgentMessage> result = messages;
        for (MessageTransformer t : transformers) {
            result = t.transform(result);
        }
        return result;
    }
}
```

### 3.9 MessageConverter — 消息格式转换

```java
package com.easyagent4j.core.context;

/**
 * 消息格式转换器 — 将AgentMessage转换为LLM能理解的格式。
 * 
 * 在core层定义接口；spring层提供Spring AI Message的实现。
 * 
 * 核心逻辑：
 * 1. 过滤掉LLM不认识的消息类型（如自定义UI消息）
 * 2. 转换为具体的LLM消息格式
 */
public interface MessageConverter {
    /**
     * 将AgentMessage列表转换为LLM消息列表。
     * @return LLM消息列表（具体类型由实现决定）
     */
    List<?> convert(List<AgentMessage> messages);
}
```

---

## 4. Spring AI 集成层详细设计

### 4.1 SpringAiChatModelAdapter

```java
package com.easyagent4j.spring.chat;

/**
 * 将Spring AI的ChatModel适配为core层的ChatModel接口。
 */
public class SpringAiChatModelAdapter implements com.easyagent4j.core.chat.ChatModel {
    
    private final org.springframework.ai.chat.model.ChatModel springChatModel;
    private final SpringAiMessageConverter messageConverter;
    
    @Override
    public ChatResponse call(ChatRequest request) {
        // 1. 将core层消息转为Spring AI的Message列表
        List<org.springframework.ai.chat.messages.Message> springMessages = 
            messageConverter.toSpringAiMessages(request.getMessages());
        
        // 2. 添加SystemMessage
        Prompt prompt = new Prompt(springMessages, buildOptions(request));
        
        // 3. 调用Spring AI
        org.springframework.ai.chat.model.ChatResponse response = 
            springChatModel.call(prompt);
        
        // 4. 转换响应
        return messageConverter.toCoreResponse(response);
    }
    
    @Override
    public void stream(ChatRequest request, Consumer<ChatResponseChunk> callback) {
        List<Message> springMessages = 
            messageConverter.toSpringAiMessages(request.getMessages());
        Prompt prompt = new Prompt(springMessages, buildOptions(request));
        
        Flux<ChatResponse> flux = springChatModel.stream(prompt);
        flux.subscribe(chunk -> {
            callback.accept(messageConverter.toCoreChunk(chunk));
        });
    }
}
```

### 4.2 SpringAiToolAdapter

```java
package com.easyagent4j.spring.tool;

/**
 * 将AgentTool适配为Spring AI的FunctionCallback。
 */
public class SpringAiToolAdapter {
    
    /**
     * 将AgentTool列表转换为Spring AI的FunctionCallback列表。
     */
    public static List<FunctionCallback> adapt(List<AgentTool> tools) {
        return tools.stream()
            .map(tool -> FunctionCallback.builder()
                .function(tool.getName(), (input, context) -> {
                    ToolContext tc = new ToolContext(
                        // 从FunctionCallingContext构建ToolContext
                    );
                    return tool.execute(tc).getContent();
                })
                .description(tool.getDescription())
                .inputType(buildInputType(tool.getParameterSchema()))
                .build())
            .collect(Collectors.toList());
    }
}
```

### 4.3 自动配置

```java
package com.easyagent4j.spring.autoconfigure;

@EnableConfigurationProperties(EasyAgentProperties.class)
public class EasyAgentAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public AgentConfig easyAgentConfig(EasyAgentProperties props) {
        return AgentConfig.builder()
            .systemPrompt(props.getSystemPrompt())
            .maxToolIterations(props.getMaxToolIterations())
            .toolExecutionMode(props.getToolExecutionMode())
            .streamingEnabled(props.isStreaming())
            .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public com.easyagent4j.core.chat.ChatModel easyAgentChatModel(
            org.springframework.ai.chat.model.ChatModel springChatModel) {
        return new SpringAiChatModelAdapter(springChatModel);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MessageTransformer messageTransformer(EasyAgentProperties props) {
        if (props.getContext().getMaxMessages() > 0) {
            return new SlidingWindowTransformer(props.getContext().getMaxMessages());
        }
        return messages -> messages; // identity
    }
    
    @Bean
    @ConditionalOnMissingBean
    public Agent agent(AgentConfig config, ChatModel chatModel) {
        return new Agent(config, chatModel);
    }
}
```

### 4.4 配置属性

```java
package com.easyagent4j.spring.properties;

@ConfigurationProperties(prefix = "easyagent")
public class EasyAgentProperties {
    
    private String systemPrompt = "You are a helpful assistant.";
    private int maxToolIterations = 10;
    private ToolExecutionMode toolExecutionMode = ToolExecutionMode.PARALLEL;
    private boolean streaming = true;
    private ContextProperties context = new ContextProperties();
    
    @Data
    public static class ContextProperties {
        private int maxMessages = 50;
    }
}
```

```yaml
# application.yml
easyagent:
  system-prompt: "You are a helpful Java coding assistant."
  max-tool-iterations: 10
  tool-execution-mode: parallel    # parallel | sequential
  streaming: true
  context:
    max-messages: 50
```

---

## 5. 与 pi-mono 的完整对照

| pi-mono (TypeScript) | EasyAgent4j (Java) | 实现差异说明 |
|----------------------|---------------------|-------------|
| `new Agent(options)` | `new Agent(config, chatModel)` | Java用配置类+构造注入 |
| `AgentMessage` (union type) | `AgentMessage` (接口+实现类) | Java用接口继承体系替代TS联合类型 |
| `AgentTool` (object literal) | `AgentTool` (接口/抽象类) | Java用接口+AbstractAgentTool |
| `Type.Object()` (JSON Schema) | `String getParameterSchema()` | 手动JSON Schema字符串，Phase 2可考虑注解 |
| `agentLoop()` (async generator) | `AgentLoop.execute()` | Java用同步循环+CompletableFuture包装 |
| `subscribe(event => ...)` | `subscribe(listener)` | Java用观察者接口 |
| `transformContext` | `MessageTransformer` | 完全对应 |
| `convertToLlm` | `MessageConverter` | 完全对应 |
| `beforeToolCall / afterToolCall` | `ToolHook` | 对应，合并为一个接口 |
| `steering` | `SteeringCallback` | Phase 2实现 |
| `followUp` | 后续队列 | Phase 2实现 |
| `Model` (统一LLM API) | `ChatModel` (Spring AI) | 复用Spring AI，不重复造轮子 |
| `parallel tool execution` | `CompletableFuture.allOf()` | Java标准并发 |
| `abort()` | `AbortController` + volatile flag | 对应 |
| `waitForIdle()` | `CompletableFuture.get()` | 对应 |
| `CustomAgentMessages` (declaration merging) | `metadata` Map + 自定义子接口 | Java用元数据Map+扩展 |

---

## 6. Maven依赖设计

### 6.1 父POM依赖管理

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.6</spring-boot.version>
    <spring-ai.version>1.1.2</spring-ai.version>
    <jackson.version>2.17.2</jackson.version>
    <junit.version>5.10.3</junit.version>
    <mockito.version>5.12.0</mockito.version>
    <slf4j.version>2.0.13</slf4j.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- 内部模块 -->
        <dependency>
            <groupId>com.easyagent4j</groupId>
            <artifactId>easyagent4j-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.easyagent4j</groupId>
            <artifactId>easyagent4j-spring</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- Spring AI BOM -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 6.2 各模块依赖

```
easyagent4j-core
├── com.fasterxml.jackson.core:jackson-databind       (JSON解析)
├── org.slf4j:slf4j-api                               (日志接口)
└── test: org.junit.jupiter:junit-jupiter
    test: org.mockito:mockito-core

easyagent4j-spring
├── com.easyagent4j:easyagent4j-core
├── org.springframework.ai:spring-ai-core
├── org.springframework.boot:spring-boot-starter
└── test: org.springframework.boot:spring-boot-starter-test

easyagent4j-spring-boot-starter
└── com.easyagent4j:easyagent4j-spring   (transitive)

easyagent4j-examples (各示例)
├── com.easyagent4j:easyagent4j-spring-boot-starter
├── org.springframework.boot:spring-boot-starter-web
└── org.springframework.ai:spring-ai-openai-spring-boot-starter (示例用OpenAI)
```

---

## 7. 执行流程时序图

```
用户代码                Agent                    AgentLoop              ChatModel(Sp)           工具
  │                      │                         │                       │                     │
  │  prompt("你好")       │                         │                       │                     │
  │─────────────────────>│                         │                       │                     │
  │                      │  publish(AgentStart)    │                       │                     │
  │                      │  execute(msg, ctx) ───>│                       │                     │
  │                      │                         │  transform(ctx)       │                     │
  │                      │                         │  convert(msgs)         │                     │
  │                      │                         │  call/Stream()  ─────>│                     │
  │                      │                         │  <─── text chunks ────│                     │
  │                      │  <── publish(MsgUpdate) │                       │                     │
  │  <── callback(delta) │                         │                       │                     │
  │                      │                         │  <── response(with toolCalls)                │
  │                      │                         │                       │                     │
  │                      │                         │  beforeToolCall()     │                     │
  │                      │                         │  execute() ────────────────────────────────>│
  │                      │                         │  <── ToolResult ────────────────────────────│
  │                      │                         │  afterToolCall()      │                     │
  │                      │                         │                       │                     │
  │                      │                         │  [循环: 再次调用LLM]  │                     │
  │                      │                         │  call/Stream()  ─────>│                     │
  │                      │  <── publish(MsgUpdate) │  <── text ───────────│                     │
  │                      │                         │  (无toolCalls，结束)   │                     │
  │                      │                         │                       │                     │
  │  <── Future完成       │                         │                       │                     │
  │                      │  publish(AgentEnd)      │                       │                     │
```

---

## 8. 示例用法预览

### 8.1 基础对话

```java
// 纯Java方式（不依赖Spring）
Agent agent = new Agent(
    AgentConfig.builder()
        .systemPrompt("你是Java编程专家")
        .maxToolIterations(10)
        .build(),
    new SpringAiChatModelAdapter(springChatModel)
);

agent.subscribe(event -> {
    if (event instanceof MessageUpdateEvent e) {
        System.out.print(e.getDelta());
    }
});

agent.prompt("用Java实现快速排序").get();
```

### 8.2 带工具调用

```java
Agent agent = new Agent(config, chatModel);
agent.addTool(new WeatherTool());
agent.addTool(new CalculatorTool());

agent.subscribe(event -> {
    if (event instanceof ToolExecutionStartEvent e) {
        System.out.println("调用工具: " + e.getToolCall().getName());
    }
});

agent.prompt("北京今天天气怎么样？28+72等于多少？").get();
```

### 8.3 Spring Boot方式

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private Agent agent;
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message) {
        return Flux.create(sink -> {
            agent.subscribe(event -> {
                if (event instanceof MessageUpdateEvent e) {
                    sink.next(e.getDelta());
                }
                if (event instanceof AgentEndEvent) {
                    sink.complete();
                }
            });
            agent.prompt(message);
        });
    }
}
```

---

*文档结束 — 下一步：开发计划*
