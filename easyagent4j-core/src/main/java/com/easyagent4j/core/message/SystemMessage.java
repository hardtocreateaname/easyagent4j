package com.easyagent4j.core.message;

import java.util.*;

/**
 * 系统消息。
 */
public class SystemMessage implements AgentMessage {
    private final String content;
    private final long timestamp;
    private final Map<String, Object> metadata;

    public SystemMessage(String content) {
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    @Override public MessageRole getRole() { return MessageRole.SYSTEM; }
    @Override public Object getContent() { return content; }
    @Override public long getTimestamp() { return timestamp; }
    @Override public Map<String, Object> getMetadata() { return metadata; }

    public String getTextContent() { return content; }
}
