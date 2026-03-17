package com.easyagent4j.core.tool.builtin;

import com.easyagent4j.core.tool.AgentTool;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 内置工具注册辅助类 — 提供便捷方式创建常用的内置工具。
 * <p>
 * 使用示例：
 * <pre>
 *     // 全部使用默认配置
 *     List&lt;AgentTool&gt; tools = BuiltinTools.allDefaults();
 *
 *     // 文件工具（指定允许目录）
 *     List&lt;AgentTool&gt; tools = BuiltinTools.fileTools("/tmp/workspace");
 *
 *     // Shell工具（指定白名单）
 *     List&lt;AgentTool&gt; tools = BuiltinTools.shellTools(Set.of("ls", "cat", "grep", "pwd"));
 * </pre>
 */
public final class BuiltinTools {

    private BuiltinTools() {
        // 工具类，不允许实例化
    }

    /**
     * 创建文件读写工具对。
     *
     * @param allowedDir 允许操作的目录
     * @return 文件读写工具列表
     */
    public static List<AgentTool> fileTools(String allowedDir) {
        return fileTools(Set.of(Paths.get(allowedDir)));
    }

    /**
     * 创建文件读写工具对。
     *
     * @param allowedDirs 允许操作的目录集合
     * @return 文件读写工具列表
     */
    public static List<AgentTool> fileTools(Set<Path> allowedDirs) {
        List<AgentTool> tools = new ArrayList<>();
        tools.add(new FileReadTool(allowedDirs));
        tools.add(new FileWriteTool(allowedDirs));
        return tools;
    }

    /**
     * 创建Shell执行工具。
     *
     * @param allowedCommands 允许执行的命令白名单
     * @return Shell工具
     */
    public static ShellExecuteTool shellTools(Set<String> allowedCommands) {
        return new ShellExecuteTool(allowedCommands);
    }

    /**
     * 创建Web搜索工具（允许所有域名）。
     *
     * @return Web搜索工具
     */
    public static WebSearchTool webSearchTool() {
        return new WebSearchTool();
    }

    /**
     * 创建Web搜索工具（域名白名单模式）。
     *
     * @param allowedDomains 允许的域名集合
     * @return Web搜索工具
     */
    public static WebSearchTool webSearchTool(Set<String> allowedDomains) {
        return new WebSearchTool(allowedDomains);
    }

    /**
     * 创建所有默认内置工具（安全配置，文件操作限制在 /tmp/easyagent4j-workspace，Shell白名单为空）。
     *
     * @return 所有内置工具列表
     */
    public static List<AgentTool> allDefaults() {
        List<AgentTool> tools = new ArrayList<>();

        // 文件工具 - 限制在默认工作目录
        String defaultDir = "/tmp/easyagent4j-workspace";
        Path defaultPath = Paths.get(defaultDir);
        try {
            java.nio.file.Files.createDirectories(defaultPath);
        } catch (Exception ignored) {
            // 目录可能已存在
        }
        tools.addAll(fileTools(defaultDir));

        // Shell工具 - 白名单为空（默认不允许任何命令，需显式配置）
        tools.add(new ShellExecuteTool(Set.of()));

        // Web搜索工具 - 允许所有域名
        tools.add(new WebSearchTool());

        return tools;
    }

    /**
     * 创建开发者友好的内置工具集（Shell允许常用命令）。
     * <p>
     * ⚠️ 仅用于开发环境！
     *
     * @param workDir 工作目录
     * @return 内置工具列表
     */
    public static List<AgentTool> developerTools(String workDir) {
        List<AgentTool> tools = new ArrayList<>();

        // 文件工具
        tools.addAll(fileTools(workDir));

        // Shell工具 - 开发者常用命令
        tools.add(new ShellExecuteTool(Set.of(
                "ls", "cat", "head", "tail", "grep", "find", "wc",
                "pwd", "echo", "date", "whoami", "env",
                "java", "mvn", "javac",
                "curl", "wget", "ping"
        )));

        // Web搜索工具
        tools.add(new WebSearchTool());

        return tools;
    }
}
