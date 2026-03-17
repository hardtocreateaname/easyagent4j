package com.easyagent4j.core.exception;

/**
 * 超过最大工具调用轮数。
 */
public class MaxIterationsExceededException extends AgentException {
    private final int maxIterations;

    public MaxIterationsExceededException(int maxIterations) {
        super("Max tool iterations (" + maxIterations + ") exceeded");
        this.maxIterations = maxIterations;
    }

    public int getMaxIterations() { return maxIterations; }
}
