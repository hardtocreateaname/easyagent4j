package com.easyagent4j.provider.minimax;

import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.core.provider.LlmProviderConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MiniMax Provider自动配置。
 * 通过配置项 easyagent.provider.minimax.enabled 控制是否启用。
 */
@Configuration
@ConditionalOnClass(MiniMaxProvider.class)
@ConditionalOnProperty(prefix = "easyagent.provider.minimax", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MiniMaxProviderProperties.class)
public class MiniMaxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "minimaxProvider")
    public LlmProvider minimaxProvider(MiniMaxProviderProperties properties) {
        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider("minimax")
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .temperature(properties.getTemperature())
                .timeoutSeconds(properties.getTimeoutSeconds())
                .build();

        return new MiniMaxProvider(config);
    }
}