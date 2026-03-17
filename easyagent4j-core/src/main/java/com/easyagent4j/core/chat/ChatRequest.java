package com.easyagent4j.core.chat;

import com.easyagent4j.core.tool.ToolDefinition;
import java.util.List;
import java.util.Map;

/**
 * LLM请求模型。
 */
public class ChatRequest {
    private List<?> messages;
    private String systemPrompt;
    private List<ToolDefinition> tools;
    private boolean streaming;
    private Map<String, Object> options;

    private ChatRequest(Builder builder) {
        this.messages = builder.messages;
        this.systemPrompt = builder.systemPrompt;
        this.tools = builder.tools;
        this.streaming = builder.streaming;
        this.options = builder.options;
    }

    public static Builder builder() { return new Builder(); }

    public List<?> getMessages() { return messages; }
    public String getSystemPrompt() { return systemPrompt; }
    public List<ToolDefinition> getTools() { return tools; }
    public boolean isStreaming() { return streaming; }
    public Map<String, Object> getOptions() { return options; }

    public static class Builder {
        private List<?> messages;
        private String systemPrompt;
        private List<ToolDefinition> tools;
        private boolean streaming;
        private Map<String, Object> options;
        public Builder messages(List<?> m) { this.messages = m; return this; }
        public Builder systemPrompt(String s) { this.systemPrompt = s; return this; }
        public Builder tools(List<ToolDefinition> t) { this.tools = t; return this; }
        public Builder streaming(boolean s) { this.streaming = s; return this; }
        public Builder options(Map<String, Object> o) { this.options = o; return this; }
        public ChatRequest build() { return new ChatRequest(this); }
    }
}
