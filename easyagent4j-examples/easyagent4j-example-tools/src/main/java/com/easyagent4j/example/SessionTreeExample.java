package com.easyagent4j.example;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.agent.AgentSessionNode;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.memory.FileMemoryStore;
import com.easyagent4j.core.message.ToolCall;
import com.easyagent4j.core.tool.builtin.SubAgentTool;

import java.util.List;
import java.util.function.Consumer;

/**
 * Session Tree / Sub-Agent 示例。
 * <p>
 * 这个示例不依赖真实模型，使用一个固定回复的ChatModel演示：
 * 1. 主Agent与子Agent的上下文隔离
 * 2. 会话树结构
 * 3. sub_agent 工具如何派生隔离上下文执行子任务
 */
public class SessionTreeExample {

    public static void main(String[] args) throws Exception {
        ChatModel chatModel = new DemoChatModel();
        AgentConfig config = AgentConfig.builder()
            .sessionId("demo-root")
            .streamingEnabled(false)
            .build();

        Agent root = new Agent(config, chatModel);
        root.setMemoryStore(new FileMemoryStore("./agent-memory", root.getContext().getSessionId()));
        root.addTool(new SubAgentTool(root));

        System.out.println("=== Root Agent ===");
        AgentContext rootContext = root.prompt("请总结当前任务").get();
        System.out.println("sessionId = " + rootContext.getSessionId());
        System.out.println("reply = " + rootContext.getLastMessage().getContent());

        System.out.println("\n=== Spawned Child Agent ===");
        Agent child = root.spawnSubAgent("demo-root/research-1", true);
        AgentContext childContext = child.prompt("请单独调查技术选型").get();
        System.out.println("childSessionId = " + childContext.getSessionId());
        System.out.println("parentSessionId = " + childContext.getParentSessionId());
        System.out.println("rootSessionId = " + childContext.getRootSessionId());
        System.out.println("childReply = " + childContext.getLastMessage().getContent());
        System.out.println("rootMessageCount = " + root.getMessages().size());
        System.out.println("childMessageCount = " + child.getMessages().size());

        System.out.println("\n=== Session Tree ===");
        AgentSessionNode rootNode = root.getSessionTree().getNode(root.getContext().getSessionId()).orElseThrow();
        System.out.println("root children = " + rootNode.getChildSessionIds());

        System.out.println("\n=== Sub-Agent Tool ===");
        ToolCall toolCall = new ToolCall(
            "tool_1",
            "sub_agent",
            "{\"task\":\"请在隔离上下文中输出一条建议\",\"childSessionId\":\"demo-root/tool-child\",\"inheritMessages\":true}"
        );
        String result = new SubAgentTool(root)
            .execute(new com.easyagent4j.core.tool.ToolContext(toolCall, root.getContext()))
            .getContent();
        System.out.println(result);
    }

    /**
     * 一个最小可运行的示例模型：
     * - 普通 prompt 返回文本
     */
    static class DemoChatModel implements ChatModel {
        @Override
        public ChatResponse call(ChatRequest request) {
            String last = extractLastUserMessage(request);
            return new ChatResponse("Echo: " + last);
        }

        @Override
        public void stream(ChatRequest request, Consumer<ChatResponseChunk> callback) {
            callback.accept(new ChatResponseChunk(call(request).getText()));
        }

        private String extractLastUserMessage(ChatRequest request) {
            if (request.getMessages() == null || request.getMessages().isEmpty()) {
                return "";
            }
            Object last = request.getMessages().get(request.getMessages().size() - 1);
            return last != null ? last.toString() : "";
        }
    }
}
