package com.easyagent4j.core.tool;

/**
 * 工具定义接口。
 * 实现此接口来定义Agent可调用的工具。
 */
public interface AgentTool {
    String getName();
    String getDescription();
    String getParameterSchema();
    ToolResult execute(ToolContext context);
}
