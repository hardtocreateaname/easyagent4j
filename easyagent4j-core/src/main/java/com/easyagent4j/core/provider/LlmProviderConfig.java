package com.easyagent4j.core.provider;

/**
 * LLM Provider配置（Builder模式）。
 */
public class LlmProviderConfig {
    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final int timeoutSeconds;

    private LlmProviderConfig(Builder builder) {
        this.provider = builder.provider;
        this.baseUrl = builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.timeoutSeconds = builder.timeoutSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProvider() {
        return provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public static class Builder {
        private String provider;
        private String baseUrl;
        private String apiKey;
        private String model;
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private int timeoutSeconds = 60;

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public LlmProviderConfig build() {
            return new LlmProviderConfig(this);
        }
    }
}