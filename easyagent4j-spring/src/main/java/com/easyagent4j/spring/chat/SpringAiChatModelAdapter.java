package com.easyagent4j.spring.chat;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.AssistantMessage;
import com.easyagent4j.core.message.SystemMessage;
import com.easyagent4j.core.message.ToolResultMessage;
import com.easyagent4j.core.message.UserMessage;
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
                // Already a Spring AI Message
                result.add((Message) msg);
            } else if (msg instanceof UserMessage) {
                // Convert easyagent4j UserMessage to Spring AI UserMessage
                UserMessage userMsg = (UserMessage) msg;
                result.add(new org.springframework.ai.chat.messages.UserMessage(userMsg.getTextContent()));
            } else if (msg instanceof AssistantMessage) {
                // Convert easyagent4j AssistantMessage to Spring AI AssistantMessage
                AssistantMessage assistantMsg = (AssistantMessage) msg;
                result.add(new org.springframework.ai.chat.messages.AssistantMessage(assistantMsg.getTextContent()));
            } else if (msg instanceof SystemMessage) {
                // Convert easyagent4j SystemMessage to Spring AI SystemMessage
                SystemMessage systemMsg = (SystemMessage) msg;
                result.add(new org.springframework.ai.chat.messages.SystemMessage(systemMsg.getTextContent()));
            } else if (msg instanceof ToolResultMessage) {
                // Convert easyagent4j ToolResultMessage to Spring AI ToolResponseMessage
                ToolResultMessage toolResultMsg = (ToolResultMessage) msg;
                org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse toolResponse =
                    new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse(
                        toolResultMsg.getToolCall().getId(),
                        toolResultMsg.getToolCall().getName(),
                        toolResultMsg.getResultContent()
                    );
                result.add(org.springframework.ai.chat.messages.ToolResponseMessage.builder()
                    .responses(java.util.List.of(toolResponse))
                    .build());
            } else if (msg instanceof AgentMessage) {
                // Generic AgentMessage fallback
                AgentMessage agentMsg = (AgentMessage) msg;
                String content = agentMsg.getContent() != null ? agentMsg.getContent().toString() : "";
                switch (agentMsg.getRole()) {
                    case USER -> result.add(new org.springframework.ai.chat.messages.UserMessage(content));
                    case ASSISTANT -> result.add(new org.springframework.ai.chat.messages.AssistantMessage(content));
                    case SYSTEM -> result.add(new org.springframework.ai.chat.messages.SystemMessage(content));
                    case TOOL_RESULT -> {
                        // Tool result messages handled separately above
                    }
                }
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
