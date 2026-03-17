package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;

/**
 * AgentEndEvent — 事件类型: agent_end
 */
public class AgentEndEvent extends AgentEvent {
    public AgentEndEvent(com.easyagent4j.core.context.AgentContext ctx) { super("agent_end", ctx); }
}
