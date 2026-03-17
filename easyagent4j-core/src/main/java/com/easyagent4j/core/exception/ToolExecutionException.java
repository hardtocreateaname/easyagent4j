package com.easyagent4j.core.exception;

/**
 * 工具执行异常。
 */
public class ToolExecutionException extends AgentException {
    private final String toolName;

    public ToolExecutionException(String toolName, String message) {
        super("Tool [" + toolName + "] execution failed: " + message);
        this.toolName = toolName;
    }

    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super("Tool [" + toolName + "] execution failed: " + message, cause);
        this.toolName = toolName;
    }

    public String getToolName() { return toolName; }
}
