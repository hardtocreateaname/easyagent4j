package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.message.ToolCall;

/**
 * ToolExecutionUpdateEvent — event type: tool_execution_update
 */
public class ToolExecutionUpdateEvent extends AgentEvent {
    private final String progress;
    private final ToolCall toolCall;

    public ToolExecutionUpdateEvent(AgentContext ctx, ToolCall toolCall, String progress) {
        super("tool_execution_update", ctx);
        this.toolCall = toolCall;
        this.progress = progress;
    }

    public ToolCall getToolCall() { return toolCall; }
    public String getProgress() { return progress; }
}
