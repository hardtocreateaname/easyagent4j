package com.easyagent4j.core.message;

import java.util.*;

/**
 * 用户消息。
 */
public class UserMessage implements AgentMessage {
    private final String content;
    private final List<ContentPart> parts;
    private final long timestamp;
    private final Map<String, Object> metadata;

    public UserMessage(String content) {
        this(content, Collections.emptyList());
    }

    public UserMessage(String content, List<ContentPart> parts) {
        this.content = content;
        this.parts = parts != null ? new ArrayList<>(parts) : Collections.emptyList();
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    public UserMessage(String content, List<ContentPart> parts, long timestamp, Map<String, Object> metadata) {
        this.content = content;
        this.parts = parts != null ? new ArrayList<>(parts) : Collections.emptyList();
        this.timestamp = timestamp;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    @Override public MessageRole getRole() { return MessageRole.USER; }
    @Override public Object getContent() { return content; }
    @Override public long getTimestamp() { return timestamp; }
    @Override public Map<String, Object> getMetadata() { return metadata; }

    public String getTextContent() { return content; }
    public List<ContentPart> getParts() { return Collections.unmodifiableList(parts); }
}
