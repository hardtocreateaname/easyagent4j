package com.easyagent4j.core.tool.builtin;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * 文件读取工具 — 安全限制：只能读取允许目录下的文件。
 */
public class FileReadTool extends AbstractAgentTool {

    private final Set<Path> allowedDirectories;
    private final int maxFileSizeBytes;

    /**
     * 创建文件读取工具。
     *
     * @param allowedDirectories 允许读取的目录集合
     */
    public FileReadTool(Set<Path> allowedDirectories) {
        super("file_read", "读取文件内容。只能读取允许目录下的文件。");
        this.allowedDirectories = Set.copyOf(allowedDirectories);
        this.maxFileSizeBytes = 1024 * 1024; // 默认最大1MB
    }

    /**
     * 创建文件读取工具，使用默认目录。
     *
     * @param allowedDir 允许读取的目录
     */
    public FileReadTool(String allowedDir) {
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
                            "description": "要读取的文件路径（相对于允许目录）"
                        },
                        "encoding": {
                            "type": "string",
                            "description": "文件编码，默认UTF-8"
                        }
                    },
                    "required": ["path"]
                }
                """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String filePath = context.getStringArg("path");
        String encoding = context.getStringArg("encoding");

        if (filePath == null || filePath.isBlank()) {
            return ToolResult.error("参数 path 不能为空");
        }

        // 安全检查：防止路径遍历
        if (filePath.contains("..")) {
            return ToolResult.error("路径中不允许包含 '..'");
        }

        Path resolvedPath = resolveSafePath(filePath);
        if (resolvedPath == null) {
            return ToolResult.error("文件路径不在允许的目录范围内。允许的目录: " + allowedDirectories);
        }

        try {
            if (!Files.exists(resolvedPath)) {
                return ToolResult.error("文件不存在: " + resolvedPath);
            }
            if (!Files.isRegularFile(resolvedPath)) {
                return ToolResult.error("路径不是文件: " + resolvedPath);
            }
            if (Files.size(resolvedPath) > maxFileSizeBytes) {
                return ToolResult.error("文件过大，超过限制 " + (maxFileSizeBytes / 1024) + "KB");
            }

            Charset charset = (encoding != null) ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            String content = Files.readString(resolvedPath, charset);
            return ToolResult.success(content);
        } catch (Exception e) {
            return ToolResult.error("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 校验文件路径是否在允许的目录范围内。
     */
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
