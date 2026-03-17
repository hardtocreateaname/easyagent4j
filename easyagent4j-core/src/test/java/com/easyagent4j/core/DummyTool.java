package com.easyagent4j.core;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

/**
 * 用于测试的简单工具 — 返回固定字符串。
 */
public class DummyTool extends AbstractAgentTool {

    private final String result;
    private int callCount = 0;

    public DummyTool(String name, String description, String result) {
        super(name, description);
        this.result = result;
    }

    public DummyTool(String name) {
        this(name, "A dummy tool for testing", "ok");
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        callCount++;
        return ToolResult.success(result);
    }

    public int getCallCount() {
        return callCount;
    }

    /**
     * 重置调用计数。
     */
    public void reset() {
        callCount = 0;
    }
}
