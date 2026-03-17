package com.easyagent4j.core.memory;

import java.time.Instant;
import java.util.Map;

/**
 * 记忆条目。
 */
public record MemoryEntry(
    String key,
    String content,
    Instant createdAt,
    Instant updatedAt,
    Map<String, String> tags
) {
    public MemoryEntry(String key, String content) {
        this(key, content, Instant.now(), Instant.now(), Map.of());
    }

    public MemoryEntry(String key, String content, Map<String, String> tags) {
        this(key, content, Instant.now(), Instant.now(), tags);
    }
}