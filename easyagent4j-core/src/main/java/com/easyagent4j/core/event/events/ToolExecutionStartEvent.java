package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.message.ToolCall;

/**
 * ToolExecutionStartEvent — event type: tool_execution_start
 */
public class ToolExecutionStartEvent extends AgentEvent {
    private final ToolCall toolCall;

    public ToolExecutionStartEvent(AgentContext ctx, ToolCall toolCall) {
        super("tool_execution_start", ctx);
        this.toolCall = toolCall;
    }

    public ToolCall getToolCall() { return toolCall; }
}
