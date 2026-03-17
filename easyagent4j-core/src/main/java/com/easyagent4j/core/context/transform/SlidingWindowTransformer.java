package com.easyagent4j.core.context.transform;

import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.MessageRole;

import java.util.ArrayList;
import java.util.List;

/**
 * 滑动窗口变换器 — 保留最近N条消息。
 */
public class SlidingWindowTransformer implements MessageTransformer {
    private final int maxMessages;

    public SlidingWindowTransformer(int maxMessages) {
        if (maxMessages < 1) throw new IllegalArgumentException("maxMessages must be >= 1");
        this.maxMessages = maxMessages;
    }

    @Override
    public List<AgentMessage> transform(List<AgentMessage> messages) {
        if (messages.size() <= maxMessages) return messages;

        List<AgentMessage> result = new ArrayList<>();
        for (AgentMessage msg : messages) {
            if (msg.getRole() == MessageRole.SYSTEM) {
                result.add(msg);
            }
        }

        int remaining = maxMessages - result.size();
        if (remaining <= 0) return result;

        int start = Math.max(0, messages.size() - remaining);
        result.addAll(messages.subList(start, messages.size()));
        return result;
    }
}
