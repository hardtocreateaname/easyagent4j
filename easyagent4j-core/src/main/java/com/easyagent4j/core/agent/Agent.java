package com.easyagent4j.core.agent;

import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.context.MessageConverter;
import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.context.converter.DefaultMessageConverter;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.event.AgentEventListener;
import com.easyagent4j.core.event.AgentEventPublisher;
import com.easyagent4j.core.event.events.AgentEndEvent;
import com.easyagent4j.core.event.events.AgentStartEvent;
import com.easyagent4j.core.exception.AgentAbortException;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.ContentPart;
import com.easyagent4j.core.message.UserMessage;
import com.easyagent4j.core.tool.AgentTool;
import com.easyagent4j.core.tool.ToolHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent主类 — 有状态的AI Agent运行时。
 *
 * 线程安全：非线程安全，设计为单线程使用。steer/abort方法可从外部线程调用。
 */
public class Agent {
    private static final Logger log = LoggerFactory.getLogger(Agent.class);

    private final AgentConfig config;
    private ChatModel chatModel;
    private final List<AgentTool> tools;
    private MessageTransformer transformer;
    private MessageConverter converter;
    private ToolHook toolHook;
    private final AgentContext context;
    private final AgentEventPublisher eventPublisher;
    private final AgentSteering steering;
    private AgentLoop loop;
    private volatile AgentState state = AgentState.IDLE;

    public Agent(AgentConfig config, ChatModel chatModel) {
        this.config = config;
        this.chatModel = chatModel;
        this.tools = new CopyOnWriteArrayList<>();
        this.transformer = msg -> msg; // identity
        this.converter = new DefaultMessageConverter();
        this.context = new AgentContext(config.getSessionId());
        this.eventPublisher = new AgentEventPublisher();
        this.steering = new AgentSteering();
        this.loop = createLoop();
    }

    private AgentLoop createLoop() {
        return new AgentLoop(chatModel, config, tools, transformer, converter, toolHook, eventPublisher, steering);
    }

    // === 核心方法 ===

    public CompletableFuture<AgentContext> prompt(String userMessage) {
        return prompt(new UserMessage(userMessage));
    }

    public CompletableFuture<AgentContext> prompt(String text, List<ContentPart> attachments) {
        return prompt(new UserMessage(text, attachments));
    }

    public CompletableFuture<AgentContext> prompt(AgentMessage message) {
        if (state == AgentState.RUNNING) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Agent is already running"));
        }

        return CompletableFuture.supplyAsync(() -> {
            state = AgentState.RUNNING;
            eventPublisher.publish(new AgentStartEvent(context));
            try {
                loop = createLoop();
                return loop.execute(message, context);
            } finally {
                state = AgentState.IDLE;
                eventPublisher.publish(new AgentEndEvent(context));
            }
        });
    }

    public CompletableFuture<AgentContext> continueExecution() {
        if (state == AgentState.RUNNING) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Agent is already running"));
        }

        return CompletableFuture.supplyAsync(() -> {
            state = AgentState.RUNNING;
            eventPublisher.publish(new AgentStartEvent(context));
            try {
                loop = createLoop();
                return loop.execute(null, context);
            } finally {
                state = AgentState.IDLE;
                eventPublisher.publish(new AgentEndEvent(context));
            }
        });
    }

    /**
     * 中止Agent执行。
     */
    public void abort() {
        state = AgentState.ABORTED;
        steering.stop();
        if (loop != null) {
            loop.setAborted(true);
        }
    }

    /**
     * 转向：用新消息改变Agent的执行方向。
     * 设置新的system prompt，AgentLoop在下一轮循环开始时检测并应用。
     *
     * @param message 新的system prompt内容
     */
    public void steer(String message) {
        steering.steer(message);
    }

    /**
     * 获取转向控制器。
     */
    public AgentSteering getSteering() {
        return steering;
    }

    // === 状态管理 ===

    public void setSystemPrompt(String prompt) {
        // 通过新config重新创建loop
    }

    public void setChatModel(ChatModel model) {
        this.chatModel = model;
    }

    public void setTools(List<AgentTool> tools) {
        this.tools.clear();
        this.tools.addAll(tools);
    }

    public void addTool(AgentTool tool) {
        this.tools.add(tool);
    }

    public void removeTool(String toolName) {
        this.tools.removeIf(t -> t.getName().equals(toolName));
    }

    public void setTransformer(MessageTransformer transformer) {
        this.transformer = transformer;
    }

    public void setConverter(MessageConverter converter) {
        this.converter = converter;
    }

    public void setToolHook(ToolHook hook) {
        this.toolHook = hook;
    }

    public void clearMessages() {
        context.clearMessages();
    }

    public void reset() {
        context.clearMessages();
        context.getAttributes().clear();
        steering.clear();
    }

    // === 查询 ===

    public AgentContext getContext() { return context; }
    public AgentState getState() { return state; }
    public AgentConfig getConfig() { return config; }
    public List<AgentMessage> getMessages() { return context.getMessages(); }

    // === 事件 ===

    public void subscribe(AgentEventListener listener) { eventPublisher.subscribe(listener); }
    public void unsubscribe(AgentEventListener listener) { eventPublisher.unsubscribe(listener); }
}
