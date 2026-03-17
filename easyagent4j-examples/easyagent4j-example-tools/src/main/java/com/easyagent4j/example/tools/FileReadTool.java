package com.easyagent4j.example.tools;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件读取工具示例。
 */
public class FileReadTool extends AbstractAgentTool {

    public FileReadTool() {
        super("read_file", "读取指定路径的文件内容并返回");
    }

    @Override
    public String getParameterSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "path": {"type": "string", "description": "文件路径"}
                },
                "required": ["path"]
            }
            """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String path = context.getStringArg("path");
        try {
            String content = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            if (content.length() > 5000) {
                content = content.substring(0, 5000) + "\n... (truncated, total " + content.length() + " chars)";
            }
            return ToolResult.success(content);
        } catch (IOException e) {
            return ToolResult.error("无法读取文件: " + e.getMessage());
        }
    }
}
