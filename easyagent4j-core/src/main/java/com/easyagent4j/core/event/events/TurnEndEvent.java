package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.message.ToolResultMessage;

import java.util.List;

/**
 * TurnEndEvent — event type: turn_end
 */
public class TurnEndEvent extends AgentEvent {
    private final List<ToolResultMessage> toolResults;

    public TurnEndEvent(AgentContext ctx, List<ToolResultMessage> toolResults) {
        super("turn_end", ctx);
        this.toolResults = toolResults != null ? toolResults : List.of();
    }

    public List<ToolResultMessage> getToolResults() { return toolResults; }
}
