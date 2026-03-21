package com.easyagent4j.example.web.controller;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.agent.AgentSessionNode;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.event.AgentEventListener;
import com.easyagent4j.core.event.events.MessageUpdateEvent;
import com.easyagent4j.core.event.events.ToolExecutionStartEvent;
import com.easyagent4j.core.event.events.ToolExecutionEndEvent;
import com.easyagent4j.core.event.events.ErrorEvent;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.example.web.model.ChatRequest;
import com.easyagent4j.example.web.model.ChatResponse;
import com.easyagent4j.spring.agent.AgentSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 聊天API控制器 — 提供REST和SSE流式对话接口。
 */
@RestController
@CrossOrigin(originPatterns = "*")
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long SSE_TIMEOUT_MS = 60_000;

    @Autowired
    private Agent agent;

    @Autowired
    private AgentSessionManager agentSessionManager;

    /**
     * POST /api/chat — 同步对话（返回完整回复JSON）。
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ChatResponse.error("消息不能为空");
        }

        long startTime = System.currentTimeMillis();
        Agent currentAgent = null;
        AgentEventListener toolListener = null;

        try {
            currentAgent = resolveAgent(request.getSessionId());

            // 可选：设置system prompt
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
                currentAgent.setSystemPrompt(request.getSystemPrompt());
            }

            // 记录工具调用
            List<ChatResponse.ToolCallInfo> toolCalls = new ArrayList<>();
            toolListener = event -> {
                if (event instanceof ToolExecutionStartEvent e) {
                    toolCalls.add(new ChatResponse.ToolCallInfo(
                            e.getToolCall().getName(),
                            e.getToolCall().getArguments(),
                            null));
                } else if (event instanceof ToolExecutionEndEvent e) {
                    // 更新最后一个工具调用的结果
                    for (int i = toolCalls.size() - 1; i >= 0; i--) {
                        ChatResponse.ToolCallInfo tci = toolCalls.get(i);
                        if (tci.getResult() == null && tci.getName().equals(e.getToolCall().getName())) {
                            // 无法直接获取result，留空即可
                            break;
                        }
                    }
                }
            };
            currentAgent.subscribe(toolListener);

            // 执行Agent
            CompletableFuture<AgentContext> future = currentAgent.prompt(request.getMessage());
            AgentContext ctx = future.get(60, TimeUnit.SECONDS);

            long elapsed = System.currentTimeMillis() - startTime;

            // 提取最后一条助手消息
            String reply = "";
            if (!ctx.getMessages().isEmpty()) {
                reply = ctx.getLastMessage().getContent().toString();
            }

            ChatResponse response = ChatResponse.success(reply, ctx.getSessionId(), elapsed);
            enrichSessionMetadata(currentAgent, response);
            if (!toolCalls.isEmpty()) {
                response.setToolCalls(toolCalls);
            }
            return response;
        } catch (Exception e) {
            log.error("Chat error", e);
            return ChatResponse.error(e.getMessage());
        } finally {
            if (currentAgent != null && toolListener != null) {
                currentAgent.unsubscribe(toolListener);
            }
        }
    }

    /**
     * GET /api/chat/stream — SSE流式对话。
     * <p>
     * 参数：query=用户消息
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam("query") String query,
                                  @RequestParam(value = "session_id", required = false) String sessionId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        if (query == null || query.isBlank()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"query 不能为空\"}"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        Agent currentAgent = resolveAgent(sessionId);

        try {
            // 发送连接确认
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"session_id\":\""
                            + currentAgent.getContext().getSessionId() + "\"}"));

            // 注册事件监听器，实时推送SSE事件
            AtomicBoolean closed = new AtomicBoolean(false);
            AgentEventListener listener = new AgentEventListener() {
                @Override
                public void onEvent(AgentEvent event) {
                    if (closed.get()) {
                        return;
                    }
                    try {
                        if (event instanceof MessageUpdateEvent e) {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data("{\"delta\":\"" + escapeJson(e.getDelta()) + "\"}"));
                        } else if (event instanceof ToolExecutionStartEvent e) {
                            emitter.send(SseEmitter.event()
                                    .name("tool_start")
                                    .data("{\"name\":\"" + escapeJson(e.getToolCall().getName())
                                            + "\",\"arguments\":\"" + escapeJson(e.getToolCall().getArguments()) + "\"}"));
                        } else if (event instanceof ToolExecutionEndEvent e) {
                            emitter.send(SseEmitter.event()
                                    .name("tool_end")
                                    .data("{\"name\":\"" + escapeJson(e.getToolCall().getName()) + "\"}"));
                        } else if (event instanceof ErrorEvent e) {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("{\"error\":\"" + escapeJson(e.getErrorMessage()) + "\"}"));
                        }
                    } catch (IOException ex) {
                        closed.set(true);
                        currentAgent.unsubscribe(this);
                        emitter.completeWithError(ex);
                    }
                }
            };
            currentAgent.subscribe(listener);
            emitter.onCompletion(() -> {
                closed.set(true);
                currentAgent.unsubscribe(listener);
            });
            emitter.onTimeout(() -> {
                closed.set(true);
                currentAgent.unsubscribe(listener);
                emitter.complete();
            });
            emitter.onError(ex -> {
                closed.set(true);
                currentAgent.unsubscribe(listener);
            });

            // 异步执行Agent
            currentAgent.prompt(query).whenComplete((ctx, ex) -> {
                try {
                    if (closed.get()) {
                        return;
                    }
                    if (ex != null) {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("{\"status\":\"error\",\"error\":\"" + escapeJson(ex.getMessage()) + "\"}"));
                    } else {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("{\"status\":\"done\",\"session_id\":\"" + ctx.getSessionId() + "\"}"));
                    }
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            });

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * POST /api/chat/tools — 工具调用接口。
     */
    @PostMapping("/chat/tools")
    public ChatResponse chatWithTools(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ChatResponse.error("消息不能为空");
        }

        long startTime = System.currentTimeMillis();
        Agent currentAgent = null;
        AgentEventListener toolListener = null;

        try {
            currentAgent = resolveAgent(request.getSessionId());
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
                currentAgent.setSystemPrompt(request.getSystemPrompt());
            }

            // 记录工具调用详情
            List<ChatResponse.ToolCallInfo> toolCalls = new ArrayList<>();
            toolListener = event -> {
                if (event instanceof ToolExecutionStartEvent e) {
                    toolCalls.add(new ChatResponse.ToolCallInfo(
                            e.getToolCall().getName(),
                            e.getToolCall().getArguments(),
                            null));
                }
            };
            currentAgent.subscribe(toolListener);

            CompletableFuture<AgentContext> future = currentAgent.prompt(request.getMessage());
            AgentContext ctx = future.get(60, TimeUnit.SECONDS);

            long elapsed = System.currentTimeMillis() - startTime;
            String reply = !ctx.getMessages().isEmpty()
                    ? ctx.getLastMessage().getContent().toString() : "";

            ChatResponse response = ChatResponse.success(reply, ctx.getSessionId(), elapsed);
            enrichSessionMetadata(currentAgent, response);
            response.setToolCalls(toolCalls);
            return response;
        } catch (Exception e) {
            log.error("Tool chat error", e);
            return ChatResponse.error(e.getMessage());
        } finally {
            if (currentAgent != null && toolListener != null) {
                currentAgent.unsubscribe(toolListener);
            }
        }
    }

    /**
     * GET /api/health — 健康检查。
     */
    @GetMapping("/health")
    public ChatResponse health() {
        return ChatResponse.healthOk();
    }

    @PostMapping("/sessions/sub-agent")
    public ChatResponse spawnSubAgent(@RequestBody ChatRequest request) {
        if (request.getParentSessionId() == null || request.getParentSessionId().isBlank()) {
            return ChatResponse.error("parent_session_id 不能为空");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ChatResponse.error("消息不能为空");
        }

        long startTime = System.currentTimeMillis();
        try {
            String childSessionId = request.getSessionId();
            boolean inheritMessages = !Boolean.FALSE.equals(request.getInheritMessages());
            Agent child = agentSessionManager.spawnSubAgent(request.getParentSessionId(), childSessionId, inheritMessages);
            AgentContext ctx = child.prompt(request.getMessage()).get(60, TimeUnit.SECONDS);

            String reply = ctx.getLastMessage() != null && ctx.getLastMessage().getContent() != null
                ? ctx.getLastMessage().getContent().toString()
                : "";
            ChatResponse response = ChatResponse.success(reply, ctx.getSessionId(), System.currentTimeMillis() - startTime);
            enrichSessionMetadata(child, response);
            return response;
        } catch (Exception e) {
            log.error("Spawn sub-agent error", e);
            return ChatResponse.error(e.getMessage());
        }
    }

    @GetMapping("/sessions/{sessionId}/tree")
    public ChatResponse sessionTree(@PathVariable("sessionId") String sessionId) {
        Agent currentAgent = resolveAgent(sessionId);
        ChatResponse response = ChatResponse.success("ok", currentAgent.getContext().getSessionId(), 0);
        enrichSessionMetadata(currentAgent, response);
        return response;
    }

    /**
     * 简单的JSON字符串转义。
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private Agent resolveAgent(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return agent;
        }
        return agentSessionManager.getOrCreateRootAgent(sessionId);
    }

    private void enrichSessionMetadata(Agent currentAgent, ChatResponse response) {
        response.setParentSessionId(currentAgent.getContext().getParentSessionId());
        response.setRootSessionId(currentAgent.getContext().getRootSessionId());
        currentAgent.getSessionTree().getNode(currentAgent.getContext().getSessionId())
            .map(AgentSessionNode::getChildSessionIds)
            .ifPresent(response::setChildSessionIds);
    }
}
