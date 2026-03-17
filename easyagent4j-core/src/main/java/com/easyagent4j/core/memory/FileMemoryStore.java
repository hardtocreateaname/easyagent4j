package com.easyagent4j.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于文件的记忆存储实现。
 *
 * 存储结构：
 * ${basePath}/${sessionId}/
 *   memory.md          — 长期记忆（Markdown格式）
 *   preferences.json   — 用户偏好（JSON）
 */
public class FileMemoryStore implements MemoryStore {
    private static final Logger log = LoggerFactory.getLogger(FileMemoryStore.class);

    private static final String MEMORY_FILE = "memory.md";
    private static final String PREFERENCES_FILE = "preferences.json";

    private final Path basePath;
    private final String sessionId;

    public FileMemoryStore(String basePath, String sessionId) {
        this.basePath = Paths.get(basePath, sessionId);
        this.sessionId = sessionId;
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(basePath);
            // 创建memory.md如果不存在
            Path memoryPath = basePath.resolve(MEMORY_FILE);
            if (!Files.exists(memoryPath)) {
                Files.writeString(memoryPath, "# Agent Memory\n\n", StandardOpenOption.CREATE);
            }
            // 创建preferences.json如果不存在
            Path prefPath = basePath.resolve(PREFERENCES_FILE);
            if (!Files.exists(prefPath)) {
                Files.writeString(prefPath, "{}", StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            log.error("Failed to initialize memory directories: {}", e.getMessage());
        }
    }

    // === 长期记忆 ===

    @Override
    public void saveLongTerm(String key, String content) {
        try {
            Path memoryPath = basePath.resolve(MEMORY_FILE);
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            String entry = String.format("## %s\n[%s] %s\n\n", key, timestamp, content);
            Files.writeString(memoryPath, entry, StandardOpenOption.APPEND);
            log.debug("Saved long-term memory: {}", key);
        } catch (IOException e) {
            log.error("Failed to save long-term memory: {}", e.getMessage());
        }
    }

    @Override
    public Optional<String> loadLongTerm(String key) {
        try {
            String content = Files.readString(basePath.resolve(MEMORY_FILE));
            Pattern pattern = Pattern.compile("## " + Pattern.quote(key) + "\\s*\\n\\[.*?\\]\\s*(.*?)(?=\\n## |\\n\\n|$)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return Optional.of(matcher.group(1).trim());
            }
        } catch (IOException e) {
            log.error("Failed to load long-term memory: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<MemoryEntry> searchLongTerm(String query, int limit) {
        try {
            String content = Files.readString(basePath.resolve(MEMORY_FILE));
            List<MemoryEntry> results = new ArrayList<>();

            Pattern sectionPattern = Pattern.compile("## (.*?)\\s*\\n\\[(.*?)\\]\\s*(.*?)(?=\\n## |$)", Pattern.DOTALL);
            Matcher sectionMatcher = sectionPattern.matcher(content);

            while (sectionMatcher.find()) {
                String key = sectionMatcher.group(1).trim();
                String timestamp = sectionMatcher.group(2);
                String entryContent = sectionMatcher.group(3).trim();

                // 简单的关键词匹配
                if (key.toLowerCase().contains(query.toLowerCase()) ||
                    entryContent.toLowerCase().contains(query.toLowerCase())) {
                    Instant createdAt = Instant.parse(timestamp);
                    results.add(new MemoryEntry(key, entryContent, createdAt, createdAt, Map.of()));
                }
            }

            return results.stream().limit(limit).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to search long-term memory: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<MemoryEntry> listLongTerm() {
        try {
            String content = Files.readString(basePath.resolve(MEMORY_FILE));
            List<MemoryEntry> entries = new ArrayList<>();

            Pattern sectionPattern = Pattern.compile("## (.*?)\\s*\\n\\[(.*?)\\]\\s*(.*?)(?=\\n## |$)", Pattern.DOTALL);
            Matcher sectionMatcher = sectionPattern.matcher(content);

            while (sectionMatcher.find()) {
                String key = sectionMatcher.group(1).trim();
                String timestamp = sectionMatcher.group(2);
                String entryContent = sectionMatcher.group(3).trim();
                Instant createdAt = Instant.parse(timestamp);
                entries.add(new MemoryEntry(key, entryContent, createdAt, createdAt, Map.of()));
            }

            return entries;
        } catch (IOException e) {
            log.error("Failed to list long-term memory: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void deleteLongTerm(String key) {
        try {
            Path memoryPath = basePath.resolve(MEMORY_FILE);
            String content = Files.readString(memoryPath);
            // 移除匹配的section（包括标题和内容）
            String newContent = content.replaceAll("## " + Pattern.quote(key) + "\\s*\\n\\[.*?\\]\\s*.*?(?=\\n## |\\n\\n|$)", "");
            Files.writeString(memoryPath, newContent, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("Deleted long-term memory: {}", key);
        } catch (IOException e) {
            log.error("Failed to delete long-term memory: {}", e.getMessage());
        }
    }

    @Override
    public void consolidate() {
        // TODO: 实现短期记忆整合逻辑
        log.debug("Consolidate called - not yet implemented");
    }

    // === 用户偏好 ===

    @Override
    public void setUserPreference(String key, String value) {
        try {
            Path prefPath = basePath.resolve(PREFERENCES_FILE);
            Map<String, String> prefs = loadPreferences();
            prefs.put(key, value);
            savePreferences(prefs);
        } catch (IOException e) {
            log.error("Failed to set user preference: {}", e.getMessage());
        }
    }

    @Override
    public Optional<String> getUserPreference(String key) {
        try {
            Map<String, String> prefs = loadPreferences();
            return Optional.ofNullable(prefs.get(key));
        } catch (IOException e) {
            log.error("Failed to get user preference: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> getAllPreferences() {
        try {
            return loadPreferences();
        } catch (IOException e) {
            log.error("Failed to get all preferences: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> loadPreferences() throws IOException {
        Path prefPath = basePath.resolve(PREFERENCES_FILE);
        String content = Files.readString(prefPath);
        return parseJsonMap(content);
    }

    private void savePreferences(Map<String, String> prefs) throws IOException {
        Path prefPath = basePath.resolve(PREFERENCES_FILE);
        String json = mapToJson(prefs);
        Files.writeString(prefPath, json, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // 简单的JSON解析（避免依赖）
    private Map<String, String> parseJsonMap(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.trim().isEmpty() || json.equals("{}")) {
            return result;
        }
        // 移除大括号
        String content = json.trim().substring(1, json.trim().length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }
        // 简单的键值对解析
        String[] pairs = content.split(",");
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(":");
            if (colonIdx > 0) {
                String key = pair.substring(0, colonIdx).trim();
                String value = pair.substring(colonIdx + 1).trim();
                // 移除引号
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        }
        return result;
    }

    private String mapToJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}