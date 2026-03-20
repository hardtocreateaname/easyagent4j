package com.easyagent4j.core.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话树，用于追踪主Agent和子Agent的分叉关系。
 */
public class AgentSessionTree {
    private final Map<String, AgentSessionNode> nodes = new ConcurrentHashMap<>();
    private final String rootSessionId;

    public AgentSessionTree(String rootSessionId) {
        this.rootSessionId = rootSessionId;
    }

    public AgentSessionNode createRoot(String sessionId) {
        AgentSessionNode root = new AgentSessionNode(sessionId, null, sessionId, 0, true);
        nodes.putIfAbsent(sessionId, root);
        return nodes.get(sessionId);
    }

    public AgentSessionNode createChild(String parentSessionId, String childSessionId, boolean inheritedHistory) {
        AgentSessionNode parent = nodes.get(parentSessionId);
        if (parent == null) {
            throw new IllegalArgumentException("Parent session not found: " + parentSessionId);
        }
        AgentSessionNode child = new AgentSessionNode(
            childSessionId,
            parentSessionId,
            parent.getRootSessionId(),
            parent.getDepth() + 1,
            inheritedHistory
        );
        AgentSessionNode existing = nodes.putIfAbsent(childSessionId, child);
        AgentSessionNode actual = existing != null ? existing : child;
        if (existing == null) {
            parent.addChild(childSessionId);
        }
        return actual;
    }

    public Optional<AgentSessionNode> getNode(String sessionId) {
        return Optional.ofNullable(nodes.get(sessionId));
    }

    public List<AgentSessionNode> getChildren(String sessionId) {
        AgentSessionNode node = nodes.get(sessionId);
        if (node == null) {
            return List.of();
        }
        List<AgentSessionNode> children = new ArrayList<>();
        for (String childId : node.getChildSessionIds()) {
            AgentSessionNode child = nodes.get(childId);
            if (child != null) {
                children.add(child);
            }
        }
        return List.copyOf(children);
    }

    public String getRootSessionId() {
        return rootSessionId;
    }
}
