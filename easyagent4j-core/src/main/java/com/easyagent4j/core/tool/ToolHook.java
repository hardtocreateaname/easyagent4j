package com.easyagent4j.core.tool;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.message.ToolCall;

/**
 * 工具钩子接口 — 在工具执行前后插入自定义逻辑。
 */
public interface ToolHook {
    /**
     * 工具执行前。返回null表示允许，返回ToolHookResult.block()表示阻止。
     */
    default ToolHookResult beforeToolCall(ToolCall toolCall, AgentContext context) {
        return ToolHookResult.allow();
    }

    /**
     * 工具执行后。
     */
    default void afterToolCall(ToolCall toolCall, ToolResult result,
                                boolean isError, AgentContext context) {
    }
}
