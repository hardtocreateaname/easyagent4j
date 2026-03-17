package com.easyagent4j.core.exception;

/**
 * Agent框架基础异常。
 */
public class AgentException extends RuntimeException {
    public AgentException(String message) { super(message); }
    public AgentException(String message, Throwable cause) { super(message, cause); }
}
