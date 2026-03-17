package com.easyagent4j.core.tool.builtin;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Shell命令执行工具 — 白名单模式，只允许执行指定命令。
 * <p>
 * 默认白名单为空（不允许执行任何命令），必须显式添加允许的命令。
 */
public class ShellExecuteTool extends AbstractAgentTool {

    private final Set<String> allowedCommands;
    private final long timeoutSeconds;
    private final int maxOutputLines;

    /**
     * 创建Shell执行工具。
     *
     * @param allowedCommands 允许执行的命令白名单（如 "ls", "grep", "cat" 等）
     */
    public ShellExecuteTool(Set<String> allowedCommands) {
        super("shell_execute", "执行Shell命令（白名单模式，只允许执行预授权的命令）");
        this.allowedCommands = Set.copyOf(allowedCommands);
        this.timeoutSeconds = 30;
        this.maxOutputLines = 500;
    }

    /**
     * 创建Shell执行工具，使用自定义名称。
     */
    public ShellExecuteTool(String name, String description, Set<String> allowedCommands) {
        super(name, description);
        this.allowedCommands = Set.copyOf(allowedCommands);
        this.timeoutSeconds = 30;
        this.maxOutputLines = 500;
    }

    /**
     * 创建Shell执行工具，自定义所有参数。
     */
    public ShellExecuteTool(Set<String> allowedCommands, long timeoutSeconds, int maxOutputLines) {
        super("shell_execute", "执行Shell命令（白名单模式，只允许执行预授权的命令）");
        this.allowedCommands = Set.copyOf(allowedCommands);
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputLines = maxOutputLines;
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "command": {
                            "type": "string",
                            "description": "要执行的Shell命令"
                        },
                        "working_dir": {
                            "type": "string",
                            "description": "工作目录（可选）"
                        },
                        "timeout": {
                            "type": "number",
                            "description": "超时时间（秒），默认30秒"
                        }
                    },
                    "required": ["command"]
                }
                """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String command = context.getStringArg("command");
        String workingDir = context.getStringArg("working_dir");
        Integer timeout = context.getIntArg("timeout");

        if (command == null || command.isBlank()) {
            return ToolResult.error("参数 command 不能为空");
        }

        // 提取基础命令名
        String baseCommand = extractBaseCommand(command);

        // 检查白名单
        if (allowedCommands.isEmpty()) {
            return ToolResult.error("Shell命令执行工具未配置白名单，拒绝执行任何命令。" +
                    "请联系管理员通过 allowedCommands 配置允许的命令。");
        }

        if (!allowedCommands.contains(baseCommand)) {
            return ToolResult.error("命令 '" + baseCommand + "' 不在白名单中。" +
                    "允许的命令: " + allowedCommands);
        }

        // 执行命令
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);
            if (workingDir != null && !workingDir.isBlank()) {
                pb.directory(new java.io.File(workingDir));
            }

            Process process = pb.start();
            long actualTimeout = (timeout != null ? timeout : timeoutSeconds);

            boolean finished = process.waitFor(actualTimeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.error("命令执行超时（" + actualTimeout + "秒），已强制终止");
            }

            // 读取输出（限制行数）
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < maxOutputLines) {
                    output.append(line).append("\n");
                    lineCount++;
                }
                if (lineCount >= maxOutputLines) {
                    output.append("\n... (输出已截断，共 ").append(maxOutputLines).append(" 行)\n");
                }
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return ToolResult.success(output.toString().trim());
            } else {
                return ToolResult.error("命令执行失败 (exit code " + exitCode + "):\n" + output);
            }
        } catch (Exception e) {
            return ToolResult.error("执行命令失败: " + e.getMessage());
        }
    }

    /**
     * 从命令字符串中提取基础命令名。
     * 例如 "ls -la /tmp" -> "ls"
     */
    private String extractBaseCommand(String command) {
        String trimmed = command.trim();
        // 跳过管道等符号前的空白
        String[] parts = trimmed.split("\\s+");
        for (String part : parts) {
            if (!part.isEmpty() && !part.equals("|") && !part.equals("&&") && !part.equals("||") && !part.equals(";")) {
                // 取命令名（去掉路径前缀）
                int lastSlash = part.lastIndexOf('/');
                return lastSlash >= 0 ? part.substring(lastSlash + 1) : part;
            }
        }
        return trimmed;
    }

    /**
     * 获取当前白名单。
     */
    public Set<String> getAllowedCommands() {
        return allowedCommands;
    }
}
