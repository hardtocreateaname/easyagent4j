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
    private MemoryProperties memory = new MemoryProperties();
    private PersonalityProperties personality = new PersonalityProperties();
    private boolean autonomousMode = false;

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
    public MemoryProperties getMemory() { return memory; }
    public void setMemory(MemoryProperties memory) { this.memory = memory; }
    public PersonalityProperties getPersonality() { return personality; }
    public void setPersonality(PersonalityProperties personality) { this.personality = personality; }
    public boolean isAutonomousMode() { return autonomousMode; }
    public void setAutonomousMode(boolean autonomousMode) { this.autonomousMode = autonomousMode; }

    public static class ContextProperties {
        private int maxMessages = 50;
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
    }

    public static class MemoryProperties {
        private String basePath = "./agent-memory";
        private boolean enabled = true;
        private boolean autoConsolidate = false;

        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isAutoConsolidate() { return autoConsolidate; }
        public void setAutoConsolidate(boolean autoConsolidate) { this.autoConsolidate = autoConsolidate; }
    }

    public static class PersonalityProperties {
        private String name;
        private String personalityPath;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPersonalityPath() { return personalityPath; }
        public void setPersonalityPath(String personalityPath) { this.personalityPath = personalityPath; }
    }
}
