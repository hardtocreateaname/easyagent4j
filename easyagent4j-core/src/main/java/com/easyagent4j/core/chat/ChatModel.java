package com.easyagent4j.core.chat;

import java.util.function.Consumer;

/**
 * LLM调用接口 — core层定义的抽象。
 * spring层提供Spring AI的实现。
 */
public interface ChatModel {
    ChatResponse call(ChatRequest request);
    void stream(ChatRequest request, Consumer<ChatResponseChunk> callback);
}
