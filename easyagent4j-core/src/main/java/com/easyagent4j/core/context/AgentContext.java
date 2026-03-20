package com.easyagent4j.core.context;

import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.AssistantMessage;
import com.easyagent4j.core.message.ContentPart;
import com.easyagent4j.core.message.SystemMessage;
import com.easyagent4j.core.message.ToolCall;
import com.easyagent4j.core.message.ToolResultMessage;
import com.easyagent4j.core.message.UserMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent上下文 — 维护消息历史和运行时状态。
 */
public class AgentContext {
    private final List<AgentMessage> messages;
    private final Map<String, Object> attributes;
    private final String sessionId;
    private final String parentSessionId;
    private final String rootSessionId;
    private final int depth;

    public AgentContext() {
        this(null);
    }

    public AgentContext(String sessionId) {
        this(sessionId, null, sessionId, 0);
    }

    public AgentContext(String sessionId, String parentSessionId, String rootSessionId, int depth) {
        this.messages = new ArrayList<>();
        this.attributes = new ConcurrentHashMap<>();
        this.sessionId = sessionId;
        this.parentSessionId = parentSessionId;
        this.rootSessionId = rootSessionId != null ? rootSessionId : sessionId;
        this.depth = Math.max(0, depth);
    }

    public void addMessage(AgentMessage message) {
        messages.add(message);
    }

    public List<AgentMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void setMessages(List<AgentMessage> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
    }

    public void clearMessages() {
        messages.clear();
    }

    public int getMessageCount() { return messages.size(); }

    public AgentMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public void setAttribute(String key, Object value) { attributes.put(key, value); }
    public Object getAttribute(String key) { return attributes.get(key); }
    public Map<String, Object> getAttributes() { return Collections.unmodifiableMap(attributes); }

    public String getSessionId() { return sessionId; }
    public String getParentSessionId() { return parentSessionId; }
    public String getRootSessionId() { return rootSessionId; }
    public int getDepth() { return depth; }

    /**
     * 复制当前上下文，形成一条新的会话分支。
     * 子上下文会继承属性快照，并可选择是否继承消息历史。
     */
    public AgentContext fork(String childSessionId, boolean inheritMessages) {
        AgentContext forked = new AgentContext(childSessionId, sessionId, rootSessionId != null ? rootSessionId : sessionId, depth + 1);
        forked.attributes.putAll(attributes);
        if (inheritMessages) {
            for (AgentMessage message : messages) {
                forked.messages.add(copyMessage(message));
            }
        }
        return forked;
    }

    private AgentMessage copyMessage(AgentMessage message) {
        if (message instanceof UserMessage userMessage) {
            return new UserMessage(
                userMessage.getTextContent(),
                copyParts(userMessage.getParts()),
                userMessage.getTimestamp(),
                new HashMap<>(userMessage.getMetadata())
            );
        }
        if (message instanceof AssistantMessage assistantMessage) {
            AssistantMessage copy = new AssistantMessage(
                assistantMessage.getTextContent(),
                copyToolCalls(assistantMessage.getToolCalls())
            );
            copy.getMetadata().putAll(message.getMetadata());
            return copy;
        }
        if (message instanceof ToolResultMessage toolResultMessage) {
            ToolResultMessage copy = new ToolResultMessage(
                copyToolCall(toolResultMessage.getToolCall()),
                toolResultMessage.getResultContent(),
                toolResultMessage.isError()
            );
            copy.getMetadata().putAll(message.getMetadata());
            return copy;
        }
        if (message instanceof SystemMessage systemMessage) {
            SystemMessage copy = new SystemMessage(systemMessage.getTextContent());
            copy.getMetadata().putAll(message.getMetadata());
            return copy;
        }
        return message;
    }

    private List<ContentPart> copyParts(List<ContentPart> parts) {
        List<ContentPart> copies = new ArrayList<>();
        for (ContentPart part : parts) {
            ContentPart.Builder builder = ContentPart.builder().type(part.getType()).text(part.getText());
            if (part.getImageData() != null) {
                builder.imageData(Arrays.copyOf(part.getImageData(), part.getImageData().length), part.getMimeType());
            }
            if (part.getToolCall() != null) {
                builder.toolCall(copyToolCall(part.getToolCall()));
            }
            copies.add(builder.build());
        }
        return copies;
    }

    private List<ToolCall> copyToolCalls(List<ToolCall> toolCalls) {
        List<ToolCall> copies = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            copies.add(copyToolCall(toolCall));
        }
        return copies;
    }

    private ToolCall copyToolCall(ToolCall toolCall) {
        if (toolCall == null) {
            return null;
        }
        return new ToolCall(toolCall.getId(), toolCall.getName(), toolCall.getArguments());
    }
}
