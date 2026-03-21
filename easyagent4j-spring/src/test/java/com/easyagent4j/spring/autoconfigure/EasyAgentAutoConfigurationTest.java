package com.easyagent4j.spring.autoconfigure;

import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.provider.LlmProviderRegistry;
import com.easyagent4j.spring.chat.SpringAiChatModelAdapter;
import com.easyagent4j.spring.properties.EasyAgentProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EasyAgentAutoConfigurationTest {

    private final EasyAgentAutoConfiguration autoConfiguration = new EasyAgentAutoConfiguration();

    @Test
    void easyAgentChatModel_shouldFailClearly_whenNoProviderAndNoSpringChatModelExist() {
        EasyAgentProperties props = new EasyAgentProperties();
        props.setChatProvider("openai");

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> autoConfiguration.easyAgentChatModel(new LlmProviderRegistry(), Map.of(), props)
        );

        assertTrue(ex.getMessage().contains("No chat model available for EasyAgent4j"));
        assertTrue(ex.getMessage().contains("Configured provider: 'openai'"));
    }

    @Test
    void easyAgentChatModel_shouldFallbackToFirstSpringChatModel_whenRequestedBeanIsMissing() {
        EasyAgentProperties props = new EasyAgentProperties();
        props.setChatProvider("openai");

        org.springframework.ai.chat.model.ChatModel springChatModel =
            mock(org.springframework.ai.chat.model.ChatModel.class);
        Map<String, org.springframework.ai.chat.model.ChatModel> chatModels = new LinkedHashMap<>();
        chatModels.put("customChatModel", springChatModel);

        ChatModel result = autoConfiguration.easyAgentChatModel(new LlmProviderRegistry(), chatModels, props);

        assertInstanceOf(SpringAiChatModelAdapter.class, result);
    }
}
