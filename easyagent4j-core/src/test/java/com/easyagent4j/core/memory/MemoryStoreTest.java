package com.easyagent4j.core.memory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryStore单元测试。
 */
class MemoryStoreTest {

    private static final String TEST_BASE_PATH = "/tmp/easyagent4j-test";
    private static final String TEST_SESSION_ID = "test-session";
    private FileMemoryStore memoryStore;

    @BeforeEach
    void setUp() {
        // 清理之前的测试数据
        try {
            Path testPath = Path.of(TEST_BASE_PATH, TEST_SESSION_ID);
            if (Files.exists(testPath)) {
                Files.walk(testPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
            }
        } catch (IOException e) {
            // ignore
        }
        memoryStore = new FileMemoryStore(TEST_BASE_PATH, TEST_SESSION_ID);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        try {
            Path testPath = Path.of(TEST_BASE_PATH, TEST_SESSION_ID);
            if (Files.exists(testPath)) {
                Files.walk(testPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
            }
        } catch (IOException e) {
            // ignore
        }
    }

    // === 长期记忆测试 ===

    @Test
    void saveLongTerm_shouldSaveMemory() {
        memoryStore.saveLongTerm("用户信息", "名字：张三，偏好：简洁回复");

        Optional<String> loaded = memoryStore.loadLongTerm("用户信息");
        assertTrue(loaded.isPresent());
        assertTrue(loaded.get().contains("张三"));
    }

    @Test
    void loadLongTerm_shouldReturnEmptyForNonExistentKey() {
        Optional<String> loaded = memoryStore.loadLongTerm("不存在的key");
        assertFalse(loaded.isPresent());
    }

    @Test
    void loadLongTerm_shouldReturnSavedContent() {
        memoryStore.saveLongTerm("测试key", "测试内容");
        Optional<String> loaded = memoryStore.loadLongTerm("测试key");
        assertTrue(loaded.isPresent());
        assertEquals("测试内容", loaded.get().trim());
    }

    @Test
    void searchLongTerm_shouldFindMatchingEntries() {
        memoryStore.saveLongTerm("用户信息", "名字：李四");
        memoryStore.saveLongTerm("项目信息", "EasyAgent4j项目");
        memoryStore.saveLongTerm("其他信息", "无关内容");

        List<MemoryEntry> results = memoryStore.searchLongTerm("项目", 10);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.key().contains("项目信息")));
    }

    @Test
    void searchLongTerm_shouldRespectLimit() {
        for (int i = 0; i < 10; i++) {
            memoryStore.saveLongTerm("条目" + i, "内容" + i);
        }

        List<MemoryEntry> results = memoryStore.searchLongTerm("条目", 5);
        assertEquals(5, results.size());
    }

    @Test
    void listLongTerm_shouldReturnAllEntries() {
        memoryStore.saveLongTerm("key1", "value1");
        memoryStore.saveLongTerm("key2", "value2");

        List<MemoryEntry> entries = memoryStore.listLongTerm();
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> e.key().equals("key1")));
        assertTrue(entries.stream().anyMatch(e -> e.key().equals("key2")));
    }

    @Test
    void listLongTerm_shouldReturnEmptyWhenNoEntries() {
        List<MemoryEntry> entries = memoryStore.listLongTerm();
        assertTrue(entries.isEmpty());
    }

    @Test
    void deleteLongTerm_shouldRemoveEntry() {
        memoryStore.saveLongTerm("toBeDeleted", "将要删除的内容");

        memoryStore.deleteLongTerm("toBeDeleted");

        Optional<String> loaded = memoryStore.loadLongTerm("toBeDeleted");
        assertFalse(loaded.isPresent());
    }

    @Test
    void consolidate_shouldDoNothing() {
        // consolidate方法目前未实现，只是记录日志
        assertDoesNotThrow(() -> memoryStore.consolidate());
    }

    // === 用户偏好测试 ===

    @Test
    void setUserPreference_shouldSavePreference() {
        memoryStore.setUserPreference("language", "zh-CN");

        Optional<String> loaded = memoryStore.getUserPreference("language");
        assertTrue(loaded.isPresent());
        assertEquals("zh-CN", loaded.get());
    }

    @Test
    void getUserPreference_shouldReturnEmptyForNonExistentKey() {
        Optional<String> loaded = memoryStore.getUserPreference("不存在的key");
        assertFalse(loaded.isPresent());
    }

    @Test
    void getAllPreferences_shouldReturnAllPreferences() {
        memoryStore.setUserPreference("key1", "value1");
        memoryStore.setUserPreference("key2", "value2");

        Map<String, String> prefs = memoryStore.getAllPreferences();
        assertEquals(2, prefs.size());
        assertEquals("value1", prefs.get("key1"));
        assertEquals("value2", prefs.get("key2"));
    }

    @Test
    void getAllPreferences_shouldReturnEmptyWhenNoPreferences() {
        Map<String, String> prefs = memoryStore.getAllPreferences();
        assertTrue(prefs.isEmpty());
    }

    @Test
    void setUserPreference_shouldUpdateExistingPreference() {
        memoryStore.setUserPreference("key", "oldValue");
        memoryStore.setUserPreference("key", "newValue");

        Optional<String> loaded = memoryStore.getUserPreference("key");
        assertTrue(loaded.isPresent());
        assertEquals("newValue", loaded.get());
    }

    // === MemoryEntry测试 ===

    @Test
    void memoryEntry_defaultConstructor_shouldSetTimestamps() {
        MemoryEntry entry = new MemoryEntry("key", "content");
        assertNotNull(entry.createdAt());
        assertNotNull(entry.updatedAt());
        assertTrue(entry.tags().isEmpty());
    }

    @Test
    void memoryEntry_withTags_shouldStoreTags() {
        Map<String, String> tags = Map.of("tag1", "value1", "tag2", "value2");
        MemoryEntry entry = new MemoryEntry("key", "content", tags);

        assertEquals(2, entry.tags().size());
        assertEquals("value1", entry.tags().get("tag1"));
    }

    @Test
    void memoryEntry_recordMethods_shouldWork() {
        MemoryEntry entry = new MemoryEntry("testKey", "testContent");
        assertEquals("testKey", entry.key());
        assertEquals("testContent", entry.content());
    }

    // === MemoryPromptBuilder测试 ===

    @Test
    void buildEnhancedSystemPrompt_shouldReturnBasePromptWhenNoMemory() {
        MemoryPromptBuilder builder = new MemoryPromptBuilder();
        String result = builder.buildEnhancedSystemPrompt("Base prompt", memoryStore);

        assertTrue(result.contains("Base prompt"));
        assertTrue(result.contains("## Memory"));
    }

    @Test
    void buildEnhancedSystemPrompt_shouldIncludePreferences() {
        memoryStore.setUserPreference("language", "zh-CN");
        memoryStore.setUserPreference("style", "concise");

        MemoryPromptBuilder builder = new MemoryPromptBuilder();
        String result = builder.buildEnhancedSystemPrompt("Base", memoryStore);

        assertTrue(result.contains("## Memory"));
        assertTrue(result.contains("### 用户偏好"));
        assertTrue(result.contains("language"));
        assertTrue(result.contains("zh-CN"));
    }

    @Test
    void buildEnhancedSystemPrompt_shouldIncludeLongTermMemory() {
        memoryStore.saveLongTerm("用户信息", "名字：王五");
        memoryStore.saveLongTerm("项目信息", "EasyAgent4j");

        MemoryPromptBuilder builder = new MemoryPromptBuilder();
        String result = builder.buildEnhancedSystemPrompt("Base", memoryStore);

        assertTrue(result.contains("### 长期记忆"));
        assertTrue(result.contains("用户信息"));
        assertTrue(result.contains("王五"));
    }

    @Test
    void buildEnhancedSystemPrompt_shouldHandleEmptyBasePrompt() {
        MemoryPromptBuilder builder = new MemoryPromptBuilder();
        String result = builder.buildEnhancedSystemPrompt("", memoryStore);

        assertTrue(result.contains("## Memory"));
    }

    @Test
    void buildEnhancedSystemPrompt_shouldHandleNullBasePrompt() {
        MemoryPromptBuilder builder = new MemoryPromptBuilder();
        String result = builder.buildEnhancedSystemPrompt(null, memoryStore);

        assertNull(result);
    }
}