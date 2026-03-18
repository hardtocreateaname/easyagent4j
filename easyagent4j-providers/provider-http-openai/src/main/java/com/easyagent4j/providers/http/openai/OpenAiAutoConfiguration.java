package com.easyagent4j.providers.http.openai;

import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.core.provider.LlmProviderConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI Provider自动配置。
 * 通过配置项 easyagent.provider.openai.enabled 控制是否启用。
 */
@Configuration
@ConditionalOnClass(OpenAiProvider.class)
@ConditionalOnProperty(prefix = "easyagent.provider.openai", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenAiProviderProperties.class)
public class OpenAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "openaiProvider")
    public LlmProvider openaiProvider(OpenAiProviderProperties properties) {
        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider("openai")
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .temperature(properties.getTemperature())
                .timeoutSeconds(properties.getTimeoutSeconds())
                .build();

        return new OpenAiProvider(config);
    }
}