package com.easyagent4j.providers.springai.openai;

import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.providers.springai.SpringAiProviderAdapter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * OpenAI Spring AI Provider自动配置。
 * 
 * <p>当Spring AI的OpenAiChatModel存在时，自动创建适配器将其包装为LlmProvider。</p>
 * 
 * <p>使用前提：</p>
 * <ol>
 *   <li>引入依赖：spring-ai-starter-model-openai</li>
 *   <li>配置属性：spring.ai.openai.api-key</li>
 *   <li>配置属性：spring.ai.model.chat=openai（如果同时引入多个starter）</li>
 * </ol>
 */
@AutoConfiguration(after = org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration.class)
@ConditionalOnClass(OpenAiChatModel.class)
public class OpenAiSpringAiAutoConfiguration {

    /**
     * 创建OpenAI Provider适配器。
     * 当Spring AI的OpenAiChatModel存在时自动创建。
     * 
     * @param chatModelProvider Spring AI注入的OpenAiChatModel
     * @return LlmProvider实例，如果OpenAiChatModel不存在则返回null
     */
    @Bean("openaiProvider")
    @ConditionalOnMissingBean(name = "openaiProvider")
    public LlmProvider openaiProvider(ObjectProvider<OpenAiChatModel> chatModelProvider) {
        OpenAiChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return null;
        }
        return new SpringAiProviderAdapter("openai", chatModel);
    }
}
