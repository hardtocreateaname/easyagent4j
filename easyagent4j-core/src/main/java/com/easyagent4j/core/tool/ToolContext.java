package com.easyagent4j.core.tool;

import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.message.ToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 工具执行上下文。
 */
public class ToolContext {
    private final ToolCall toolCall;
    private final AgentContext agentContext;
    private final Map<String, Object> arguments;
    private final String rawArguments;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ToolContext(ToolCall toolCall, AgentContext agentContext) {
        this.toolCall = toolCall;
        this.agentContext = agentContext;
        this.rawArguments = toolCall.getArguments();
        try {
            this.arguments = rawArguments != null && !rawArguments.isEmpty()
                ? MAPPER.readValue(rawArguments, new TypeReference<Map<String, Object>>() {})
                : Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tool arguments: " + rawArguments, e);
        }
    }

    public ToolCall getToolCall() { return toolCall; }
    public AgentContext getAgentContext() { return agentContext; }
    public Map<String, Object> getArguments() { return arguments; }
    public String getRawArguments() { return rawArguments; }

    public String getStringArg(String name) {
        Object v = arguments.get(name);
        return v != null ? v.toString() : null;
    }

    public Integer getIntArg(String name) {
        Object v = arguments.get(name);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    public Boolean getBoolArg(String name) {
        Object v = arguments.get(name);
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    @SuppressWarnings("unchecked")
    public <T> T getArg(String name, Class<T> type) {
        Object v = arguments.get(name);
        return v != null ? (T) v : null;
    }
}
