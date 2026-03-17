package com.easyagent4j.core.message;

import java.util.*;

/**
 * 助手消息 — 可能包含文本回复和工具调用请求。
 */
public class AssistantMessage implements AgentMessage {
    private String textContent;
    private List<ToolCall> toolCalls;
    private final long timestamp;
    private final Map<String, Object> metadata;

    public AssistantMessage() {
        this.textContent = "";
        this.toolCalls = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    public AssistantMessage(String textContent) {
        this();
        this.textContent = textContent;
    }

    public AssistantMessage(String textContent, List<ToolCall> toolCalls) {
        this.textContent = textContent != null ? textContent : "";
        this.toolCalls = toolCalls != null ? new ArrayList<>(toolCalls) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    @Override public MessageRole getRole() { return MessageRole.ASSISTANT; }
    @Override public Object getContent() { return textContent; }
    @Override public long getTimestamp() { return timestamp; }
    @Override public Map<String, Object> getMetadata() { return metadata; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String text) { this.textContent = text; }
    public List<ToolCall> getToolCalls() { return Collections.unmodifiableList(toolCalls); }
    public void setToolCalls(List<ToolCall> calls) { this.toolCalls = new ArrayList<>(calls); }
    public void addToolCall(ToolCall call) { this.toolCalls.add(call); }

    public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
    public boolean isTextOnly() { return toolCalls == null || toolCalls.isEmpty(); }
}
