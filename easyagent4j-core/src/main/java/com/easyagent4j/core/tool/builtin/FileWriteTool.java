package com.easyagent4j.core.tool.builtin;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * 文件写入工具 — 安全限制：只能写入允许目录下的文件。
 */
public class FileWriteTool extends AbstractAgentTool {

    private final Set<Path> allowedDirectories;
    private final int maxFileSizeBytes;

    /**
     * 创建文件写入工具。
     *
     * @param allowedDirectories 允许写入的目录集合
     */
    public FileWriteTool(Set<Path> allowedDirectories) {
        super("file_write", "写入文件内容。只能写入允许目录下的文件。");
        this.allowedDirectories = Set.copyOf(allowedDirectories);
        this.maxFileSizeBytes = 1024 * 1024; // 默认最大1MB
    }

    /**
     * 创建文件写入工具，使用默认目录。
     *
     * @param allowedDir 允许写入的目录
     */
    public FileWriteTool(String allowedDir) {
        this(Set.of(Paths.get(allowedDir)));
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "要写入的文件路径（相对于允许目录）"
                        },
                        "content": {
                            "type": "string",
                            "description": "要写入的内容"
                        },
                        "append": {
                            "type": "boolean",
                            "description": "是否追加模式，默认false（覆盖）"
                        },
                        "encoding": {
                            "type": "string",
                            "description": "文件编码，默认UTF-8"
                        }
                    },
                    "required": ["path", "content"]
                }
                """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String filePath = context.getStringArg("path");
        String content = context.getStringArg("content");
        Boolean append = context.getBoolArg("append");
        String encoding = context.getStringArg("encoding");

        if (filePath == null || filePath.isBlank()) {
            return ToolResult.error("参数 path 不能为空");
        }
        if (content == null) {
            return ToolResult.error("参数 content 不能为空");
        }

        // 安全检查：防止路径遍历
        if (filePath.contains("..")) {
            return ToolResult.error("路径中不允许包含 '..'");
        }

        Path resolvedPath = resolveSafePath(filePath);
        if (resolvedPath == null) {
            return ToolResult.error("文件路径不在允许的目录范围内。允许的目录: " + allowedDirectories);
        }

        if (content.length() > maxFileSizeBytes) {
            return ToolResult.error("内容过大，超过限制 " + (maxFileSizeBytes / 1024) + "KB");
        }

        try {
            // 自动创建父目录
            Path parent = resolvedPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (Boolean.TRUE.equals(append)) {
                Files.writeString(resolvedPath, content,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(resolvedPath, content,
                        encoding != null ? java.nio.file.StandardOpenOption.TRUNCATE_EXISTING : java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
                Files.writeString(resolvedPath, content,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            }

            return ToolResult.success("文件写入成功: " + resolvedPath + " (" + content.length() + " 字节)");
        } catch (IOException e) {
            return ToolResult.error("写入文件失败: " + e.getMessage());
        }
    }

    private Path resolveSafePath(String filePath) {
        for (Path allowedDir : allowedDirectories) {
            Path resolved = allowedDir.resolve(filePath).normalize().toAbsolutePath();
            Path allowedAbs = allowedDir.normalize().toAbsolutePath();
            if (resolved.startsWith(allowedAbs)) {
                return resolved;
            }
        }
        return null;
    }
}
