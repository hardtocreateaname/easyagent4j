package com.easyagent4j.core.tool;

import com.easyagent4j.core.exception.ToolExecutionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工具抽象基类 — 简化工具实现，支持beforeToolCall/afterToolCall钩子链。
 */
public abstract class AbstractAgentTool implements AgentTool {
    private final String name;
    private final String description;
    private final List<ToolHook> hooks = new ArrayList<>();

    protected AbstractAgentTool(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getParameterSchema() { return "{}"; }

    /**
     * 添加工具执行钩子。
     *
     * @param hook 钩子实例
     * @return this（支持链式调用）
     */
    public AbstractAgentTool addHook(ToolHook hook) {
        if (hook != null) {
            this.hooks.add(hook);
        }
        return this;
    }

    /**
     * 获取所有已注册的钩子（不可变列表）。
     */
    public List<ToolHook> getHooks() {
        return Collections.unmodifiableList(hooks);
    }

    @Override
    public final ToolResult execute(ToolContext context) {
        // 执行前：遍历hooks调用beforeToolCall
        for (ToolHook hook : hooks) {
            ToolHookResult hookResult = hook.beforeToolCall(context.getToolCall(), context.getAgentContext());
            if (hookResult.isBlocked()) {
                return ToolResult.error("Blocked: " + hookResult.getReason());
            }
        }

        // 执行工具
        ToolResult result;
        try {
            result = doExecute(context);
        } catch (ToolExecutionException e) {
            // 执行后：通知hooks（即使失败）
            for (ToolHook hook : hooks) {
                hook.afterToolCall(context.getToolCall(), ToolResult.error(e.getMessage()), true, context.getAgentContext());
            }
            throw e;
        } catch (Exception e) {
            ToolExecutionException tee = new ToolExecutionException(name, e.getMessage(), e);
            for (ToolHook hook : hooks) {
                hook.afterToolCall(context.getToolCall(), ToolResult.error(e.getMessage()), true, context.getAgentContext());
            }
            throw tee;
        }

        // 执行后：遍历hooks调用afterToolCall
        for (ToolHook hook : hooks) {
            hook.afterToolCall(context.getToolCall(), result, !result.isSuccess(), context.getAgentContext());
        }

        return result;
    }

    protected abstract ToolResult doExecute(ToolContext context);
}
