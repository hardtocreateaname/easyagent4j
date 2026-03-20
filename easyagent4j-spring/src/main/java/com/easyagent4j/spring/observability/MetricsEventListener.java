package com.easyagent4j.spring.observability;

import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.event.AgentEventListener;
import com.easyagent4j.core.event.events.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于事件系统的指标自动采集器。
 * <p>
 * 通过实现{@link AgentEventListener}，监听Agent运行生命周期事件，
 * 自动将关键指标记录到{@link AgentMetrics}中。
 * </p>
 *
 * <h3>采集逻辑</h3>
 * <ul>
 *   <li>{@code agent_start} → 记录开始时间戳和消息计数</li>
 *   <li>{@code agent_end} → 计算运行时长和轮次，调用{@link AgentMetrics#recordAgentRun}</li>
 *   <li>{@code tool_execution_start} → 记录工具开始时间</li>
 *   <li>{@code tool_execution_end} → 计算工具耗时，调用{@link AgentMetrics#recordToolExecution}</li>
 *   <li>{@code message_end} → 累计消息数量</li>
 *   <li>{@code turn_start} → 累加轮次计数</li>
 * </ul>
 *
 * @author Chen Jiaming
 * @since 0.1.0
 */
public class MetricsEventListener implements AgentEventListener {

    private final AgentMetrics metrics;

    /** sessionId → agent开始时间戳 */
    private final Map<String, Long> agentStartTimes = new ConcurrentHashMap<>();

    /** sessionId → 当前进行的轮次计数 */
    private final Map<String, AtomicInteger> turnCounters = new ConcurrentHashMap<>();

    /** sessionId → 上一轮的消息数（用于计算增量） */
    private final Map<String, Integer> lastMessageCounts = new ConcurrentHashMap<>();

    /** toolCallId → 工具开始时间戳 */
    private final Map<String, Long> toolStartTimes = new ConcurrentHashMap<>();

    /** toolCallId → 工具名称 */
    private final Map<String, String> toolNames = new ConcurrentHashMap<>();

    public MetricsEventListener(AgentMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void onEvent(AgentEvent event) {
        switch (event.getType()) {
            case "agent_start" -> handleAgentStart((AgentStartEvent) event);
            case "agent_end" -> handleAgentEnd((AgentEndEvent) event);
            case "tool_execution_start" -> handleToolStart((ToolExecutionStartEvent) event);
            case "tool_execution_end" -> handleToolEnd((ToolExecutionEndEvent) event);
            case "message_end" -> handleMessageEnd((MessageEndEvent) event);
            case "turn_start" -> handleTurnStart((TurnStartEvent) event);
            default -> { /* ignore other events */ }
        }
    }

    private void handleAgentStart(AgentStartEvent event) {
        String sessionId = getSessionId(event);
        agentStartTimes.put(sessionId, System.currentTimeMillis());
        turnCounters.put(sessionId, new AtomicInteger(0));
        lastMessageCounts.put(sessionId, event.getContext().getMessageCount());
    }

    private void handleAgentEnd(AgentEndEvent event) {
        String sessionId = getSessionId(event);
        Long startTime = agentStartTimes.remove(sessionId);
        AtomicInteger turns = turnCounters.remove(sessionId);
        lastMessageCounts.remove(sessionId);

        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        int turnCount = turns != null ? turns.get() : 0;
        metrics.recordAgentRun(sessionId, duration, turnCount, "completed");
    }

    private void handleToolStart(ToolExecutionStartEvent event) {
        String toolCallId = event.getToolCall().getId();
        toolStartTimes.put(toolCallId, System.currentTimeMillis());
        toolNames.put(toolCallId, event.getToolCall().getName());
    }

    private void handleToolEnd(ToolExecutionEndEvent event) {
        String toolCallId = event.getToolCall().getId();
        Long startTime = toolStartTimes.remove(toolCallId);
        String toolName = toolNames.remove(toolCallId);

        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        boolean success = !event.isError();
        metrics.recordToolExecution(toolName, duration, success);
    }

    private void handleMessageEnd(MessageEndEvent event) {
        String sessionId = getSessionId(event);
        metrics.recordMessageCount(1);
    }

    private void handleTurnStart(TurnStartEvent event) {
        String sessionId = getSessionId(event);
        AtomicInteger counter = turnCounters.get(sessionId);
        if (counter != null) {
            counter.incrementAndGet();
        }
    }

    private String getSessionId(AgentEvent event) {
        String sessionId = event.getSessionId();
        return sessionId != null ? sessionId : "default";
    }
}
