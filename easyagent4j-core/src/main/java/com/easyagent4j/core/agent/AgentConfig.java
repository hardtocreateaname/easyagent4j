package com.easyagent4j.core.agent;

import com.easyagent4j.core.tool.ToolExecutionMode;

/**
 * Agent配置（Builder模式）。
 */
public class AgentConfig {
    private String systemPrompt;
    private final int maxToolIterations;
    private final ToolExecutionMode toolExecutionMode;
    private final boolean streamingEnabled;
    private final String sessionId;

    private AgentConfig(Builder builder) {
        this.systemPrompt = builder.systemPrompt;
        this.maxToolIterations = builder.maxToolIterations;
        this.toolExecutionMode = builder.toolExecutionMode;
        this.streamingEnabled = builder.streamingEnabled;
        this.sessionId = builder.sessionId;
    }

    public static Builder builder() { return new Builder(); }

    public String getSystemPrompt() { return systemPrompt; }
    public int getMaxToolIterations() { return maxToolIterations; }
    public ToolExecutionMode getToolExecutionMode() { return toolExecutionMode; }
    public boolean isStreamingEnabled() { return streamingEnabled; }
    public String getSessionId() { return sessionId; }

    /**
     * 应用system prompt覆盖（由转向机制使用）。
     */
    public void applySystemPromptOverride(String overridePrompt) {
        this.systemPrompt = overridePrompt;
    }

    public static class Builder {
        private String systemPrompt = "You are a helpful assistant.";
        private int maxToolIterations = 10;
        private ToolExecutionMode toolExecutionMode = ToolExecutionMode.PARALLEL;
        private boolean streamingEnabled = true;
        private String sessionId;

        public Builder systemPrompt(String s) { this.systemPrompt = s; return this; }
        public Builder maxToolIterations(int n) { this.maxToolIterations = Math.max(1, n); return this; }
        public Builder toolExecutionMode(ToolExecutionMode m) { this.toolExecutionMode = m; return this; }
        public Builder streamingEnabled(boolean e) { this.streamingEnabled = e; return this; }
        public Builder sessionId(String id) { this.sessionId = id; return this; }
        public AgentConfig build() { return new AgentConfig(this); }
    }
}
