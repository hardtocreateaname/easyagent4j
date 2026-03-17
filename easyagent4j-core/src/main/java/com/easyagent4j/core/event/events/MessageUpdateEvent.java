package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;

/**
 * MessageUpdateEvent — event type: message_update
 */
public class MessageUpdateEvent extends AgentEvent {
    private final String delta;

    public MessageUpdateEvent(AgentContext ctx, String delta) {
        super("message_update", ctx);
        this.delta = delta;
    }

    public String getDelta() { return delta; }
}
