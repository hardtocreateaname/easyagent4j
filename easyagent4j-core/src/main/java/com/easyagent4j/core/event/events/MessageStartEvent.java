package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.message.AgentMessage;

/**
 * MessageStartEvent — event type: message_start
 */
public class MessageStartEvent extends AgentEvent {
    private final AgentMessage message;

    public MessageStartEvent(AgentContext ctx, AgentMessage message) {
        super("message_start", ctx);
        this.message = message;
    }

    public AgentMessage getMessage() { return message; }
}
