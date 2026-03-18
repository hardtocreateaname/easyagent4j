package com.easyagent4j.providers.springai.anthropic;

import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.providers.springai.SpringAiProviderAdapter;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Anthropic Spring AI Provider自动配置。
 * 
 * <p>当Spring AI的AnthropicChatModel存在时，自动创建适配器将其包装为LlmProvider。</p>
 * 
 * <p>使用前提：</p>
 * <ol>
 *   <li>引入依赖：spring-ai-starter-model-anthropic</li>
 *   <li>配置属性：spring.ai.anthropic.api-key</li>
 *   <li>配置属性：spring.ai.model.chat=anthropic（如果同时引入多个starter）</li>
 * </ol>
 */
@AutoConfiguration(after = org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration.class)
@ConditionalOnClass(AnthropicChatModel.class)
public class AnthropicSpringAiAutoConfiguration {

    /**
     * 创建Anthropic Provider适配器。
     * 当Spring AI的AnthropicChatModel存在时自动创建。
     * 
     * @param chatModelProvider Spring AI注入的AnthropicChatModel
     * @return LlmProvider实例，如果AnthropicChatModel不存在则返回null
     */
    @Bean("anthropicProvider")
    @ConditionalOnMissingBean(name = "anthropicProvider")
    public LlmProvider anthropicProvider(ObjectProvider<AnthropicChatModel> chatModelProvider) {
        AnthropicChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return null;
        }
        return new SpringAiProviderAdapter("anthropic", chatModel);
    }
}
