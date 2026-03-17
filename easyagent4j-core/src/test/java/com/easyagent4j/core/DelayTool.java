package com.easyagent4j.core;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

/**
 * 延迟工具 — 用于测试并行执行和中断。
 */
public class DelayTool extends AbstractAgentTool {

    private final long delayMs;
    private int callCount = 0;

    public DelayTool(String name, long delayMs) {
        super(name, "A tool that delays for testing");
        this.delayMs = delayMs;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        callCount++;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.error("interrupted");
        }
        return ToolResult.success("delayed " + delayMs + "ms");
    }

    public int getCallCount() {
        return callCount;
    }
}
