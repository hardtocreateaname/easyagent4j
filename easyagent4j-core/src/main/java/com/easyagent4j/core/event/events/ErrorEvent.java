package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;

/**
 * ErrorEvent — event type: error
 */
public class ErrorEvent extends AgentEvent {
    private final String errorMessage;

    public ErrorEvent(AgentContext ctx, String errorMessage) {
        super("error", ctx);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() { return errorMessage; }
}
