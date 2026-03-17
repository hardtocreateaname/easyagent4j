package com.easyagent4j.spring.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Agent指标的默认实现 — 使用ConcurrentHashMap在内存中记录指标数据。
 * <p>
 * 适用于开发调试和轻量级场景。生产环境建议通过{@code @Bean}替换为
 * Micrometer-backed实现以接入Prometheus/Grafana等监控系统。
 * </p>
 *
 * <h3>指标名称约定</h3>
 * <ul>
 *   <li>{@code easyagent.tokens.input} — 累计输入Token数</li>
 *   <li>{@code easyagent.tokens.output} — 累计输出Token数</li>
 *   <li>{@code easyagent.tool.executions} — 工具总执行次数</li>
 *   <li>{@code easyagent.tool.errors} — 工具执行失败次数</li>
 *   <li>{@code easyagent.tool.duration_ms} — 工具累计执行耗时</li>
 *   <li>{@code easyagent.agent.runs} — Agent运行总次数</li>
 *   <li>{@code easyagent.agent.total_turns} — 累计对话轮次</li>
 *   <li>{@code easyagent.agent.total_duration_ms} — Agent累计运行耗时</li>
 *   <li>{@code easyagent.messages.count} — 累计消息数</li>
 * </ul>
 *
 * @author Chen Jiaming
 * @since 0.1.0
 */
public class DefaultAgentMetrics implements AgentMetrics {

    private final LongAdder inputTokens = new LongAdder();
    private final LongAdder outputTokens = new LongAdder();
    private final LongAdder toolExecutions = new LongAdder();
    private final LongAdder toolErrors = new LongAdder();
    private final LongAdder toolDurationMs = new LongAdder();
    private final LongAdder agentRuns = new LongAdder();
    private final LongAdder totalTurns = new LongAdder();
    private final LongAdder totalDurationMs = new LongAdder();
    private final LongAdder messageCount = new LongAdder();

    /**
     * 按模型分组的Token使用量。Key格式: "tokens.input.{model}" / "tokens.output.{model}"
     */
    private final ConcurrentHashMap<String, LongAdder> tokensByModel = new ConcurrentHashMap<>();

    @Override
    public void recordTokenUsage(String model, int inputTokens, int outputTokens) {
        this.inputTokens.add(inputTokens);
        this.outputTokens.add(outputTokens);
        tokensByModel.computeIfAbsent("tokens.input." + model, k -> new LongAdder()).add(inputTokens);
        tokensByModel.computeIfAbsent("tokens.output." + model, k -> new LongAdder()).add(outputTokens);
    }

    @Override
    public void recordToolExecution(String toolName, long durationMs, boolean success) {
        toolExecutions.increment();
        toolDurationMs.add(durationMs);
        if (!success) {
            toolErrors.increment();
        }
    }

    @Override
    public void recordAgentRun(String agentId, long durationMs, int turns, String status) {
        agentRuns.increment();
        totalTurns.add(turns);
        totalDurationMs.add(durationMs);
    }

    @Override
    public void recordMessageCount(int count) {
        messageCount.add(count);
    }

    /**
     * 获取所有指标的快照。
     *
     * @return 不可变的指标Map
     */
    public Map<String, Number> getMetricsSnapshot() {
        Map<String, Number> snapshot = new ConcurrentHashMap<>();
        snapshot.put("easyagent.tokens.input", inputTokens.sum());
        snapshot.put("easyagent.tokens.output", outputTokens.sum());
        snapshot.put("easyagent.tool.executions", toolExecutions.sum());
        snapshot.put("easyagent.tool.errors", toolErrors.sum());
        snapshot.put("easyagent.tool.duration_ms", toolDurationMs.sum());
        snapshot.put("easyagent.agent.runs", agentRuns.sum());
        snapshot.put("easyagent.agent.total_turns", totalTurns.sum());
        snapshot.put("easyagent.agent.total_duration_ms", totalDurationMs.sum());
        snapshot.put("easyagent.messages.count", messageCount.sum());

        // 按模型分组
        tokensByModel.forEach((key, adder) -> snapshot.put("easyagent." + key, adder.sum()));

        return Map.copyOf(snapshot);
    }

    /**
     * 重置所有指标数据。
     */
    public void reset() {
        inputTokens.reset();
        outputTokens.reset();
        toolExecutions.reset();
        toolErrors.reset();
        toolDurationMs.reset();
        agentRuns.reset();
        totalTurns.reset();
        totalDurationMs.reset();
        messageCount.reset();
        tokensByModel.clear();
    }
}
