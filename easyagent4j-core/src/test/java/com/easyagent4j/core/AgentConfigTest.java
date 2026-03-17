package com.easyagent4j.core;

import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.tool.ToolExecutionMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentConfig Builder模式的单元测试。
 */
class AgentConfigTest {

    @Test
    void defaultBuilder_shouldHaveDefaultValues() {
        AgentConfig config = AgentConfig.builder().build();

        assertEquals("You are a helpful assistant.", config.getSystemPrompt());
        assertEquals(10, config.getMaxToolIterations());
        assertEquals(ToolExecutionMode.PARALLEL, config.getToolExecutionMode());
        assertTrue(config.isStreamingEnabled());
        assertNull(config.getSessionId());
    }

    @Test
    void builder_shouldSetSystemPrompt() {
        AgentConfig config = AgentConfig.builder()
                .systemPrompt("Be concise")
                .build();

        assertEquals("Be concise", config.getSystemPrompt());
    }

    @Test
    void builder_shouldSetMaxToolIterations() {
        AgentConfig config = AgentConfig.builder()
                .maxToolIterations(5)
                .build();

        assertEquals(5, config.getMaxToolIterations());
    }

    @Test
    void builder_shouldClampMaxToolIterationsToMinimumOne() {
        AgentConfig config = AgentConfig.builder()
                .maxToolIterations(0)
                .build();

        assertEquals(1, config.getMaxToolIterations());
    }

    @Test
    void builder_shouldSetToolExecutionMode() {
        AgentConfig config = AgentConfig.builder()
                .toolExecutionMode(ToolExecutionMode.SEQUENTIAL)
                .build();

        assertEquals(ToolExecutionMode.SEQUENTIAL, config.getToolExecutionMode());
    }

    @Test
    void builder_shouldSetStreamingEnabled() {
        AgentConfig config = AgentConfig.builder()
                .streamingEnabled(false)
                .build();

        assertFalse(config.isStreamingEnabled());
    }

    @Test
    void builder_shouldSetSessionId() {
        AgentConfig config = AgentConfig.builder()
                .sessionId("session-123")
                .build();

        assertEquals("session-123", config.getSessionId());
    }

    @Test
    void builder_shouldSupportChaining() {
        AgentConfig config = AgentConfig.builder()
                .systemPrompt("Custom")
                .maxToolIterations(3)
                .toolExecutionMode(ToolExecutionMode.SEQUENTIAL)
                .streamingEnabled(false)
                .sessionId("abc")
                .build();

        assertEquals("Custom", config.getSystemPrompt());
        assertEquals(3, config.getMaxToolIterations());
        assertEquals(ToolExecutionMode.SEQUENTIAL, config.getToolExecutionMode());
        assertFalse(config.isStreamingEnabled());
        assertEquals("abc", config.getSessionId());
    }
}
