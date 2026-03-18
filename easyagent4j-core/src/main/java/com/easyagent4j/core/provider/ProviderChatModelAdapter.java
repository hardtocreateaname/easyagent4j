package com.easyagent4j.core.provider;

import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;

import java.util.function.Consumer;

/**
 * LlmProvider到ChatModel的适配器。
 * 将Provider实现适配为ChatModel接口，便于在Agent中使用。
 */
public class ProviderChatModelAdapter implements ChatModel {
    private final LlmProvider provider;

    public ProviderChatModelAdapter(LlmProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        this.provider = provider;
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        return provider.call(request);
    }

    @Override
    public void stream(ChatRequest request, Consumer<ChatResponseChunk> callback) {
        provider.stream(request, callback);
    }

    /**
     * 获取内部持有的Provider。
     * @return Provider实例
     */
    public LlmProvider getProvider() {
        return provider;
    }

    /**
     * 获取Provider名称。
     * @return Provider名称
     */
    public String getProviderName() {
        return provider.getName();
    }

    /**
     * 是否支持工具调用。
     * @return true支持，false不支持
     */
    public boolean supportsToolCalling() {
        return provider.supportsToolCalling();
    }

    /**
     * 是否支持流式响应。
     * @return true支持，false不支持
     */
    public boolean supportsStreaming() {
        return provider.supportsStreaming();
    }

    /**
     * 是否支持视觉能力。
     * @return true支持，false不支持
     */
    public boolean supportsVision() {
        return provider.supportsVision();
    }
}