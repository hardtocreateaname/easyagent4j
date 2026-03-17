package com.easyagent4j.core.event.events;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.AgentEvent;

/**
 * TurnStartEvent — 事件类型: turn_start
 */
public class TurnStartEvent extends AgentEvent {
    public TurnStartEvent(com.easyagent4j.core.context.AgentContext ctx) { super("turn_start", ctx); }
}
