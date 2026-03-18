package com.easyagent4j.provider.openai;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.message.*;
import com.easyagent4j.core.provider.LlmProviderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI Provider单元测试。
 */
class OpenAiProviderTest {

    private OpenAiProvider provider;

    @BeforeEach
    void setUp() {
        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider("openai")
                .baseUrl("https://api.openai.com")
                .apiKey("test-api-key")
                .model("gpt-4o")
                .maxTokens(4096)
                .temperature(0.7)
                .timeoutSeconds(60)
                .build();

        provider = new OpenAiProvider(config);
    }

    @Test
    void testGetName() {
        assertEquals("openai", provider.getName());
    }

    @Test
    void testSupportsToolCalling() {
        assertTrue(provider.supportsToolCalling());
    }

    @Test
    void testSupportsStreaming() {
        assertTrue(provider.supportsStreaming());
    }

    @Test
    void testSupportsVision() {
        assertTrue(provider.supportsVision());
    }

    @Test
    void testBuildRequestWithSimpleMessage() {
        List<Object> messages = new ArrayList<>();
        messages.add(new UserMessage("Hello, how are you?"));

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .systemPrompt("You are a helpful assistant.")
                .build();

        assertNotNull(request);
        assertEquals(1, request.getMessages().size());
        assertNotNull(request.getSystemPrompt());
    }

    @Test
    void testBuildRequestWithTools() {
        List<Object> messages = new ArrayList<>();
        messages.add(new UserMessage("What's the weather like?"));

        List<com.easyagent4j.core.tool.ToolDefinition> tools = new ArrayList<>();
        tools.add(new com.easyagent4j.core.tool.ToolDefinition(
                "get_weather",
                "Get current weather",
                "{\"type\": \"object\", \"properties\": {\"location\": {\"type\": \"string\"}}}"
        ));

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .tools(tools)
                .build();

        assertNotNull(request);
        assertEquals(1, request.getTools().size());
        assertEquals("get_weather", request.getTools().get(0).getName());
    }

    @Test
    void testBuildRequestWithToolResults() {
        List<Object> messages = new ArrayList<>();
        messages.add(new UserMessage("What's the weather like?"));
        messages.add(new AssistantMessage("", List.of(new ToolCall("call_123", "get_weather", "{\"location\": \"Beijing\"}"))));
        messages.add(ToolResultMessage.success(new ToolCall("call_123", "get_weather", "{\"location\": \"Beijing\"}"), "Sunny, 25°C"));

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();

        assertNotNull(request);
        assertEquals(3, request.getMessages().size());
    }

    @Test
    void testBuildRequestWithMultiModalContent() {
        List<ContentPart> parts = new ArrayList<>();
        parts.add(ContentPart.text("What's in this image?"));
        parts.add(ContentPart.builder()
                .type(ContentPart.Type.IMAGE)
                .imageData(new byte[]{1, 2, 3}, "image/jpeg")
                .build());

        UserMessage userMessage = new UserMessage("", parts);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(userMessage))
                .build();

        assertNotNull(request);
        assertEquals(1, request.getMessages().size());
        UserMessage msg = (UserMessage) request.getMessages().get(0);
        assertEquals(2, msg.getParts().size());
    }

    @Test
    void testToolCallCreation() {
        ToolCall toolCall = new ToolCall("call_abc123", "get_weather", "{\"location\": \"Beijing\"}");

        assertNotNull(toolCall);
        assertEquals("call_abc123", toolCall.getId());
        assertEquals("get_weather", toolCall.getName());
        assertEquals("{\"location\": \"Beijing\"}", toolCall.getArguments());
    }

    @Test
    void testToolResultMessageCreation() {
        ToolCall toolCall = new ToolCall("call_abc123", "get_weather", "{\"location\": \"Beijing\"}");
        ToolResultMessage successResult = ToolResultMessage.success(toolCall, "Sunny, 25°C");
        ToolResultMessage errorResult = ToolResultMessage.error(toolCall, "API Error");

        assertNotNull(successResult);
        assertNotNull(errorResult);
        assertFalse(successResult.isError());
        assertTrue(errorResult.isError());
    }

    @Test
    void testContentPartTypes() {
        ContentPart textPart = ContentPart.text("Hello");
        ContentPart imagePart = ContentPart.builder()
                .type(ContentPart.Type.IMAGE)
                .imageData(new byte[]{1, 2, 3}, "image/jpeg")
                .build();
        ContentPart toolCallPart = ContentPart.toolCall(new ToolCall("call_123", "func", "{}"));

        assertEquals(ContentPart.Type.TEXT, textPart.getType());
        assertEquals(ContentPart.Type.IMAGE, imagePart.getType());
        assertEquals(ContentPart.Type.TOOL_CALL, toolCallPart.getType());
    }
}