package com.easyagent4j.core.memory;

import java.util.List;
import java.util.Map;

/**
 * 记忆信息注入到System Prompt的构建器。
 */
public class MemoryPromptBuilder {

    /**
     * 将记忆信息注入到system prompt中。
     *
     * @param basePrompt 基础system prompt
     * @param memoryStore 记忆存储
     * @return 增强后的system prompt
     */
    public String buildEnhancedSystemPrompt(String basePrompt, MemoryStore memoryStore) {
        if (basePrompt == null) {
            return null;
        }

        StringBuilder enhanced = new StringBuilder(basePrompt);
        if (!basePrompt.isEmpty()) {
            enhanced.append("\n\n");
        }
        enhanced.append("## Memory\n");

        // 用户偏好
        Map<String, String> preferences = memoryStore.getAllPreferences();
        if (!preferences.isEmpty()) {
            enhanced.append("### 用户偏好\n");
            preferences.forEach((key, value) ->
                enhanced.append("- ").append(key).append(": ").append(value).append("\n")
            );
            enhanced.append("\n");
        }

        // 长期记忆摘要（取最近5条）
        List<MemoryEntry> memories = memoryStore.listLongTerm();
        if (!memories.isEmpty()) {
            enhanced.append("### 长期记忆\n");
            int limit = Math.min(5, memories.size());
            for (int i = 0; i < limit; i++) {
                MemoryEntry entry = memories.get(i);
                enhanced.append("- ").append(entry.key()).append(": ").append(entry.content()).append("\n");
            }
            enhanced.append("\n");
        }

        return enhanced.toString();
    }
}