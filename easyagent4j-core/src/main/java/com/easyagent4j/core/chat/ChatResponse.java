package com.easyagent4j.core.chat;

import com.easyagent4j.core.message.ToolCall;
import java.util.List;

/**
 * LLM响应模型。
 */
public class ChatResponse {
    private String text;
    private List<ToolCall> toolCalls;
    private Usage usage;

    public ChatResponse() {
        this.toolCalls = List.of();
    }

    public ChatResponse(String text) {
        this.text = text;
        this.toolCalls = List.of();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCall> calls) { this.toolCalls = calls; }
    public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }
}
