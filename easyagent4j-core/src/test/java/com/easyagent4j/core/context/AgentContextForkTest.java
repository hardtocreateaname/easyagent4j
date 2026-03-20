package com.easyagent4j.core.context;

import com.easyagent4j.core.message.UserMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentContextForkTest {

    @Test
    void forkShouldCopyMessagesAndMetadataIntoIsolatedChildContext() {
        AgentContext root = new AgentContext("root-session", null, "root-session", 0);
        root.addMessage(new UserMessage("hello"));
        root.setAttribute("mode", "root");

        AgentContext child = root.fork("child-session", true);
        child.addMessage(new UserMessage("child-only"));
        child.setAttribute("mode", "child");

        assertEquals("child-session", child.getSessionId());
        assertEquals("root-session", child.getParentSessionId());
        assertEquals("root-session", child.getRootSessionId());
        assertEquals(1, child.getDepth());
        assertEquals(1, root.getMessageCount());
        assertEquals(2, child.getMessageCount());
        assertEquals("root", root.getAttribute("mode"));
        assertEquals("child", child.getAttribute("mode"));
    }

    @Test
    void forkWithoutHistoryShouldCreateEmptyMessageList() {
        AgentContext root = new AgentContext("root-session", null, "root-session", 0);
        root.addMessage(new UserMessage("hello"));

        AgentContext child = root.fork("child-session", false);

        assertEquals(0, child.getMessageCount());
        assertEquals("root-session", child.getParentSessionId());
    }
}
