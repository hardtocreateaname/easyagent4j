package com.easyagent4j.core.memory;

/**
 * 记忆类型。
 */
public enum MemoryType {
    LONG_TERM,      // 长期记忆（持久化）
    PREFERENCE,     // 用户偏好
    PERSONALITY,    // Agent性格
    EPISODIC        // 情节记忆（对话片段）
}