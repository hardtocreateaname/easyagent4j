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
import com.easyagent4j.core.memory.MemoryPromptBuilder;
import com.easyagent4j.core.memory.MemoryStore;
import com.easyagent4j.core.memory.ForkableMemoryStore;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.ContentPart;
import com.easyagent4j.core.message.UserMessage;
import com.easyagent4j.core.personality.AgentPersonality;
import com.easyagent4j.core.resilience.RetryPolicy;
import com.easyagent4j.core.task.AutonomousAgentLoop;
import com.easyagent4j.core.task.DefaultTaskPlanner;
import com.easyagent4j.core.task.TaskPlanner;
import com.easyagent4j.core.tool.AgentTool;
import com.easyagent4j.core.tool.ToolHook;
import com.easyagent4j.core.tool.builtin.SubAgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
    private final AgentSessionTree sessionTree;
    private AgentLoop loop;
    private volatile AgentState state = AgentState.IDLE;
    private MemoryStore memoryStore;
    private AgentPersonality personality;

    public Agent(AgentConfig config, ChatModel chatModel) {
        this(config, chatModel, createRootContext(config), null);
    }

    private Agent(AgentConfig config, ChatModel chatModel, AgentContext context, AgentSessionTree existingSessionTree) {
        this.config = config;
        this.chatModel = chatModel;
        this.tools = new CopyOnWriteArrayList<>();
        this.transformer = msg -> msg; // identity
        this.converter = new DefaultMessageConverter();
        this.context = context;
        this.eventPublisher = new AgentEventPublisher();
        this.steering = new AgentSteering();
        this.sessionTree = existingSessionTree != null ? existingSessionTree : new AgentSessionTree(context.getRootSessionId());
        if (existingSessionTree == null) {
            this.sessionTree.createRoot(context.getSessionId());
        }
        this.loop = createLoop();
    }

    private static AgentContext createRootContext(AgentConfig config) {
        String sessionId = config.getSessionId() != null ? config.getSessionId() : "session-" + UUID.randomUUID();
        return new AgentContext(sessionId, null, sessionId, 0);
    }

    private AgentLoop createLoop() {
        return new AgentLoop(chatModel, config, tools, transformer, converter, toolHook, eventPublisher, steering, config.getRetryPolicy());
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
                // 如果配置了personality，增强system prompt
                enhanceSystemPromptIfConfigured();
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
                // 如果配置了personality，增强system prompt
                enhanceSystemPromptIfConfigured();
                loop = createLoop();
                return loop.execute(null, context);
            } finally {
                state = AgentState.IDLE;
                eventPublisher.publish(new AgentEndEvent(context));
            }
        });
    }

    /**
     * 执行自主任务。
     *
     * @param goal 任务目标
     * @return 执行完成的上下文
     */
    public CompletableFuture<AgentContext> executeAutonomous(String goal) {
        if (state == AgentState.RUNNING) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Agent is already running"));
        }

        return CompletableFuture.supplyAsync(() -> {
            state = AgentState.RUNNING;
            eventPublisher.publish(new AgentStartEvent(context));
            try {
                // 如果配置了personality，增强system prompt
                enhanceSystemPromptIfConfigured();

                // 创建自主循环
                TaskPlanner taskPlanner = new DefaultTaskPlanner(chatModel);
                RetryPolicy retryPolicy = config.getRetryPolicy() != null ? config.getRetryPolicy() : new RetryPolicy();
                AutonomousAgentLoop autonomousLoop = new AutonomousAgentLoop(
                    chatModel, config, tools, transformer, converter, toolHook,
                    eventPublisher, taskPlanner, retryPolicy
                );

                return autonomousLoop.executeAutonomous(goal);
            } finally {
                state = AgentState.IDLE;
                eventPublisher.publish(new AgentEndEvent(context));
            }
        });
    }

    /**
     * 如果配置了personality，增强system prompt。
     */
    private void enhanceSystemPromptIfConfigured() {
        if (personality != null) {
            String personalityPrompt = personality.buildSystemPrompt();
            String currentPrompt = config.getSystemPrompt();
            String enhancedPrompt = personalityPrompt + "\n\n" + currentPrompt;
            config.applySystemPromptOverride(enhancedPrompt);
        }

        if (memoryStore != null && config.getMemory() != null && config.getMemory().isEnabled()) {
            MemoryPromptBuilder promptBuilder = new MemoryPromptBuilder();
            String memoryEnhancedPrompt = promptBuilder.buildEnhancedSystemPrompt(
                config.getSystemPrompt(), memoryStore
            );
            config.applySystemPromptOverride(memoryEnhancedPrompt);
        }
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
     * 转向：用一条高优先级用户消息改变Agent的执行方向。
     * AgentLoop会在当前轮次完成后优先消费该消息。
     *
     * @param message 转向消息
     */
    public void steer(String message) {
        steering.steer(message);
    }

    /**
     * 添加一条普通后续消息。
     * 仅当当前轮没有工具调用且不存在待处理steering消息时才会消费。
     *
     * @param message 后续消息
     */
    public void followUp(String message) {
        steering.followUp(message);
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

    /**
     * 从当前Agent派生一个子Agent。
     * 子Agent默认复制当前消息快照，但之后的上下文互相隔离。
     */
    public Agent spawnSubAgent() {
        return spawnSubAgent(true);
    }

    public Agent spawnSubAgent(boolean inheritMessages) {
        String childSessionId = context.getSessionId() + "/child-" + UUID.randomUUID();
        return spawnSubAgent(childSessionId, inheritMessages);
    }

    public Agent spawnSubAgent(String childSessionId, boolean inheritMessages) {
        if (childSessionId == null || childSessionId.isBlank()) {
            throw new IllegalArgumentException("childSessionId cannot be blank");
        }

        AgentContext childContext = context.fork(childSessionId, inheritMessages);
        sessionTree.createChild(context.getSessionId(), childSessionId, inheritMessages);

        AgentConfig childConfig = config.toBuilder()
            .sessionId(childSessionId)
            .build();

        Agent child = new Agent(childConfig, chatModel, childContext, sessionTree);
        child.setTools(copyToolsForChild(child));
        child.setTransformer(transformer);
        child.setConverter(converter);
        child.setToolHook(toolHook);
        child.setPersonality(personality);
        if (memoryStore instanceof ForkableMemoryStore forkableMemoryStore) {
            child.setMemoryStore(forkableMemoryStore.fork(childSessionId));
        }
        return child;
    }

    private List<AgentTool> copyToolsForChild(Agent child) {
        List<AgentTool> copiedTools = new ArrayList<>();
        for (AgentTool tool : tools) {
            if (tool instanceof SubAgentTool subAgentTool) {
                copiedTools.add(subAgentTool.bindTo(child));
            } else {
                copiedTools.add(tool);
            }
        }
        return copiedTools;
    }

    /**
     * 设置记忆存储。
     */
    public void setMemoryStore(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    /**
     * 设置Agent性格。
     */
    public void setPersonality(AgentPersonality personality) {
        this.personality = personality;
    }

    /**
     * 获取记忆存储。
     */
    public MemoryStore getMemory() {
        return memoryStore;
    }

    /**
     * 获取Agent性格。
     */
    public AgentPersonality getPersonality() {
        return personality;
    }

    // === 查询 ===

    public AgentContext getContext() { return context; }
    public AgentState getState() { return state; }
    public AgentConfig getConfig() { return config; }
    public List<AgentMessage> getMessages() { return context.getMessages(); }
    public AgentSessionTree getSessionTree() { return sessionTree; }

    // === 事件 ===

    public void subscribe(AgentEventListener listener) { eventPublisher.subscribe(listener); }
    public void unsubscribe(AgentEventListener listener) { eventPublisher.unsubscribe(listener); }
    
    /**
     * 清除所有事件监听器。
     * <p>
     * 在需要重新订阅监听器时使用，避免重复订阅导致事件被多次处理。
     * </p>
     */
    public void clearListeners() { eventPublisher.clear(); }
}
