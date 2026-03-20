package com.easyagent4j.core.agent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 会话树节点。
 */
public class AgentSessionNode {
    private final String sessionId;
    private final String parentSessionId;
    private final String rootSessionId;
    private final int depth;
    private final long createdAt;
    private final boolean inheritedHistory;
    private final List<String> childSessionIds = new CopyOnWriteArrayList<>();

    public AgentSessionNode(String sessionId, String parentSessionId, String rootSessionId, int depth, boolean inheritedHistory) {
        this.sessionId = sessionId;
        this.parentSessionId = parentSessionId;
        this.rootSessionId = rootSessionId;
        this.depth = depth;
        this.inheritedHistory = inheritedHistory;
        this.createdAt = System.currentTimeMillis();
    }

    public String getSessionId() { return sessionId; }
    public String getParentSessionId() { return parentSessionId; }
    public String getRootSessionId() { return rootSessionId; }
    public int getDepth() { return depth; }
    public long getCreatedAt() { return createdAt; }
    public boolean isInheritedHistory() { return inheritedHistory; }
    public List<String> getChildSessionIds() { return List.copyOf(childSessionIds); }

    void addChild(String childSessionId) {
        childSessionIds.add(childSessionId);
    }
}
