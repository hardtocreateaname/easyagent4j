package com.easyagent4j.spring.properties;

import com.easyagent4j.core.tool.ToolExecutionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easyagent")
public class EasyAgentProperties {
    private String systemPrompt = "You are a helpful assistant.";
    private int maxToolIterations = 10;
    private ToolExecutionMode toolExecutionMode = ToolExecutionMode.PARALLEL;
    private boolean streaming = true;
    private ContextProperties context = new ContextProperties();

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public int getMaxToolIterations() { return maxToolIterations; }
    public void setMaxToolIterations(int maxToolIterations) { this.maxToolIterations = maxToolIterations; }
    public ToolExecutionMode getToolExecutionMode() { return toolExecutionMode; }
    public void setToolExecutionMode(ToolExecutionMode toolExecutionMode) { this.toolExecutionMode = toolExecutionMode; }
    public boolean isStreaming() { return streaming; }
    public void setStreaming(boolean streaming) { this.streaming = streaming; }
    public ContextProperties getContext() { return context; }
    public void setContext(ContextProperties context) { this.context = context; }

    public static class ContextProperties {
        private int maxMessages = 50;
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
    }
}
