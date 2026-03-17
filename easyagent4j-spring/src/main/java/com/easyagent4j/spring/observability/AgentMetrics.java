package com.easyagent4j.spring.observability;

/**
 * Agent指标采集接口 — 提供轻量级的可观测性抽象。
 * <p>
 * 默认实现 {@link DefaultAgentMetrics} 使用内存计数器，零外部依赖。
 * 当classpath中存在Micrometer时，可通过{@link com.easyagent4j.spring.autoconfigure.EasyAgentAutoConfiguration}
 * 自动替换为Micrometer-backed实现。
 * </p>
 *
 * @author Chen Jiaming
 * @since 0.1.0
 */
public interface AgentMetrics {

    /**
     * 记录Token使用量。
     *
     * @param model        模型名称（如 "gpt-4o", "qwen-plus"）
     * @param inputTokens  输入Token数
     * @param outputTokens 输出Token数
     */
    void recordTokenUsage(String model, int inputTokens, int outputTokens);

    /**
     * 记录工具执行情况。
     *
     * @param toolName   工具名称
     * @param durationMs 执行耗时（毫秒）
     * @param success    是否成功
     */
    void recordToolExecution(String toolName, long durationMs, boolean success);

    /**
     * 记录一次Agent运行。
     *
     * @param agentId    Agent标识（通常为sessionId）
     * @param durationMs 运行总耗时（毫秒）
     * @param turns      对话轮次
     * @param status     运行状态（如 "completed", "error", "aborted"）
     */
    void recordAgentRun(String agentId, long durationMs, int turns, String status);

    /**
     * 记录消息数量。
     *
     * @param count 本轮新增消息数
     */
    void recordMessageCount(int count);
}
