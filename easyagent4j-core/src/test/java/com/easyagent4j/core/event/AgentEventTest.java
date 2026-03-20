package com.easyagent4j.core.event;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.event.events.AgentStartEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentEventTest {

    @Test
    void eventShouldExposeSessionTreeMetadata() {
        AgentContext context = new AgentContext("child", "parent", "root", 1);
        AgentEvent event = new AgentStartEvent(context);

        assertEquals("child", event.getSessionId());
        assertEquals("parent", event.getParentSessionId());
        assertEquals("root", event.getRootSessionId());
    }
}
