package com.easyagent4j.provider.anthropic;

import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.core.provider.LlmProviderConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Anthropic Provider自动配置。
 * 通过配置项 easyagent.provider.anthropic.enabled 控制是否启用。
 */
@Configuration
@ConditionalOnClass(AnthropicProvider.class)
@ConditionalOnProperty(prefix = "easyagent.provider.anthropic", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AnthropicProviderProperties.class)
public class AnthropicAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "anthropicProvider")
    public LlmProvider anthropicProvider(AnthropicProviderProperties properties) {
        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider("anthropic")
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .temperature(properties.getTemperature())
                .timeoutSeconds(properties.getTimeoutSeconds())
                .build();

        return new AnthropicProvider(config);
    }
}