package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;

/**
 * AgentStartEvent — 事件类型: agent_start
 */
public class AgentStartEvent extends AgentEvent {
    public AgentStartEvent(com.easyagent4j.core.context.AgentContext ctx) { super("agent_start", ctx); }
}
