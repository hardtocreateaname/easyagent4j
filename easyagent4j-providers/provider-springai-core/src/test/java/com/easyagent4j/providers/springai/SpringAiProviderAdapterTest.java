package com.easyagent4j.providers.springai;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.message.AssistantMessage;
import com.easyagent4j.core.message.ToolCall;
import com.easyagent4j.core.message.ToolResultMessage;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SpringAiProviderAdapterTest {

    @Test
    void shouldConvertMapMessagesIntoSpringAiPromptMessages() {
        CapturingChatModel chatModel = new CapturingChatModel();
        SpringAiProviderAdapter adapter = new SpringAiProviderAdapter("zhipu", chatModel);

        adapter.call(ChatRequest.builder()
            .systemPrompt("system prompt")
            .messages(List.of(Map.of("role", "user", "content", "hello")))
            .build());

        List<Message> instructions = chatModel.prompt.getInstructions();
        assertEquals(2, instructions.size());
        assertEquals("system", instructions.get(0).getMessageType().getValue());
        assertEquals("user", instructions.get(1).getMessageType().getValue());
        assertEquals("hello", instructions.get(1).getText());
    }

    @Test
    void shouldConvertToolConversationMessagesIntoSpringAiPromptMessages() {
        CapturingChatModel chatModel = new CapturingChatModel();
        SpringAiProviderAdapter adapter = new SpringAiProviderAdapter("zhipu", chatModel);
        ToolCall toolCall = new ToolCall("call_1", "weather", "{\"city\":\"Shanghai\"}");

        adapter.call(ChatRequest.builder()
            .messages(List.of(
                new AssistantMessage("", List.of(toolCall)),
                ToolResultMessage.success(toolCall, "{\"temperature\":22}")
            ))
            .build());

        List<Message> instructions = chatModel.prompt.getInstructions();
        assertEquals(2, instructions.size());
        org.springframework.ai.chat.messages.AssistantMessage assistantMessage =
            assertInstanceOf(org.springframework.ai.chat.messages.AssistantMessage.class, instructions.get(0));
        assertEquals(1, assistantMessage.getToolCalls().size());
        ToolResponseMessage toolResponseMessage =
            assertInstanceOf(ToolResponseMessage.class, instructions.get(1));
        assertEquals("call_1", toolResponseMessage.getResponses().get(0).id());
    }

    private static final class CapturingChatModel implements org.springframework.ai.chat.model.ChatModel {
        private Prompt prompt;

        @Override
        public ChatResponse call(Prompt prompt) {
            this.prompt = prompt;
            return new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("ok"))));
        }
    }
}
