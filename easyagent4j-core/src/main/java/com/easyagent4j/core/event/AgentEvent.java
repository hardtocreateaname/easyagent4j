package com.easyagent4j.core.event;

import com.easyagent4j.core.context.AgentContext;

/**
 * 事件基类。
 */
public class AgentEvent {
    private final String type;
    private final long timestamp;
    private final AgentContext context;

    protected AgentEvent(String type, AgentContext context) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.context = context;
    }

    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public AgentContext getContext() { return context; }
}
