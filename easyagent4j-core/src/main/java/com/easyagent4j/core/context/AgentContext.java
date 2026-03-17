package com.easyagent4j.core.context;

import com.easyagent4j.core.message.AgentMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent上下文 — 维护消息历史和运行时状态。
 */
public class AgentContext {
    private final List<AgentMessage> messages;
    private final Map<String, Object> attributes;
    private final String sessionId;

    public AgentContext() {
        this(null);
    }

    public AgentContext(String sessionId) {
        this.messages = new ArrayList<>();
        this.attributes = new ConcurrentHashMap<>();
        this.sessionId = sessionId;
    }

    public void addMessage(AgentMessage message) {
        messages.add(message);
    }

    public List<AgentMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void setMessages(List<AgentMessage> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
    }

    public void clearMessages() {
        messages.clear();
    }

    public int getMessageCount() { return messages.size(); }

    public AgentMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public void setAttribute(String key, Object value) { attributes.put(key, value); }
    public Object getAttribute(String key) { return attributes.get(key); }
    public Map<String, Object> getAttributes() { return Collections.unmodifiableMap(attributes); }

    public String getSessionId() { return sessionId; }
}
