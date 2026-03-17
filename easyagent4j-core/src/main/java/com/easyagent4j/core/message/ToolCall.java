package com.easyagent4j.core.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 工具调用请求 — 由LLM生成，嵌入在AssistantMessage中。
 */
public class ToolCall {
    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("function")
    private FunctionCall function;

    public ToolCall() {}

    public ToolCall(String id, String name, String arguments) {
        this.id = id;
        this.type = "function";
        this.function = new FunctionCall(name, arguments);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public String getName() { return function != null ? function.name : null; }
    public String getArguments() { return function != null ? function.arguments : null; }

    public static class FunctionCall {
        @JsonProperty("name")
        public String name;
        @JsonProperty("arguments")
        public String arguments;
        public FunctionCall() {}
        public FunctionCall(String name, String arguments) {
            this.name = name; this.arguments = arguments;
        }
    }
}
