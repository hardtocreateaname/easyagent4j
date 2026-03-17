package com.easyagent4j.core.resilience;

import java.time.Duration;

/**
 * 重试策略。
 * 定义重试的配置和延迟计算逻辑。
 */
public class RetryPolicy {
    
    private final int maxRetries;
    private final Duration initialDelay;
    private final double backoffMultiplier;
    
    /**
     * 创建默认重试策略。
     * maxRetries=3, initialDelay=1s, backoffMultiplier=2.0
     */
    public RetryPolicy() {
        this(3, Duration.ofSeconds(1), 2.0);
    }
    
    /**
     * 创建自定义重试策略。
     * 
     * @param maxRetries 最大重试次数
     * @param initialDelay 初始延迟
     * @param backoffMultiplier 退避乘数
     */
    public RetryPolicy(int maxRetries, Duration initialDelay, double backoffMultiplier) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (initialDelay == null || initialDelay.isNegative() || initialDelay.isZero()) {
            throw new IllegalArgumentException("initialDelay must be positive");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }
        
        this.maxRetries = maxRetries;
        this.initialDelay = initialDelay;
        this.backoffMultiplier = backoffMultiplier;
    }
    
    /**
     * 获取最大重试次数。
     */
    public int getMaxRetries() {
        return maxRetries;
    }
    
    /**
     * 获取初始延迟。
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }
    
    /**
     * 获取退避乘数。
     */
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    /**
     * 计算第 attempt 次重试的延迟时间。
     * 
     * @param attempt 重试尝试次数（从 1 开始）
     * @return 延迟时间
     */
    public Duration getNextDelay(int attempt) {
        if (attempt <= 0) {
            return Duration.ZERO;
        }
        
        // 使用指数退避算法：delay = initialDelay * (backoffMultiplier ^ (attempt - 1))
        double multiplier = Math.pow(backoffMultiplier, attempt - 1);
        long delayMillis = (long) (initialDelay.toMillis() * multiplier);
        
        return Duration.ofMillis(delayMillis);
    }
    
    /**
     * 检查是否可以重试。
     * 
     * @param currentRetryCount 当前重试次数
     * @return 如果可以重试返回 true
     */
    public boolean canRetry(int currentRetryCount) {
        return currentRetryCount < maxRetries;
    }
    
    /**
     * 创建 Builder。
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * RetryPolicy 构建器。
     */
    public static class Builder {
        private int maxRetries = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private double backoffMultiplier = 2.0;
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }
        
        public Builder initialDelay(long amount, java.time.temporal.ChronoUnit unit) {
            this.initialDelay = Duration.of(amount, unit);
            return this;
        }
        
        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }
        
        public RetryPolicy build() {
            return new RetryPolicy(maxRetries, initialDelay, backoffMultiplier);
        }
    }
}