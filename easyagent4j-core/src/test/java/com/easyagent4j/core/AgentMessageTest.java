package com.easyagent4j.core.message;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentMessage相关消息类的单元测试。
 */
class AgentMessageTest {

    // === UserMessage ===

    @Test
    void userMessage_shouldHaveUserRole() {
        UserMessage msg = new UserMessage("hello");
        assertEquals(MessageRole.USER, msg.getRole());
    }

    @Test
    void userMessage_shouldReturnContent() {
        UserMessage msg = new UserMessage("hello world");
        assertEquals("hello world", msg.getContent());
        assertEquals("hello world", msg.getTextContent());
    }

    @Test
    void userMessage_shouldHaveTimestamp() {
        long before = System.currentTimeMillis();
        UserMessage msg = new UserMessage("test");
        long after = System.currentTimeMillis();
        assertTrue(msg.getTimestamp() >= before);
        assertTrue(msg.getTimestamp() <= after);
    }

    @Test
    void userMessage_shouldInitializeEmptyMetadata() {
        UserMessage msg = new UserMessage("test");
        assertNotNull(msg.getMetadata());
        assertTrue(msg.getMetadata().isEmpty());
    }

    @Test
    void userMessage_shouldReturnEmptyPartsWhenNoParts() {
        UserMessage msg = new UserMessage("test");
        assertNotNull(msg.getParts());
        assertTrue(msg.getParts().isEmpty());
    }

    @Test
    void userMessage_shouldAcceptCustomParts() {
        UserMessage msg = new UserMessage("test", Collections.emptyList());
        assertTrue(msg.getParts().isEmpty());
    }

    @Test
    void userMessage_shouldSupportFullConstructor() {
        long ts = 1000L;
        Map<String, Object> meta = Map.of("key", "value");
        UserMessage msg = new UserMessage("full", Collections.emptyList(), ts, meta);

        assertEquals("full", msg.getTextContent());
        assertEquals(ts, msg.getTimestamp());
        assertEquals("value", msg.getMetadata().get("key"));
    }

    // === AssistantMessage ===

    @Test
    void assistantMessage_shouldHaveAssistantRole() {
        AssistantMessage msg = new AssistantMessage("hi");
        assertEquals(MessageRole.ASSISTANT, msg.getRole());
    }

    @Test
    void assistantMessage_shouldReturnTextContent() {
        AssistantMessage msg = new AssistantMessage("response");
        assertEquals("response", msg.getContent());
        assertEquals("response", msg.getTextContent());
    }

    @Test
    void assistantMessage_defaultConstructor_shouldReturnEmptyText() {
        AssistantMessage msg = new AssistantMessage();
        assertEquals("", msg.getTextContent());
        assertFalse(msg.hasToolCalls());
        assertTrue(msg.isTextOnly());
    }

    @Test
    void assistantMessage_shouldHaveNoToolCallsByDefault() {
        AssistantMessage msg = new AssistantMessage("text only");
        assertFalse(msg.hasToolCalls());
        assertTrue(msg.isTextOnly());
        assertTrue(msg.getToolCalls().isEmpty());
    }

    @Test
    void assistantMessage_shouldSupportToolCalls() {
        ToolCall call = new ToolCall("call_1", "my_tool", "{\"arg\":\"val\"}");
        AssistantMessage msg = new AssistantMessage("let me check", List.of(call));

        assertTrue(msg.hasToolCalls());
        assertFalse(msg.isTextOnly());
        assertEquals(1, msg.getToolCalls().size());
        assertEquals("my_tool", msg.getToolCalls().get(0).getName());
    }

    @Test
    void assistantMessage_shouldSupportAddToolCall() {
        AssistantMessage msg = new AssistantMessage();
        msg.addToolCall(new ToolCall("call_1", "tool_a", "{}"));
        msg.addToolCall(new ToolCall("call_2", "tool_b", "{}"));

        assertEquals(2, msg.getToolCalls().size());
        assertTrue(msg.hasToolCalls());
    }

    @Test
    void assistantMessage_shouldSupportSetTextContent() {
        AssistantMessage msg = new AssistantMessage();
        msg.setTextContent("updated");
        assertEquals("updated", msg.getTextContent());
    }

    @Test
    void assistantMessage_shouldHaveTimestamp() {
        long before = System.currentTimeMillis();
        AssistantMessage msg = new AssistantMessage("test");
        long after = System.currentTimeMillis();
        assertTrue(msg.getTimestamp() >= before);
        assertTrue(msg.getTimestamp() <= after);
    }

    // === SystemMessage ===

    @Test
    void systemMessage_shouldHaveSystemRole() {
        SystemMessage msg = new SystemMessage("system");
        assertEquals(MessageRole.SYSTEM, msg.getRole());
    }

    @Test
    void systemMessage_shouldReturnContent() {
        SystemMessage msg = new SystemMessage("You are helpful");
        assertEquals("You are helpful", msg.getContent());
        assertEquals("You are helpful", msg.getTextContent());
    }

    @Test
    void systemMessage_shouldHaveTimestamp() {
        long before = System.currentTimeMillis();
        SystemMessage msg = new SystemMessage("test");
        long after = System.currentTimeMillis();
        assertTrue(msg.getTimestamp() >= before);
        assertTrue(msg.getTimestamp() <= after);
    }

    @Test
    void systemMessage_shouldInitializeEmptyMetadata() {
        SystemMessage msg = new SystemMessage("test");
        assertNotNull(msg.getMetadata());
        assertTrue(msg.getMetadata().isEmpty());
    }

    // === ToolResultMessage ===

    @Test
    void toolResultMessage_shouldHaveToolResultRole() {
        ToolCall call = new ToolCall("id", "tool", "{}");
        ToolResultMessage msg = ToolResultMessage.success(call, "ok");
        assertEquals(MessageRole.TOOL_RESULT, msg.getRole());
    }

    @Test
    void toolResultMessage_success_shouldNotBeError() {
        ToolCall call = new ToolCall("id", "tool", "{}");
        ToolResultMessage msg = ToolResultMessage.success(call, "result");
        assertFalse(msg.isError());
        assertEquals("result", msg.getResultContent());
        assertEquals("result", msg.getContent());
        assertSame(call, msg.getToolCall());
    }

    @Test
    void toolResultMessage_error_shouldBeError() {
        ToolCall call = new ToolCall("id", "tool", "{}");
        ToolResultMessage msg = ToolResultMessage.error(call, "failed");
        assertTrue(msg.isError());
        assertEquals("failed", msg.getResultContent());
    }

    @Test
    void toolResultMessage_blocked_shouldBeError() {
        ToolCall call = new ToolCall("id", "tool", "{}");
        ToolResultMessage msg = ToolResultMessage.blocked(call, "not allowed");
        assertTrue(msg.isError());
        assertTrue(msg.getResultContent().contains("Blocked"));
        assertTrue(msg.getResultContent().contains("not allowed"));
    }

    @Test
    void toolResultMessage_shouldHaveTimestamp() {
        ToolCall call = new ToolCall("id", "tool", "{}");
        long before = System.currentTimeMillis();
        ToolResultMessage msg = ToolResultMessage.success(call, "ok");
        long after = System.currentTimeMillis();
        assertTrue(msg.getTimestamp() >= before);
        assertTrue(msg.getTimestamp() <= after);
    }
}
