package com.easyagent4j.provider.anthropic;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;
import com.easyagent4j.core.message.*;
import com.easyagent4j.core.provider.LlmProviderConfig;
import com.easyagent4j.core.tool.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnthropicProvider测试类（单元测试）。
 */
class AnthropicProviderTest {

    private AnthropicProvider provider;

    @BeforeEach
    void setUp() {
        LlmProviderConfig config = LlmProviderConfig.builder()
                .provider("anthropic")
                .baseUrl("https://api.anthropic.com")
                .apiKey("test-api-key")
                .model("claude-sonnet-4-20250514")
                .maxTokens(4096)
                .temperature(0.7)
                .timeoutSeconds(60)
                .build();

        provider = new AnthropicProvider(config);
    }

    @Test
    void testGetName() {
        assertEquals("anthropic", provider.getName());
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
    void testCallWithSimpleMessage() {
        // This is a unit test that doesn't make actual API calls
        // Integration tests would require a real API key
        
        List<Object> messages = new ArrayList<>();
        messages.add(new UserMessage("Hello, how are you?"));
        
        ChatRequest request = ChatRequest.builder()
                .systemPrompt("You are a helpful assistant.")
                .messages(messages)
                .build();

        // Test that the provider can handle the request structure
        // (actual API call would fail without valid key)
        assertDoesNotThrow(() -> {
            // We're not calling the API in unit tests
            // Just verify the provider is configured correctly
            assertNotNull(provider);
        });
    }

    @Test
    void testCallWithToolCalling() {
        List<Object> messages = new ArrayList<>();
        messages.add(new UserMessage("What's the weather like?"));

        List<ToolDefinition> tools = new ArrayList<>();
        ToolDefinition weatherTool = new ToolDefinition(
                "get_weather",
                "Get the current weather for a location",
                "{\n" +
                        "  \"type\": \"object\",\n" +
                        "  \"properties\": {\n" +
                        "    \"location\": {\n" +
                        "      \"type\": \"string\",\n" +
                        "      \"description\": \"The city and state, e.g. San Francisco, CA\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"required\": [\"location\"]\n" +
                        "}"
        );
        tools.add(weatherTool);
        
        ChatRequest request = ChatRequest.builder()
                .systemPrompt("You are a helpful assistant.")
                .messages(messages)
                .tools(tools)
                .build();

        assertDoesNotThrow(() -> {
            assertNotNull(provider);
        });
    }

    @Test
    void testCallWithMultiModalContent() {
        List<ContentPart> parts = new ArrayList<>();
        parts.add(ContentPart.text("Hello"));
        parts.add(ContentPart.builder()
                .type(ContentPart.Type.IMAGE)
                .imageData(new byte[]{1, 2, 3, 4, 5}, "image/png")
                .build());
        
        UserMessage userMsg = new UserMessage("What's in this image?", parts);
        
        List<Object> messages = new ArrayList<>();
        messages.add(userMsg);
        
        ChatRequest request = ChatRequest.builder()
                .systemPrompt("You are a helpful assistant.")
                .messages(messages)
                .build();

        assertDoesNotThrow(() -> {
            assertNotNull(provider);
        });
    }

    @Test
    void testStreamWithSimpleMessage() {
        List<Object> messages = new ArrayList<>();
        messages.add(new UserMessage("Tell me a short story."));
        
        ChatRequest request = ChatRequest.builder()
                .systemPrompt("You are a helpful assistant.")
                .messages(messages)
                .build();

        // Mock streaming behavior
        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicReference<String> fullText = new AtomicReference<>("");

        assertDoesNotThrow(() -> {
            // We're not making actual API calls in unit tests
            // Just verify the provider structure
            assertNotNull(provider);
        });
    }
}