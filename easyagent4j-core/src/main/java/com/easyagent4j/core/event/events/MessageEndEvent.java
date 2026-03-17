package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.message.AgentMessage;

/**
 * MessageEndEvent — event type: message_end
 */
public class MessageEndEvent extends AgentEvent {
    private final AgentMessage message;

    public MessageEndEvent(AgentContext ctx, AgentMessage message) {
        super("message_end", ctx);
        this.message = message;
    }

    public AgentMessage getMessage() { return message; }
}
