package com.easyagent4j.core;

import com.easyagent4j.core.chat.*;
import com.easyagent4j.core.message.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Mock ChatModel — 用于集成测试。
 * 支持预设响应序列，按顺序返回。
 * 支持模拟纯文本回复和工具调用。
 */
public class MockChatModel implements ChatModel {

    private final Queue<ChatResponse> responses = new ConcurrentLinkedQueue<>();
    private final Queue<ChatResponseChunk> streamChunks = new ConcurrentLinkedQueue<>();

    /**
     * 添加一个纯文本响应。
     */
    public MockChatModel addTextResponse(String text) {
        responses.add(new ChatResponse(text));
        return this;
    }

    /**
     * 添加一个工具调用响应。
     */
    public MockChatModel addToolCallResponse(List<ToolCall> toolCalls) {
        ChatResponse response = new ChatResponse();
        response.setToolCalls(toolCalls);
        responses.add(response);
        return this;
    }

    /**
     * 添加一个工具调用响应（单个工具调用）。
     */
    public MockChatModel addToolCallResponse(String id, String name, String arguments) {
        return addToolCallResponse(List.of(new ToolCall(id, name, arguments)));
    }

    /**
     * 添加流式文本块。
     */
    public MockChatModel addStreamChunk(String text) {
        streamChunks.add(new ChatResponseChunk(text));
        return this;
    }

    /**
     * 清空所有预设响应。
     */
    public MockChatModel clear() {
        responses.clear();
        streamChunks.clear();
        return this;
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        ChatResponse response = responses.poll();
        if (response != null) {
            return response;
        }
        // 默认返回空文本
        return new ChatResponse("(no preset response)");
    }

    @Override
    public void stream(ChatRequest request, Consumer<ChatResponseChunk> callback) {
        if (!streamChunks.isEmpty()) {
            ChatResponseChunk chunk;
            while ((chunk = streamChunks.poll()) != null) {
                callback.accept(chunk);
            }
        } else {
            // 如果没有预设流式块，从非流式响应模拟
            ChatResponse response = responses.poll();
            if (response != null) {
                String text = response.getText();
                if (text != null && !text.isEmpty()) {
                    callback.accept(new ChatResponseChunk(text));
                }
                if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
                    ChatResponseChunk tcChunk = new ChatResponseChunk();
                    tcChunk.setToolCalls(response.getToolCalls());
                    callback.accept(tcChunk);
                }
            }
        }
        // 发送完成标记
        ChatResponseChunk finishedChunk = new ChatResponseChunk();
        finishedChunk.setFinished(true);
        callback.accept(finishedChunk);
    }
}