package com.easyagent4j.example.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 聊天响应DTO。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("tool_calls")
    private List<ToolCallInfo> toolCalls;

    @JsonProperty("parent_session_id")
    private String parentSessionId;

    @JsonProperty("root_session_id")
    private String rootSessionId;

    @JsonProperty("child_session_ids")
    private List<String> childSessionIds;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("elapsed_ms")
    private long elapsedMs;

    public ChatResponse() {}

    public ChatResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }

    // === Static factory methods ===

    public static ChatResponse success(String message, String sessionId, long elapsedMs) {
        ChatResponse resp = new ChatResponse(message, "success");
        resp.sessionId = sessionId;
        resp.elapsedMs = elapsedMs;
        return resp;
    }

    public static ChatResponse error(String errorMessage) {
        ChatResponse resp = new ChatResponse(null, "error");
        resp.error = errorMessage;
        return resp;
    }

    public static ChatResponse healthOk() {
        ChatResponse resp = new ChatResponse(null, "ok");
        return resp;
    }

    // === Getters & Setters ===

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<ToolCallInfo> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCallInfo> toolCalls) { this.toolCalls = toolCalls; }
    public String getParentSessionId() { return parentSessionId; }
    public void setParentSessionId(String parentSessionId) { this.parentSessionId = parentSessionId; }
    public String getRootSessionId() { return rootSessionId; }
    public void setRootSessionId(String rootSessionId) { this.rootSessionId = rootSessionId; }
    public List<String> getChildSessionIds() { return childSessionIds; }
    public void setChildSessionIds(List<String> childSessionIds) { this.childSessionIds = childSessionIds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }

    /**
     * 工具调用信息。
     */
    public static class ToolCallInfo {
        @JsonProperty("name")
        private String name;

        @JsonProperty("arguments")
        private String arguments;

        @JsonProperty("result")
        private String result;

        public ToolCallInfo() {}

        public ToolCallInfo(String name, String arguments, String result) {
            this.name = name;
            this.arguments = arguments;
            this.result = result;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
