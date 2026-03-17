package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.message.ToolCall;

/**
 * ToolExecutionEndEvent — event type: tool_execution_end
 */
public class ToolExecutionEndEvent extends AgentEvent {
    private final ToolCall toolCall;
    private final boolean isError;

    public ToolExecutionEndEvent(AgentContext ctx, ToolCall toolCall, boolean isError) {
        super("tool_execution_end", ctx);
        this.toolCall = toolCall;
        this.isError = isError;
    }

    public ToolCall getToolCall() { return toolCall; }
    public boolean isError() { return isError; }
}
