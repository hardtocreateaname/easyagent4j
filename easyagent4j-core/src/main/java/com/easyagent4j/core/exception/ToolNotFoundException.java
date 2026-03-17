package com.easyagent4j.core.exception;

/**
 * 工具未找到异常。
 */
public class ToolNotFoundException extends AgentException {
    public ToolNotFoundException(String toolName) {
        super("Tool not found: " + toolName);
    }
}
