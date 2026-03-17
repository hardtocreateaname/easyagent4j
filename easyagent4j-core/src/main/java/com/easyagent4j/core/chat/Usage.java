package com.easyagent4j.core.chat;

/**
 * Token使用量。
 */
public class Usage {
    private final long promptTokens;
    private final long completionTokens;
    private final long totalTokens;

    public Usage(long promptTokens, long completionTokens, long totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public long getPromptTokens() { return promptTokens; }
    public long getCompletionTokens() { return completionTokens; }
    public long getTotalTokens() { return totalTokens; }
}
