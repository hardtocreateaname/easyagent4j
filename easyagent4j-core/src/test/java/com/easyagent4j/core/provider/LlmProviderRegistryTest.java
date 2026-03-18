package com.easyagent4j.core.provider;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmProviderRegistry的单元测试。
 */
class LlmProviderRegistryTest {

    private LlmProviderRegistry registry;
    private MockLlmProvider mockProvider1;
    private MockLlmProvider mockProvider2;

    @BeforeEach
    void setUp() {
        registry = new LlmProviderRegistry();
        mockProvider1 = new MockLlmProvider("openai");
        mockProvider2 = new MockLlmProvider("anthropic");
    }

    @Test
    void register_shouldAddProvider() {
        registry.register(mockProvider1);

        assertEquals(1, registry.size());
        assertTrue(registry.contains("openai"));
        assertNotNull(registry.get("openai"));
    }

    @Test
    void register_multipleProviders_shouldAllBeRegistered() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);

        assertEquals(2, registry.size());
        assertTrue(registry.contains("openai"));
        assertTrue(registry.contains("anthropic"));
    }

    @Test
    void register_nullProvider_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.register(null)
        );
        assertEquals("Provider cannot be null", exception.getMessage());
    }

    @Test
    void register_providerWithNullName_shouldThrowException() {
        MockLlmProvider provider = new MockLlmProvider(null);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.register(provider)
        );
        assertEquals("Provider name cannot be null or empty", exception.getMessage());
    }

    @Test
    void register_providerWithEmptyName_shouldThrowException() {
        MockLlmProvider provider = new MockLlmProvider("   ");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.register(provider)
        );
        assertEquals("Provider name cannot be null or empty", exception.getMessage());
    }

    @Test
    void get_existingProvider_shouldReturnProvider() {
        registry.register(mockProvider1);

        LlmProvider provider = registry.get("openai");

        assertNotNull(provider);
        assertEquals("openai", provider.getName());
    }

    @Test
    void get_nonExistingProvider_shouldReturnNull() {
        LlmProvider provider = registry.get("nonexistent");

        assertNull(provider);
    }

    @Test
    void get_nullName_shouldReturnNull() {
        LlmProvider provider = registry.get(null);

        assertNull(provider);
    }

    @Test
    void getDefault_noDefaultSet_shouldReturnNull() {
        registry.register(mockProvider1);

        LlmProvider provider = registry.getDefault();

        assertNull(provider);
    }

    @Test
    void getDefault_defaultSet_shouldReturnDefaultProvider() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);
        registry.setDefault("openai");

        LlmProvider provider = registry.getDefault();

        assertNotNull(provider);
        assertEquals("openai", provider.getName());
    }

    @Test
    void setDefault_existingProvider_shouldSetDefault() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);

        registry.setDefault("anthropic");

        LlmProvider provider = registry.getDefault();
        assertNotNull(provider);
        assertEquals("anthropic", provider.getName());
    }

    @Test
    void setDefault_nonExistingProvider_shouldThrowException() {
        registry.register(mockProvider1);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.setDefault("nonexistent")
        );
        assertTrue(exception.getMessage().contains("Provider 'nonexistent' not found"));
    }

    @Test
    void setDefault_nullName_shouldThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.setDefault(null)
        );
        assertEquals("Provider name cannot be null", exception.getMessage());
    }

    @Test
    void getAll_shouldReturnAllProviders() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);

        var allProviders = registry.getAll();

        assertEquals(2, allProviders.size());
        assertTrue(allProviders.contains(mockProvider1));
        assertTrue(allProviders.contains(mockProvider2));
    }

    @Test
    void getAllNames_shouldReturnAllProviderNames() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);

        var allNames = registry.getAllNames();

        assertEquals(2, allNames.size());
        assertTrue(allNames.contains("openai"));
        assertTrue(allNames.contains("anthropic"));
    }

    @Test
    void contains_existingProvider_shouldReturnTrue() {
        registry.register(mockProvider1);

        assertTrue(registry.contains("openai"));
    }

    @Test
    void contains_nonExistingProvider_shouldReturnFalse() {
        assertFalse(registry.contains("nonexistent"));
    }

    @Test
    void contains_nullName_shouldReturnFalse() {
        assertFalse(registry.contains(null));
    }

    @Test
    void size_emptyRegistry_shouldReturnZero() {
        assertEquals(0, registry.size());
    }

    @Test
    void size_withProviders_shouldReturnCorrectSize() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);

        assertEquals(2, registry.size());
    }

    @Test
    void clear_shouldRemoveAllProviders() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);
        registry.setDefault("openai");

        registry.clear();

        assertEquals(0, registry.size());
        assertNull(registry.getDefault());
        assertFalse(registry.contains("openai"));
        assertFalse(registry.contains("anthropic"));
    }

    @Test
    void switchDefault_multipleTimes_shouldWorkCorrectly() {
        registry.register(mockProvider1);
        registry.register(mockProvider2);

        registry.setDefault("openai");
        assertEquals("openai", registry.getDefault().getName());

        registry.setDefault("anthropic");
        assertEquals("anthropic", registry.getDefault().getName());

        registry.setDefault("openai");
        assertEquals("openai", registry.getDefault().getName());
    }

    /**
     * Mock LlmProvider用于测试。
     */
    private static class MockLlmProvider implements LlmProvider {
        private final String name;

        MockLlmProvider(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ChatResponse call(ChatRequest request) {
            return new ChatResponse("Mock response from " + name);
        }

        @Override
        public void stream(ChatRequest request, Consumer<ChatResponseChunk> callback) {
            callback.accept(new ChatResponseChunk("Mock chunk from " + name));
        }

        @Override
        public boolean supportsToolCalling() {
            return true;
        }

        @Override
        public boolean supportsStreaming() {
            return true;
        }

        @Override
        public boolean supportsVision() {
            return false;
        }
    }
}