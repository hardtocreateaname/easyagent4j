package com.easyagent4j.core.context.transform;

import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.MessageRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Token预算变换器 — 基于简单token估算裁剪消息历史。
 *
 * 估算规则：
 * - 中文约 1.5 token/字
 * - 英文约 0.25 token/word（空格分词）
 *
 * 裁剪策略：
 * - 始终保留系统消息
 * - 从最早的消息开始裁剪
 * - 保留最后 minKeepMessages 条消息
 */
public class TokenBudgetTransformer implements MessageTransformer {
    private final int maxTokens;
    private final int minKeepMessages;

    /**
     * @param maxTokens        最大token预算
     * @param minKeepMessages  至少保留的最后N条非系统消息
     */
    public TokenBudgetTransformer(int maxTokens, int minKeepMessages) {
        if (maxTokens < 1) throw new IllegalArgumentException("maxTokens must be >= 1");
        if (minKeepMessages < 0) throw new IllegalArgumentException("minKeepMessages must be >= 0");
        this.maxTokens = maxTokens;
        this.minKeepMessages = minKeepMessages;
    }

    public TokenBudgetTransformer(int maxTokens) {
        this(maxTokens, 4);
    }

    @Override
    public List<AgentMessage> transform(List<AgentMessage> messages) {
        if (messages.isEmpty()) return messages;

        // 1. 分离系统消息和非系统消息
        List<AgentMessage> systemMessages = new ArrayList<>();
        List<AgentMessage> nonSystemMessages = new ArrayList<>();

        for (AgentMessage msg : messages) {
            if (msg.getRole() == MessageRole.SYSTEM) {
                systemMessages.add(msg);
            } else {
                nonSystemMessages.add(msg);
            }
        }

        // 2. 计算系统消息占用的token
        long systemTokens = 0;
        for (AgentMessage msg : systemMessages) {
            systemTokens += estimateTokens(msg);
        }

        long availableTokens = maxTokens - systemTokens;
        if (availableTokens <= 0) {
            return new ArrayList<>(systemMessages);
        }

        // 3. 如果全部非系统消息都在预算内，直接返回
        long totalNonSystemTokens = 0;
        for (AgentMessage msg : nonSystemMessages) {
            totalNonSystemTokens += estimateTokens(msg);
        }

        if (totalNonSystemTokens <= availableTokens) {
            return messages;
        }

        // 4. 需要裁剪：保留最后 minKeepMessages 条 + 从后往前填充预算
        int keepFromEnd = Math.min(minKeepMessages, nonSystemMessages.size());
        int removeFromStart = nonSystemMessages.size() - keepFromEnd;

        // 计算保留的尾部消息token
        long tailTokens = 0;
        for (int i = nonSystemMessages.size() - keepFromEnd; i < nonSystemMessages.size(); i++) {
            tailTokens += estimateTokens(nonSystemMessages.get(i));
        }

        // 从头部开始裁剪，直到预算足够
        List<AgentMessage> trimmedNonSystem = new ArrayList<>();
        long usedTokens = tailTokens;

        for (int i = removeFromStart - 1; i >= 0; i--) {
            long msgTokens = estimateTokens(nonSystemMessages.get(i));
            if (usedTokens + msgTokens > availableTokens) {
                break;
            }
            trimmedNonSystem.add(0, nonSystemMessages.get(i));
            usedTokens += msgTokens;
        }

        // 添加尾部保留的消息
        for (int i = nonSystemMessages.size() - keepFromEnd; i < nonSystemMessages.size(); i++) {
            trimmedNonSystem.add(nonSystemMessages.get(i));
        }

        // 5. 组合结果：系统消息 + 裁剪后的非系统消息
        List<AgentMessage> result = new ArrayList<>(systemMessages);
        result.addAll(trimmedNonSystem);
        return result;
    }

    /**
     * 简单估算消息的token数。
     */
    private long estimateTokens(AgentMessage message) {
        Object content = message.getContent();
        if (content == null) return 1;

        String text = content.toString();
        if (text.isEmpty()) return 1;

        long tokens = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isChinese(c)) {
                tokens += 1.5;
            } else if (c == ' ' || c == '\n' || c == '\t') {
                // 空格等分隔符：英文按word计算，分隔符本身不额外计费
            } else if (isBasicLatin(c)) {
                // 英文字符每个贡献约 0.25 / 平均单词约4字符 ≈ 0.0625
                // 但按word计算更简单，这里每个非空格latin字符算 0.06
                tokens += 0.06;
            } else {
                // 其他Unicode字符（日韩、符号等）按中文近似
                tokens += 1.5;
            }
        }

        return Math.max(1, tokens);
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private boolean isBasicLatin(char c) {
        return c >= ' ' && c <= '~';
    }
}
