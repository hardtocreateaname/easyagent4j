package com.easyagent4j.providers.http.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI Provider配置属性。
 */
@ConfigurationProperties(prefix = "easyagent.provider.openai")
public class OpenAiProviderProperties {
    private String baseUrl = "https://api.openai.com";
    private String apiKey;
    private String model = "gpt-4o";
    private int maxTokens = 4096;
    private double temperature = 0.7;
    private int timeoutSeconds = 60;

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