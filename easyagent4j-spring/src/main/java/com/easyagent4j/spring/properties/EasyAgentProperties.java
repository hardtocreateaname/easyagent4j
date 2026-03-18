package com.easyagent4j.spring.properties;

import com.easyagent4j.core.tool.ToolExecutionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easyagent")
public class EasyAgentProperties {
    private String systemPrompt = "You are a helpful assistant.";
    private int maxToolIterations = 10;
    private ToolExecutionMode toolExecutionMode = ToolExecutionMode.PARALLEL;
    private boolean streaming = true;
    private String chatProvider = "openai";
    private ProviderProperties provider = new ProviderProperties();
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
    public String getChatProvider() { return chatProvider; }
    public void setChatProvider(String chatProvider) { this.chatProvider = chatProvider; }
    public ProviderProperties getProvider() { return provider; }
    public void setProvider(ProviderProperties provider) { this.provider = provider; }
    public ContextProperties getContext() { return context; }
    public void setContext(ContextProperties context) { this.context = context; }
    public MemoryProperties getMemory() { return memory; }
    public void setMemory(MemoryProperties memory) { this.memory = memory; }
    public PersonalityProperties getPersonality() { return personality; }
    public void setPersonality(PersonalityProperties personality) { this.personality = personality; }
    public boolean isAutonomousMode() { return autonomousMode; }
    public void setAutonomousMode(boolean autonomousMode) { this.autonomousMode = autonomousMode; }

    public static class ProviderProperties {
        private OpenAiProviderProperties openai = new OpenAiProviderProperties();
        private AnthropicProviderProperties anthropic = new AnthropicProviderProperties();
        private ZhipuProviderProperties zhipu = new ZhipuProviderProperties();
        private MiniMaxProviderProperties minimax = new MiniMaxProviderProperties();

        public OpenAiProviderProperties getOpenai() { return openai; }
        public void setOpenai(OpenAiProviderProperties openai) { this.openai = openai; }
        public AnthropicProviderProperties getAnthropic() { return anthropic; }
        public void setAnthropic(AnthropicProviderProperties anthropic) { this.anthropic = anthropic; }
        public ZhipuProviderProperties getZhipu() { return zhipu; }
        public void setZhipu(ZhipuProviderProperties zhipu) { this.zhipu = zhipu; }
        public MiniMaxProviderProperties getMinimax() { return minimax; }
        public void setMinimax(MiniMaxProviderProperties minimax) { this.minimax = minimax; }
    }

    public static class OpenAiProviderProperties {
        private boolean enabled = false;
        private String baseUrl = "https://api.openai.com";
        private String apiKey;
        private String model = "gpt-4o";
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private int timeoutSeconds = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class AnthropicProviderProperties {
        private boolean enabled = false;
        private String baseUrl = "https://api.anthropic.com";
        private String apiKey;
        private String model = "claude-sonnet-4-20250514";
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private int timeoutSeconds = 60;
        private String version = "2023-06-01";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    public static class ZhipuProviderProperties {
        private boolean enabled = false;
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
        private String apiKey;
        private String model = "glm-4";
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private int timeoutSeconds = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class MiniMaxProviderProperties {
        private boolean enabled = false;
        private String baseUrl = "https://api.minimax.chat";
        private String apiKey;
        private String model = "abab6.5s-chat";
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private int timeoutSeconds = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

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
