package com.easyagent4j.core.memory;

/**
 * 支持按session分叉的记忆存储。
 */
public interface ForkableMemoryStore extends MemoryStore {

    /**
     * 基于同一底层存储配置，为新session创建独立的MemoryStore。
     */
    MemoryStore fork(String sessionId);

    /**
     * 当前MemoryStore绑定的sessionId。
     */
    String getSessionId();
}
