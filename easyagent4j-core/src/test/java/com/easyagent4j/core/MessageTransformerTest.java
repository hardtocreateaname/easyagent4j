package com.easyagent4j.core;

import com.easyagent4j.core.context.transform.SlidingWindowTransformer;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.AssistantMessage;
import com.easyagent4j.core.message.MessageRole;
import com.easyagent4j.core.message.SystemMessage;
import com.easyagent4j.core.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlidingWindowTransformer的单元测试。
 */
class MessageTransformerTest {

    @Test
    void slidingWindow_shouldReturnSameListWhenUnderLimit() {
        SlidingWindowTransformer transformer = new SlidingWindowTransformer(5);
        List<AgentMessage> messages = List.of(
            new UserMessage("msg1"),
            new AssistantMessage("reply1")
        );

        List<AgentMessage> result = transformer.transform(messages);

        assertSame(messages, result);
        assertEquals(2, result.size());
    }

    @Test
    void slidingWindow_shouldReturnSameListWhenExactlyAtLimit() {
        SlidingWindowTransformer transformer = new SlidingWindowTransformer(3);
        List<AgentMessage> messages = List.of(
            new UserMessage("a"),
            new AssistantMessage("b"),
            new UserMessage("c")
        );

        List<AgentMessage> result = transformer.transform(messages);

        assertSame(messages, result);
        assertEquals(3, result.size());
    }

    @Test
    void slidingWindow_shouldTrimWhenOverLimit() {
        SlidingWindowTransformer transformer = new SlidingWindowTransformer(2);
        List<AgentMessage> messages = List.of(
            new UserMessage("old1"),
            new AssistantMessage("old2"),
            new UserMessage("keep1"),
            new AssistantMessage("keep2")
        );

        List<AgentMessage> result = transformer.transform(messages);

        assertEquals(2, result.size());
        assertEquals("keep1", ((UserMessage) result.get(0)).getTextContent());
        assertEquals("keep2", ((AssistantMessage) result.get(1)).getTextContent());
    }

    @Test
    void slidingWindow_shouldAlwaysKeepSystemMessages() {
        SlidingWindowTransformer transformer = new SlidingWindowTransformer(3);
        List<AgentMessage> messages = List.of(
            new SystemMessage("system prompt"),
            new UserMessage("q1"),
            new AssistantMessage("a1"),
            new UserMessage("q2"),
            new AssistantMessage("a2")
        );

        List<AgentMessage> result = transformer.transform(messages);

        // system message (always kept) + 2 most recent (3 - 1 system = 2 remaining)
        assertEquals(3, result.size());
        assertEquals(MessageRole.SYSTEM, result.get(0).getRole());
        assertEquals("system prompt", ((SystemMessage) result.get(0)).getTextContent());
        assertEquals("q2", ((UserMessage) result.get(1)).getTextContent());
        assertEquals("a2", ((AssistantMessage) result.get(2)).getTextContent());
    }

    @Test
    void slidingWindow_shouldReturnOnlySystemMessagesWhenMaxIsOne() {
        SlidingWindowTransformer transformer = new SlidingWindowTransformer(1);
        List<AgentMessage> messages = List.of(
            new SystemMessage("sys"),
            new UserMessage("q1"),
            new AssistantMessage("a1")
        );

        List<AgentMessage> result = transformer.transform(messages);

        assertEquals(1, result.size());
        assertEquals(MessageRole.SYSTEM, result.get(0).getRole());
    }

    @Test
    void slidingWindow_shouldThrowOnZeroMaxMessages() {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowTransformer(0));
    }

    @Test
    void slidingWindow_shouldThrowOnNegativeMaxMessages() {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowTransformer(-1));
    }

    @Test
    void slidingWindow_shouldHandleEmptyList() {
        SlidingWindowTransformer transformer = new SlidingWindowTransformer(3);
        List<AgentMessage> messages = new ArrayList<>();

        List<AgentMessage> result = transformer.transform(messages);

        assertTrue(result.isEmpty());
    }
}
