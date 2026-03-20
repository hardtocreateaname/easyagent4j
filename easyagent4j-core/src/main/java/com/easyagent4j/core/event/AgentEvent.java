package com.easyagent4j.core.event;

import com.easyagent4j.core.context.AgentContext;

/**
 * 事件基类。
 */
public class AgentEvent {
    private final String type;
    private final long timestamp;
    private final AgentContext context;
    private final String sessionId;
    private final String parentSessionId;
    private final String rootSessionId;

    protected AgentEvent(String type, AgentContext context) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.context = context;
        this.sessionId = context != null ? context.getSessionId() : null;
        this.parentSessionId = context != null ? context.getParentSessionId() : null;
        this.rootSessionId = context != null ? context.getRootSessionId() : null;
    }

    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public AgentContext getContext() { return context; }
    public String getSessionId() { return sessionId; }
    public String getParentSessionId() { return parentSessionId; }
    public String getRootSessionId() { return rootSessionId; }
}
