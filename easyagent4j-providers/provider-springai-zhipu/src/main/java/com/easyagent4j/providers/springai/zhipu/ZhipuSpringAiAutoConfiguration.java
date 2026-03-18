package com.easyagent4j.providers.springai.zhipu;

import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.providers.springai.SpringAiProviderAdapter;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Zhipu Spring AI Provider自动配置。
 * 
 * <p>当Spring AI的ZhiPuAiChatModel存在时，自动创建适配器将其包装为LlmProvider。</p>
 * 
 * <p>使用前提：</p>
 * <ol>
 *   <li>引入依赖：spring-ai-starter-model-zhipuai</li>
 *   <li>配置属性：spring.ai.zhipuai.api-key</li>
 *   <li>配置属性：spring.ai.model.chat=zhipuai（如果同时引入多个starter）</li>
 * </ol>
 */
@AutoConfiguration(after = org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration.class)
@ConditionalOnClass(ZhiPuAiChatModel.class)
public class ZhipuSpringAiAutoConfiguration {

    /**
     * 创建Zhipu Provider适配器。
     * 当Spring AI的ZhiPuAiChatModel存在时自动创建。
     * 
     * @param chatModelProvider Spring AI注入的ZhiPuAiChatModel
     * @return LlmProvider实例，如果ZhiPuAiChatModel不存在则返回null
     */
    @Bean("zhipuProvider")
    @ConditionalOnMissingBean(name = "zhipuProvider")
    public LlmProvider zhipuProvider(ObjectProvider<ZhiPuAiChatModel> chatModelProvider) {
        ZhiPuAiChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return null;
        }
        return new SpringAiProviderAdapter("zhipu", chatModel);
    }
}
