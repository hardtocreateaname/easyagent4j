package com.easyagent4j.provider.anthropic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Anthropic Provider配置属性。
 */
@ConfigurationProperties(prefix = "easyagent.provider.anthropic")
public class AnthropicProviderProperties {
    private String baseUrl = "https://api.anthropic.com";
    private String apiKey;
    private String model = "claude-sonnet-4-20250514";
    private int maxTokens = 4096;
    private double temperature = 0.7;
    private int timeoutSeconds = 60;
    private String version = "2023-06-01";

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