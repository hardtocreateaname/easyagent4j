package com.easyagent4j.core.agent;

import com.easyagent4j.core.MockChatModel;
import com.easyagent4j.core.memory.FileMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SubAgentMemoryForkTest {

    @TempDir
    Path tempDir;

    @Test
    void spawnSubAgentShouldForkMemoryStoreBySessionId() {
        Agent root = new Agent(AgentConfig.builder().sessionId("root").build(), new MockChatModel());
        root.setMemoryStore(new FileMemoryStore(tempDir.toString(), "root"));

        Agent child = root.spawnSubAgent("root/child-1", true);

        assertNotNull(child.getMemory());
        assertInstanceOf(FileMemoryStore.class, child.getMemory());
        FileMemoryStore childMemory = (FileMemoryStore) child.getMemory();
        assertEquals("root/child-1", childMemory.getSessionId());
    }
}
