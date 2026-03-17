package com.easyagent4j.core.tool;

/**
 * 工具元数据 — 不可变的工具定义信息。
 */
public class ToolDefinition {
    private final String name;
    private final String description;
    private final String parameterSchema;

    public ToolDefinition(String name, String description, String parameterSchema) {
        this.name = name;
        this.description = description;
        this.parameterSchema = parameterSchema != null ? parameterSchema : "{}";
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParameterSchema() { return parameterSchema; }
}
