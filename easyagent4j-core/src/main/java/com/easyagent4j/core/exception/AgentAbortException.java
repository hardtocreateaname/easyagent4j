package com.easyagent4j.core.exception;

/**
 * Agent被用户中断。
 */
public class AgentAbortException extends AgentException {
    public AgentAbortException() { super("Agent execution was aborted"); }
}
