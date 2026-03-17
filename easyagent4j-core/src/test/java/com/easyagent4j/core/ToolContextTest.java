package com.easyagent4j.core;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.message.ToolCall;
import com.easyagent4j.core.tool.ToolContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolContext参数获取的单元测试。
 */
class ToolContextTest {

    private ToolCall toolCall;
    private AgentContext agentContext;
    private ToolContext toolContext;

    @BeforeEach
    void setUp() {
        toolCall = new ToolCall("call_1", "my_tool", "{\"city\":\"Beijing\",\"count\":5,\"enabled\":true}");
        agentContext = new AgentContext("test-session");
        toolContext = new ToolContext(toolCall, agentContext);
    }

    @Test
    void getArguments_shouldReturnParsedMap() {
        var args = toolContext.getArguments();
        assertEquals("Beijing", args.get("city"));
        assertEquals(5, args.get("count"));
        assertEquals(true, args.get("enabled"));
    }

    @Test
    void getStringArg_shouldReturnStringValue() {
        assertEquals("Beijing", toolContext.getStringArg("city"));
    }

    @Test
    void getStringArg_shouldReturnNullForMissingKey() {
        assertNull(toolContext.getStringArg("nonexistent"));
    }

    @Test
    void getIntArg_shouldReturnIntValue() {
        assertEquals(5, toolContext.getIntArg("count"));
    }

    @Test
    void getIntArg_shouldReturnNullForMissingKey() {
        assertNull(toolContext.getIntArg("nonexistent"));
    }

    @Test
    void getBoolArg_shouldReturnBooleanValue() {
        assertTrue(toolContext.getBoolArg("enabled"));
    }

    @Test
    void getBoolArg_shouldReturnNullForMissingKey() {
        assertNull(toolContext.getBoolArg("nonexistent"));
    }

    @Test
    void getArg_shouldReturnTypedValue() {
        String city = toolContext.getArg("city", String.class);
        assertEquals("Beijing", city);
    }

    @Test
    void getArg_shouldReturnNullForMissingKey() {
        assertNull(toolContext.getArg("nonexistent", String.class));
    }

    @Test
    void getToolCall_shouldReturnOriginalToolCall() {
        assertSame(toolCall, toolContext.getToolCall());
    }

    @Test
    void getAgentContext_shouldReturnOriginalContext() {
        assertSame(agentContext, toolContext.getAgentContext());
    }

    @Test
    void getRawArguments_shouldReturnRawJson() {
        assertEquals("{\"city\":\"Beijing\",\"count\":5,\"enabled\":true}", toolContext.getRawArguments());
    }

    @Test
    void constructor_shouldHandleEmptyArguments() {
        ToolCall emptyCall = new ToolCall("call_2", "tool", "");
        ToolContext ctx = new ToolContext(emptyCall, agentContext);

        assertTrue(ctx.getArguments().isEmpty());
        assertNull(ctx.getStringArg("anything"));
    }

    @Test
    void constructor_shouldHandleNullArguments() {
        ToolCall nullCall = new ToolCall("call_3", "tool", null);
        ToolContext ctx = new ToolContext(nullCall, agentContext);

        assertTrue(ctx.getArguments().isEmpty());
    }

    @Test
    void constructor_shouldThrowOnInvalidJson() {
        ToolCall badCall = new ToolCall("call_4", "tool", "not-json");
        assertThrows(RuntimeException.class, () -> new ToolContext(badCall, agentContext));
    }
}
