package com.easyagent4j.core.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 记忆存储接口。
 */
public interface MemoryStore {
    // === 长期记忆 ===

    /**
     * 保存长期记忆。
     */
    void saveLongTerm(String key, String content);

    /**
     * 加载长期记忆。
     */
    Optional<String> loadLongTerm(String key);

    /**
     * 搜索长期记忆。
     */
    List<MemoryEntry> searchLongTerm(String query, int limit);

    /**
     * 列出所有长期记忆。
     */
    List<MemoryEntry> listLongTerm();

    /**
     * 删除长期记忆。
     */
    void deleteLongTerm(String key);

    /**
     * 整合短期记忆到长期记忆。
     */
    void consolidate();

    // === 用户偏好 ===

    /**
     * 设置用户偏好。
     */
    void setUserPreference(String key, String value);

    /**
     * 获取用户偏好。
     */
    Optional<String> getUserPreference(String key);

    /**
     * 获取所有用户偏好。
     */
    Map<String, String> getAllPreferences();
}