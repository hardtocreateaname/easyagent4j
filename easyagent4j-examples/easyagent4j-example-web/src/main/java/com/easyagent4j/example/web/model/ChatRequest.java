package com.easyagent4j.example.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 聊天请求DTO。
 */
public class ChatRequest {

    @JsonProperty("message")
    private String message;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("system_prompt")
    private String systemPrompt;

    @JsonProperty("stream")
    private boolean stream = false;

    public ChatRequest() {}

    public ChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
}
