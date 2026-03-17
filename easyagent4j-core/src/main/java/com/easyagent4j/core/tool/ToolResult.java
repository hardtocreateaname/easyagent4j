package com.easyagent4j.core.tool;

import java.util.*;

/**
 * 工具执行结果。
 */
public class ToolResult {
    private final boolean success;
    private final String content;
    private final Map<String, Object> details;

    private ToolResult(boolean success, String content, Map<String, Object> details) {
        this.success = success;
        this.content = content;
        this.details = details != null ? details : Collections.emptyMap();
    }

    public static ToolResult success(String content) {
        return new ToolResult(true, content, null);
    }

    public static ToolResult success(String content, Map<String, Object> details) {
        return new ToolResult(true, content, details);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, errorMessage, null);
    }

    public boolean isSuccess() { return success; }
    public String getContent() { return content; }
    public Map<String, Object> getDetails() { return details; }
}
