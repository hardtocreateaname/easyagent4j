package com.easyagent4j.core.chat;

import com.easyagent4j.core.message.ToolCall;
import java.util.List;

/**
 * 流式响应块。
 */
public class ChatResponseChunk {
    private String text;
    private List<ToolCall> toolCalls;
    private boolean finished;

    public ChatResponseChunk() {}

    public ChatResponseChunk(String text) {
        this.text = text;
        this.finished = false;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
}
