package com.easyagent4j.core.agent;

import com.easyagent4j.core.MockChatModel;
import com.easyagent4j.core.message.UserMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentSessionTreeTest {

    @Test
    void spawnSubAgentShouldInheritSnapshotButKeepContextsIsolated() {
        AgentConfig config = AgentConfig.builder()
            .sessionId("root")
            .streamingEnabled(false)
            .build();

        Agent root = new Agent(config, new MockChatModel());
        root.getContext().addMessage(new UserMessage("root-msg"));

        Agent child = root.spawnSubAgent("root/child-1", true);
        child.getContext().addMessage(new UserMessage("child-msg"));

        assertEquals("root", root.getContext().getSessionId());
        assertEquals("root/child-1", child.getContext().getSessionId());
        assertEquals("root", child.getContext().getParentSessionId());
        assertEquals("root", child.getContext().getRootSessionId());
        assertEquals(1, child.getContext().getDepth());
        assertEquals(1, root.getMessages().size());
        assertEquals(2, child.getMessages().size());

        AgentSessionNode rootNode = root.getSessionTree().getNode("root").orElseThrow();
        AgentSessionNode childNode = root.getSessionTree().getNode("root/child-1").orElseThrow();
        assertEquals(1, rootNode.getChildSessionIds().size());
        assertEquals("root", childNode.getParentSessionId());
        assertTrue(childNode.isInheritedHistory());
    }

    @Test
    void spawnSubAgentWithoutHistoryShouldStartWithEmptyMessages() {
        AgentConfig config = AgentConfig.builder()
            .sessionId("root")
            .streamingEnabled(false)
            .build();

        Agent root = new Agent(config, new MockChatModel());
        root.getContext().addMessage(new UserMessage("root-msg"));

        Agent child = root.spawnSubAgent("root/child-2", false);

        assertEquals(0, child.getMessages().size());
        assertEquals("root", child.getContext().getParentSessionId());
    }
}
