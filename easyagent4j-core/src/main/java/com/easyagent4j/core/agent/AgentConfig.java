package com.easyagent4j.core.agent;

import com.easyagent4j.core.resilience.RetryPolicy;
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
    private final PersonalityConfig personality;
    private final MemoryConfig memory;
    private final RetryPolicy retryPolicy;
    private final boolean autonomousMode;

    private AgentConfig(Builder builder) {
        this.systemPrompt = builder.systemPrompt;
        this.maxToolIterations = builder.maxToolIterations;
        this.toolExecutionMode = builder.toolExecutionMode;
        this.streamingEnabled = builder.streamingEnabled;
        this.sessionId = builder.sessionId;
        this.personality = builder.personality;
        this.memory = builder.memory;
        this.retryPolicy = builder.retryPolicy;
        this.autonomousMode = builder.autonomousMode;
    }

    public static Builder builder() { return new Builder(); }

    public String getSystemPrompt() { return systemPrompt; }
    public int getMaxToolIterations() { return maxToolIterations; }
    public ToolExecutionMode getToolExecutionMode() { return toolExecutionMode; }
    public boolean isStreamingEnabled() { return streamingEnabled; }
    public String getSessionId() { return sessionId; }
    public PersonalityConfig getPersonality() { return personality; }
    public MemoryConfig getMemory() { return memory; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    public boolean isAutonomousMode() { return autonomousMode; }

    /**
     * 应用system prompt覆盖（由转向机制使用）。
     */
    public void applySystemPromptOverride(String overridePrompt) {
        this.systemPrompt = overridePrompt;
    }

    /**
     * 性格配置。
     */
    public static class PersonalityConfig {
        private final String name;
        private final String personalityPath;

        public PersonalityConfig(String name, String personalityPath) {
            this.name = name;
            this.personalityPath = personalityPath;
        }

        public String getName() { return name; }
        public String getPersonalityPath() { return personalityPath; }
    }

    /**
     * 记忆配置。
     */
    public static class MemoryConfig {
        private final String basePath;
        private final boolean enabled;
        private final boolean autoConsolidate;

        public MemoryConfig(String basePath, boolean enabled, boolean autoConsolidate) {
            this.basePath = basePath;
            this.enabled = enabled;
            this.autoConsolidate = autoConsolidate;
        }

        public String getBasePath() { return basePath; }
        public boolean isEnabled() { return enabled; }
        public boolean isAutoConsolidate() { return autoConsolidate; }
    }

    public static class Builder {
        private String systemPrompt = "You are a helpful assistant.";
        private int maxToolIterations = 10;
        private ToolExecutionMode toolExecutionMode = ToolExecutionMode.PARALLEL;
        private boolean streamingEnabled = true;
        private String sessionId;
        private PersonalityConfig personality;
        private MemoryConfig memory;
        private RetryPolicy retryPolicy;
        private boolean autonomousMode = false;

        public Builder systemPrompt(String s) { this.systemPrompt = s; return this; }
        public Builder maxToolIterations(int n) { this.maxToolIterations = Math.max(1, n); return this; }
        public Builder toolExecutionMode(ToolExecutionMode m) { this.toolExecutionMode = m; return this; }
        public Builder streamingEnabled(boolean e) { this.streamingEnabled = e; return this; }
        public Builder sessionId(String id) { this.sessionId = id; return this; }
        public Builder personality(PersonalityConfig personality) {
            this.personality = personality;
            return this;
        }
        public Builder memory(MemoryConfig memory) {
            this.memory = memory;
            return this;
        }
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }
        public Builder autonomousMode(boolean autonomousMode) {
            this.autonomousMode = autonomousMode;
            return this;
        }
        public AgentConfig build() { return new AgentConfig(this); }
    }
}
