package com.easyagent4j.core.agent;

import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.context.MessageConverter;
import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.event.AgentEventPublisher;
import com.easyagent4j.core.event.events.*;
import com.easyagent4j.core.exception.*;
import com.easyagent4j.core.message.*;
import com.easyagent4j.core.tool.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Agent核心执行循环。
 *
 * 循环逻辑：
 * 1. 将用户消息加入上下文
 * 2. transformContext → convertToLlm → 调用ChatModel
 * 3. 如果响应包含工具调用 → 执行工具 → 将结果加入上下文 → 回到步骤2
 * 4. 如果响应是纯文本 → 循环结束
 *
 * 支持：
 * - 串行/并行工具执行（由ToolExecutionMode控制）
 * - 转向机制（通过AgentSteering动态改变执行方向）
 */
public class AgentLoop {
    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    private final ChatModel chatModel;
    private final AgentConfig config;
    private final List<AgentTool> tools;
    private final MessageTransformer transformer;
    private final MessageConverter converter;
    private final ToolHook toolHook;
    private final AgentEventPublisher eventPublisher;
    private final AgentSteering steering;
    private volatile boolean aborted = false;

    private final Map<String, AgentTool> toolMap = new ConcurrentHashMap<>();
    private final ExecutorService parallelExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "tool-parallel");
        t.setDaemon(true);
        return t;
    });

    public AgentLoop(ChatModel chatModel, AgentConfig config, List<AgentTool> tools,
                     MessageTransformer transformer, MessageConverter converter,
                     ToolHook toolHook, AgentEventPublisher eventPublisher) {
        this(chatModel, config, tools, transformer, converter, toolHook, eventPublisher, null);
    }

    public AgentLoop(ChatModel chatModel, AgentConfig config, List<AgentTool> tools,
                     MessageTransformer transformer, MessageConverter converter,
                     ToolHook toolHook, AgentEventPublisher eventPublisher,
                     AgentSteering steering) {
        this.chatModel = chatModel;
        this.config = config;
        this.tools = new CopyOnWriteArrayList<>(tools != null ? tools : List.of());
        this.transformer = transformer;
        this.converter = converter;
        this.toolHook = toolHook;
        this.eventPublisher = eventPublisher;
        this.steering = steering;

        for (AgentTool tool : this.tools) {
            toolMap.put(tool.getName(), tool);
        }
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    /**
     * 执行完整的Agent循环。
     */
    public AgentContext execute(AgentMessage userMessage, AgentContext context) {
        // 1. 添加用户消息
        if (userMessage != null) {
            context.addMessage(userMessage);
            eventPublisher.publish(new MessageStartEvent(context, userMessage));
            eventPublisher.publish(new MessageEndEvent(context, userMessage));
        }

        // 2. 主循环
        int iteration = 0;
        while (iteration < config.getMaxToolIterations() && !aborted) {
            // 2.0 检查转向状态
            if (steering != null) {
                SteeringCommand cmd = steering.pollCommand();
                if (cmd == SteeringCommand.STOP) {
                    log.info("Agent loop stopped by steering command");
                    break;
                } else if (cmd == SteeringCommand.STEER) {
                    String overridePrompt = steering.consumeOverrideSystemPrompt();
                    if (overridePrompt != null) {
                        log.info("Agent loop steered with new system prompt");
                        // 通过设置config属性来使用覆盖的system prompt
                        config.applySystemPromptOverride(overridePrompt);
                    }
                }
            }

            eventPublisher.publish(new TurnStartEvent(context));

            // 2.1 变换上下文
            List<AgentMessage> transformed = transformer.transform(context.getMessages());

            // 2.2 转换为LLM格式
            List<?> llmMessages = converter.convert(transformed);

            // 2.3 构建工具定义
            List<ToolDefinition> toolDefs = tools.stream()
                .map(t -> new ToolDefinition(t.getName(), t.getDescription(), t.getParameterSchema()))
                .toList();

            // 2.4 构建请求
            ChatRequest request = ChatRequest.builder()
                .messages(llmMessages)
                .systemPrompt(config.getSystemPrompt())
                .tools(toolDefs)
                .streaming(config.isStreamingEnabled())
                .build();

            // 2.5 调用LLM
            AssistantMessage assistantMessage;
            try {
                if (config.isStreamingEnabled()) {
                    assistantMessage = callWithStreaming(request, context);
                } else {
                    assistantMessage = callWithoutStreaming(request, context);
                }
            } catch (Exception e) {
                log.error("LLM call failed", e);
                eventPublisher.publish(new ErrorEvent(context, "LLM call failed: " + e.getMessage()));
                break;
            }

            context.addMessage(assistantMessage);

            // 2.6 检查工具调用
            List<ToolCall> toolCalls = assistantMessage.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                eventPublisher.publish(new TurnEndEvent(context, List.of()));
                break;
            }

            // 2.7 执行工具（根据模式选择串行或并行）
            List<ToolResultMessage> toolResults;
            if (config.getToolExecutionMode() == ToolExecutionMode.PARALLEL) {
                toolResults = executeToolsParallel(toolCalls, context);
            } else {
                toolResults = executeToolsSequential(toolCalls, context);
            }

            // 2.8 将结果加入上下文
            for (ToolResultMessage result : toolResults) {
                context.addMessage(result);
            }

            eventPublisher.publish(new TurnEndEvent(context, toolResults));
            iteration++;
        }

        if (aborted) {
            eventPublisher.publish(new ErrorEvent(context, "Agent execution was aborted"));
        } else if (iteration >= config.getMaxToolIterations()) {
            eventPublisher.publish(new ErrorEvent(context,
                "Max tool iterations (" + config.getMaxToolIterations() + ") exceeded"));
        }

        return context;
    }

    private AssistantMessage callWithStreaming(ChatRequest request, AgentContext context) {
        StringBuilder content = new StringBuilder();
        AssistantMessage message = new AssistantMessage();

        eventPublisher.publish(new MessageStartEvent(context, message));

        chatModel.stream(request, chunk -> {
            if (chunk.getText() != null) {
                content.append(chunk.getText());
                message.setTextContent(content.toString());
                eventPublisher.publish(new MessageUpdateEvent(context, chunk.getText()));
            }
            if (chunk.getToolCalls() != null && !chunk.getToolCalls().isEmpty()) {
                message.setToolCalls(chunk.getToolCalls());
            }
        });

        eventPublisher.publish(new MessageEndEvent(context, message));
        return message;
    }

    private AssistantMessage callWithoutStreaming(ChatRequest request, AgentContext context) {
        ChatResponse response = chatModel.call(request);
        AssistantMessage message = new AssistantMessage(response.getText(), response.getToolCalls());

        eventPublisher.publish(new MessageStartEvent(context, message));
        eventPublisher.publish(new MessageEndEvent(context, message));
        return message;
    }

    /**
     * 串行执行工具调用（原有逻辑）。
     */
    private List<ToolResultMessage> executeToolsSequential(List<ToolCall> toolCalls, AgentContext context) {
        List<ToolResultMessage> results = new ArrayList<>();

        for (ToolCall call : toolCalls) {
            results.add(executeSingleTool(call, context));
        }

        return results;
    }

    /**
     * 并行执行工具调用 — 每个ToolCall在独立线程中执行，异常不中断其他工具。
     */
    private List<ToolResultMessage> executeToolsParallel(List<ToolCall> toolCalls, AgentContext context) {
        if (toolCalls.size() == 1) {
            // 单个工具调用无需并行开销
            return executeToolsSequential(toolCalls, context);
        }

        log.debug("Executing {} tool calls in parallel", toolCalls.size());

        // 为每个ToolCall创建CompletableFuture
        List<CompletableFuture<ToolResultMessage>> futures = new ArrayList<>();
        for (ToolCall call : toolCalls) {
            CompletableFuture<ToolResultMessage> future = CompletableFuture.supplyAsync(
                () -> executeSingleTool(call, context),
                parallelExecutor
            );
            futures.add(future);
        }

        // 等待所有完成，收集结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 按原始顺序收集结果
        List<ToolResultMessage> results = new ArrayList<>();
        for (CompletableFuture<ToolResultMessage> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                // 不应该发生，因为executeSingleTool内部已捕获异常
                log.error("Unexpected error collecting parallel tool result", e);
                results.add(ToolResultMessage.error(
                    new ToolCall("unknown", "unknown", "{}"),
                    "Unexpected error: " + e.getMessage()
                ));
            }
        }

        return results;
    }

    /**
     * 执行单个工具调用（含hook支持和事件发布）。
     * 并行安全：每个工具调用独立执行。
     */
    private ToolResultMessage executeSingleTool(ToolCall call, AgentContext context) {
        eventPublisher.publish(new ToolExecutionStartEvent(context, call));

        // beforeToolCall hook
        if (toolHook != null) {
            ToolHookResult hookResult = toolHook.beforeToolCall(call, context);
            if (hookResult.isBlocked()) {
                ToolResultMessage msg = ToolResultMessage.blocked(call, hookResult.getReason());
                eventPublisher.publish(new ToolExecutionEndEvent(context, call, true));
                return msg;
            }
        }

        // 查找工具
        AgentTool tool = toolMap.get(call.getName());
        if (tool == null) {
            ToolResultMessage msg = ToolResultMessage.error(call, "Tool not found: " + call.getName());
            eventPublisher.publish(new ToolExecutionEndEvent(context, call, true));
            return msg;
        }

        // 执行工具
        try {
            ToolContext tc = new ToolContext(call, context);
            ToolResult result = tool.execute(tc);

            if (toolHook != null) {
                toolHook.afterToolCall(call, result, false, context);
            }

            ToolResultMessage msg = ToolResultMessage.success(call, result.getContent());
            eventPublisher.publish(new ToolExecutionEndEvent(context, call, false));
            return msg;
        } catch (Exception e) {
            log.error("Tool execution error: {}", call.getName(), e);
            ToolResultMessage msg = ToolResultMessage.error(call, e.getMessage());

            if (toolHook != null) {
                toolHook.afterToolCall(call, ToolResult.error(e.getMessage()), true, context);
            }
            eventPublisher.publish(new ToolExecutionEndEvent(context, call, true));
            return msg;
        }
    }
}
