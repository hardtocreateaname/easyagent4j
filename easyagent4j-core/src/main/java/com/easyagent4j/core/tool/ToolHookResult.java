package com.easyagent4j.core.tool;

/**
 * 工具钩子结果 — 用于beforeToolCall决定是否阻止执行。
 */
public class ToolHookResult {
    private final boolean blocked;
    private final String reason;

    private ToolHookResult(boolean blocked, String reason) {
        this.blocked = blocked;
        this.reason = reason;
    }

    public static ToolHookResult block(String reason) { return new ToolHookResult(true, reason); }
    public static ToolHookResult allow() { return new ToolHookResult(false, null); }

    public boolean isBlocked() { return blocked; }
    public String getReason() { return reason; }
}
