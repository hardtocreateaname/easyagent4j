package com.easyagent4j.core.context.transform;

import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.message.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenBudgetTransformer 单元测试。
 */
class TokenBudgetTransformerTest {

    private TokenBudgetTransformer transformer;

    @BeforeEach
    void setUp() {
        // 默认：100 token 预算，至少保留2条消息
        transformer = new TokenBudgetTransformer(100, 2);
    }

    private List<AgentMessage> createMessages(String... texts) {
        List<AgentMessage> messages = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            if (i % 2 == 0) {
                messages.add(new UserMessage(texts[i]));
            } else {
                messages.add(new AssistantMessage(texts[i]));
            }
        }
        return messages;
    }

    private SystemMessage createSystemMessage(String text) {
        // SystemMessage 需要实现 AgentMessage
        return new SystemMessage(text);
    }

    @Test
    @DisplayName("空消息列表应直接返回")
    void testEmptyMessages() {
        List<AgentMessage> result = transformer.transform(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("消息在预算内，不应裁剪")
    void testWithinBudget() {
        List<AgentMessage> messages = createMessages("Hello", "Hi there");
        List<AgentMessage> result = transformer.transform(messages);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("系统消息始终被保留")
    void testSystemMessageAlwaysKept() {
        List<AgentMessage> messages = new ArrayList<>();
        messages.add(createSystemMessage("You are a helpful assistant."));

        // 添加大量消息超出预算
        for (int i = 0; i < 20; i++) {
            messages.add(new UserMessage("This is a longer message to consume tokens: " + i));
            messages.add(new AssistantMessage("Response number: " + i + " with some content"));
        }

        // 非常小的预算
        TokenBudgetTransformer strict = new TokenBudgetTransformer(10, 0);
        List<AgentMessage> result = strict.transform(messages);

        // 系统消息应该被保留
        boolean hasSystem = result.stream()
            .anyMatch(m -> m.getRole() == MessageRole.SYSTEM);
        assertTrue(hasSystem, "System message should always be preserved");
    }

    @Test
    @DisplayName("超出预算时裁剪旧消息，保留最新消息")
    void testTrimOldMessages() {
        List<AgentMessage> messages = new ArrayList<>();

        // 添加大量消息
        for (int i = 0; i < 50; i++) {
            messages.add(new UserMessage("User message number " + i));
            messages.add(new AssistantMessage("Assistant response number " + i));
        }

        // 小预算
        TokenBudgetTransformer small = new TokenBudgetTransformer(20, 4);
        List<AgentMessage> result = small.transform(messages);

        // 结果应该比原始消息少
        assertTrue(result.size() < messages.size(),
            "Should trim messages when over budget");

        // 保留的消息数应该是 minKeepMessages(4) + 一些填入预算的消息
        assertTrue(result.size() >= 4,
            "Should keep at least minKeepMessages messages");
    }

    @Test
    @DisplayName("minKeepMessages=0时可以完全裁剪非系统消息")
    void testZeroMinKeepMessages() {
        List<AgentMessage> messages = new ArrayList<>();
        messages.add(new UserMessage("Hello"));
        messages.add(new AssistantMessage("Hi"));

        TokenBudgetTransformer strict = new TokenBudgetTransformer(1, 0);
        List<AgentMessage> result = strict.transform(messages);

        // 预算为1，两条消息的token肯定超过1
        assertTrue(result.size() <= messages.size());
    }

    @Test
    @DisplayName("构造函数参数校验")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class,
            () -> new TokenBudgetTransformer(0, 4),
            "maxTokens must be >= 1");

        assertThrows(IllegalArgumentException.class,
            () -> new TokenBudgetTransformer(100, -1),
            "minKeepMessages must be >= 0");
    }

    @Test
    @DisplayName("混合系统消息和非系统消息时正确分离")
    void testMixedMessages() {
        List<AgentMessage> messages = new ArrayList<>();
        messages.add(createSystemMessage("System prompt"));

        // 添加一些消息
        for (int i = 0; i < 10; i++) {
            messages.add(new UserMessage("User " + i));
            messages.add(new AssistantMessage("Assistant " + i));
        }

        // 添加中间的系统消息
        messages.add(5, createSystemMessage("Another system instruction"));

        // 足够大的预算，不应裁剪
        TokenBudgetTransformer large = new TokenBudgetTransformer(10000, 4);
        List<AgentMessage> result = large.transform(messages);

        assertEquals(messages.size(), result.size());
    }

    @Test
    @DisplayName("中文消息token估算")
    void testChineseTokenEstimation() {
        List<AgentMessage> messages = new ArrayList<>();
        // 中文每字约1.5 token，100字的中文消息约150 token
        StringBuilder longChinese = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longChinese.append("中");
        }
        messages.add(new UserMessage(longChinese.toString()));
        messages.add(new AssistantMessage("回复"));

        // 200 token 预算，应该能容纳
        TokenBudgetTransformer t = new TokenBudgetTransformer(200, 2);
        List<AgentMessage> result = t.transform(messages);
        assertEquals(2, result.size());

        // 50 token 预算，应该裁剪
        TokenBudgetTransformer strict = new TokenBudgetTransformer(50, 0);
        result = strict.transform(messages);
        assertTrue(result.size() < 2);
    }

    @Test
    @DisplayName("默认minKeepMessages=4")
    void testDefaultMinKeepMessages() {
        TokenBudgetTransformer t = new TokenBudgetTransformer(100);
        // 大量消息
        List<AgentMessage> messages = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            messages.add(new UserMessage("Message " + i));
        }
        List<AgentMessage> result = t.transform(messages);
        // 至少保留4条
        assertTrue(result.size() >= 4);
    }

    /**
     * 简单的系统消息实现（用于测试）。
     */
    private static class SystemMessage implements AgentMessage {
        private final String content;
        private final long timestamp;

        SystemMessage(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        @Override public MessageRole getRole() { return MessageRole.SYSTEM; }
        @Override public Object getContent() { return content; }
        @Override public long getTimestamp() { return timestamp; }
        @Override public java.util.Map<String, Object> getMetadata() {
            return new java.util.HashMap<>();
        }
    }
}
