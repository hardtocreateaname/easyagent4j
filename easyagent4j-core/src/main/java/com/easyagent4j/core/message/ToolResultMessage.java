package com.easyagent4j.core.message;

import java.util.*;

/**
 * 工具执行结果消息。
 */
public class ToolResultMessage implements AgentMessage {
    private final ToolCall toolCall;
    private final String resultContent;
    private final boolean isError;
    private final long timestamp;
    private final Map<String, Object> metadata;

    public ToolResultMessage(ToolCall toolCall, String resultContent, boolean isError) {
        this.toolCall = toolCall;
        this.resultContent = resultContent;
        this.isError = isError;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    public static ToolResultMessage success(ToolCall call, String content) {
        return new ToolResultMessage(call, content, false);
    }

    public static ToolResultMessage error(ToolCall call, String errorMsg) {
        return new ToolResultMessage(call, errorMsg, true);
    }

    public static ToolResultMessage blocked(ToolCall call, String reason) {
        return new ToolResultMessage(call, "Blocked: " + reason, true);
    }

    @Override public MessageRole getRole() { return MessageRole.TOOL_RESULT; }
    @Override public Object getContent() { return resultContent; }
    @Override public long getTimestamp() { return timestamp; }
    @Override public Map<String, Object> getMetadata() { return metadata; }

    public ToolCall getToolCall() { return toolCall; }
    public String getResultContent() { return resultContent; }
    public boolean isError() { return isError; }
}
