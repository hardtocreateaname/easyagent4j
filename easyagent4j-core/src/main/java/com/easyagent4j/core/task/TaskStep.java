package com.easyagent4j.core.task;

/**
 * 任务步骤。
 * 表示任务计划中的一个具体执行步骤。
 */
public record TaskStep(
    String description,
    String action,
    boolean completed,
    String result,
    int maxRetries,
    int retryCount
) {
    public TaskStep(String description, String action) {
        this(description, action, false, null, 3, 0);
    }

    public TaskStep(String description, String action, int maxRetries) {
        this(description, action, false, null, maxRetries, 0);
    }

    public TaskStep markCompleted(String result) {
        return new TaskStep(description, action, true, result, maxRetries, retryCount);
    }

    public TaskStep markFailed(String error) {
        return new TaskStep(description, action, false, error, maxRetries, retryCount);
    }

    public TaskStep incrementRetry() {
        return new TaskStep(description, action, false, result, maxRetries, retryCount + 1);
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }
}