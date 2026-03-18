package com.easyagent4j.core.provider;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;

import java.util.function.Consumer;

/**
 * LLM Provider接口 — 支持多种模型接入。
 * 实现类包括：OpenAiProvider, AnthropicProvider, ZhipuProvider等。
 */
public interface LlmProvider {

    /**
     * 获取Provider名称。
     * @return 名称，如 "openai", "anthropic", "zhipu", "minimax"
     */
    String getName();

    /**
     * 同步调用LLM。
     * @param request 请求参数
     * @return 响应结果
     */
    ChatResponse call(ChatRequest request);

    /**
     * 流式调用LLM。
     * @param request 请求参数
     * @param callback 流式响应回调
     */
    void stream(ChatRequest request, Consumer<ChatResponseChunk> callback);

    /**
     * 是否支持工具调用（function calling）。
     * @return true支持，false不支持
     */
    boolean supportsToolCalling();

    /**
     * 是否支持流式响应。
     * @return true支持，false不支持
     */
    boolean supportsStreaming();

    /**
     * 是否支持视觉能力（多模态）。
     * @return true支持，false不支持
     */
    boolean supportsVision();
}