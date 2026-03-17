package com.easyagent4j.spring.chat;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adapts Spring AI ChatModel to core ChatModel interface.
 */
public class SpringAiChatModelAdapter implements com.easyagent4j.core.chat.ChatModel {

    private final org.springframework.ai.chat.model.ChatModel springChatModel;

    public SpringAiChatModelAdapter(org.springframework.ai.chat.model.ChatModel springChatModel) {
        this.springChatModel = springChatModel;
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        List<Message> springMessages = convertMessages(request.getMessages());
        Prompt prompt = new Prompt(springMessages);

        org.springframework.ai.chat.model.ChatResponse springResponse = springChatModel.call(prompt);
        return convertResponse(springResponse);
    }

    @Override
    public void stream(ChatRequest request, Consumer<ChatResponseChunk> callback) {
        List<Message> springMessages = convertMessages(request.getMessages());
        Prompt prompt = new Prompt(springMessages);

        Flux<org.springframework.ai.chat.model.ChatResponse> flux = springChatModel.stream(prompt);
        flux.subscribe(chunk -> {
            callback.accept(convertChunk(chunk));
        });
    }

    @SuppressWarnings("unchecked")
    private List<Message> convertMessages(List<?> messages) {
        List<Message> result = new ArrayList<>();
        for (Object msg : messages) {
            if (msg instanceof Message) {
                result.add((Message) msg);
            }
        }
        return result;
    }

    private ChatResponse convertResponse(org.springframework.ai.chat.model.ChatResponse springResponse) {
        ChatResponse response = new ChatResponse();
        if (springResponse.getResult() != null && springResponse.getResult().getOutput() != null) {
            response.setText(springResponse.getResult().getOutput().getText());
        }
        return response;
    }

    private ChatResponseChunk convertChunk(org.springframework.ai.chat.model.ChatResponse chunk) {
        ChatResponseChunk result = new ChatResponseChunk();
        if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
            result.setText(chunk.getResult().getOutput().getText());
        }
        return result;
    }
}
