package com.easyagent4j.core.tool.builtin;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.AssistantMessage;
import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

/**
 * 子Agent工具：在隔离上下文中执行一个子任务。
 */
public class SubAgentTool extends AbstractAgentTool {

    private final Agent owner;

    public SubAgentTool(Agent owner) {
        super("sub_agent", "在隔离上下文中派生一个子Agent执行子任务，并返回结果。");
        this.owner = owner;
    }

    public SubAgentTool bindTo(Agent newOwner) {
        return new SubAgentTool(newOwner);
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "task": {
                            "type": "string",
                            "description": "要交给子Agent执行的任务"
                        },
                        "childSessionId": {
                            "type": "string",
                            "description": "可选的子session id"
                        },
                        "inheritMessages": {
                            "type": "boolean",
                            "description": "是否继承父Agent当前消息历史，默认true"
                        }
                    },
                    "required": ["task"]
                }
                """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String task = context.getStringArg("task");
        String childSessionId = context.getStringArg("childSessionId");
        Boolean inheritMessages = context.getBoolArg("inheritMessages");

        if (task == null || task.isBlank()) {
            return ToolResult.error("参数 task 不能为空");
        }

        try {
            Agent child = childSessionId != null && !childSessionId.isBlank()
                ? owner.spawnSubAgent(childSessionId, !Boolean.FALSE.equals(inheritMessages))
                : owner.spawnSubAgent(!Boolean.FALSE.equals(inheritMessages));

            AgentContext childContext = child.prompt(task).join();
            AgentMessage lastMessage = childContext.getLastMessage();
            String reply = "";
            if (lastMessage instanceof AssistantMessage assistantMessage) {
                reply = assistantMessage.getTextContent();
            } else if (lastMessage != null && lastMessage.getContent() != null) {
                reply = lastMessage.getContent().toString();
            }

            String result = """
                child_session_id: %s
                root_session_id: %s
                reply:
                %s
                """.formatted(
                childContext.getSessionId(),
                childContext.getRootSessionId(),
                reply != null ? reply : ""
            );
            return ToolResult.success(result.trim());
        } catch (Exception e) {
            return ToolResult.error("子Agent执行失败: " + e.getMessage());
        }
    }
}
