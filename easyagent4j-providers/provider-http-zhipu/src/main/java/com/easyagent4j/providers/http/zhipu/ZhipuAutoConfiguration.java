package com.easyagent4j.providers.http.zhipu;

import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.core.provider.LlmProviderConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Zhipu Provider自动配置。
 * 通过配置项 easyagent.provider.zhipu.enabled 控制是否启用。
 */
@Configuration
@ConditionalOnClass(ZhipuProvider.class)
@ConditionalOnProperty(prefix = "easyagent.provider.zhipu", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ZhipuProviderProperties.class)
public class ZhipuAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "zhipuProvider")
    public LlmProvider zhipuProvider(ZhipuProviderProperties properties) {
        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider("zhipu")
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .temperature(properties.getTemperature())
                .timeoutSeconds(properties.getTimeoutSeconds())
                .build();

        return new ZhipuProvider(config);
    }
}