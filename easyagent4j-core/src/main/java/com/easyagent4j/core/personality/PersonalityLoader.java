package com.easyagent4j.core.personality;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Personality文件加载器。
 *
 * 支持从文件系统或classpath加载性格配置文件。
 * 自动识别文件格式：.json使用JSON格式，.md使用Markdown格式。
 */
public class PersonalityLoader {

    private PersonalityLoader() {
        // 工具类，防止实例化
    }

    /**
     * 从文件路径加载性格配置。
     *
     * @param path 文件路径
     * @return AgentPersonality实例
     * @throws IOException 读取文件失败时抛出
     */
    public static AgentPersonality load(Path path) throws IOException {
        String content = Files.readString(path);
        String filename = path.getFileName().toString().toLowerCase();

        if (filename.endsWith(".json")) {
            try {
                return AgentPersonality.fromJson(content);
            } catch (Exception e) {
                throw new IOException("Failed to parse personality from JSON: " + path, e);
            }
        } else if (filename.endsWith(".md")) {
            return AgentPersonality.fromMarkdown(content);
        } else {
            throw new IOException("Unsupported file format: " + filename + ". Supported formats: .json, .md");
        }
    }

    /**
     * 从classpath加载性格配置。
     *
     * @param resource 资源路径（例如：/personality.json 或 /personality.md）
     * @return AgentPersonality实例
     * @throws IOException 读取资源失败时抛出
     */
    public static AgentPersonality loadFromClasspath(String resource) throws IOException {
        String content = readResource(resource);
        String resourceLower = resource.toLowerCase();

        if (resourceLower.endsWith(".json")) {
            try {
                return AgentPersonality.fromJson(content);
            } catch (Exception e) {
                throw new IOException("Failed to parse personality from JSON resource: " + resource, e);
            }
        } else if (resourceLower.endsWith(".md")) {
            return AgentPersonality.fromMarkdown(content);
        } else {
            throw new IOException("Unsupported resource format: " + resource + ". Supported formats: .json, .md");
        }
    }

    /**
     * 读取classpath资源内容。
     */
    private static String readResource(String resource) throws IOException {
        try (InputStream is = PersonalityLoader.class.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(is.readAllBytes());
        }
    }
}