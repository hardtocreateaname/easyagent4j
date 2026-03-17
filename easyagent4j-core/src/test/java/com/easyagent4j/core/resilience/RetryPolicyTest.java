package com.easyagent4j.core.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RetryPolicy 单元测试。
 */
class RetryPolicyTest {

    @Test
    void testDefaultRetryPolicy() {
        RetryPolicy policy = new RetryPolicy();
        
        assertEquals(3, policy.getMaxRetries());
        assertEquals(Duration.ofSeconds(1), policy.getInitialDelay());
        assertEquals(2.0, policy.getBackoffMultiplier());
    }

    @Test
    void testCustomRetryPolicy() {
        RetryPolicy policy = new RetryPolicy(5, Duration.ofMillis(500), 3.0);
        
        assertEquals(5, policy.getMaxRetries());
        assertEquals(Duration.ofMillis(500), policy.getInitialDelay());
        assertEquals(3.0, policy.getBackoffMultiplier());
    }

    @Test
    void testGetNextDelay() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1), 2.0);
        
        // attempt 0: 0ms
        assertEquals(Duration.ZERO, policy.getNextDelay(0));
        
        // attempt 1: 1000ms (initialDelay * 2^0)
        assertEquals(Duration.ofSeconds(1), policy.getNextDelay(1));
        
        // attempt 2: 2000ms (initialDelay * 2^1)
        assertEquals(Duration.ofSeconds(2), policy.getNextDelay(2));
        
        // attempt 3: 4000ms (initialDelay * 2^2)
        assertEquals(Duration.ofSeconds(4), policy.getNextDelay(3));
        
        // attempt 4: 8000ms (initialDelay * 2^3)
        assertEquals(Duration.ofSeconds(8), policy.getNextDelay(4));
    }

    @Test
    void testGetNextDelayWithDifferentMultiplier() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(100), 3.0);
        
        // attempt 1: 100ms
        assertEquals(Duration.ofMillis(100), policy.getNextDelay(1));
        
        // attempt 2: 300ms
        assertEquals(Duration.ofMillis(300), policy.getNextDelay(2));
        
        // attempt 3: 900ms
        assertEquals(Duration.ofMillis(900), policy.getNextDelay(3));
        
        // attempt 4: 2700ms
        assertEquals(Duration.ofMillis(2700), policy.getNextDelay(4));
    }

    @Test
    void testGetNextDelayWithMultiplier1() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(2), 1.0);
        
        // 所有重试都是相同的延迟
        assertEquals(Duration.ofSeconds(2), policy.getNextDelay(1));
        assertEquals(Duration.ofSeconds(2), policy.getNextDelay(2));
        assertEquals(Duration.ofSeconds(2), policy.getNextDelay(3));
    }

    @Test
    void testCanRetry() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1), 2.0);
        
        assertTrue(policy.canRetry(0));
        assertTrue(policy.canRetry(1));
        assertTrue(policy.canRetry(2));
        assertFalse(policy.canRetry(3));
        assertFalse(policy.canRetry(4));
    }

    @Test
    void testCanRetryWithZeroMaxRetries() {
        RetryPolicy policy = new RetryPolicy(0, Duration.ofSeconds(1), 2.0);
        
        assertFalse(policy.canRetry(0));
        assertFalse(policy.canRetry(1));
    }

    @Test
    void testBuilder() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxRetries(5)
            .initialDelay(Duration.ofMillis(200))
            .backoffMultiplier(1.5)
            .build();
        
        assertEquals(5, policy.getMaxRetries());
        assertEquals(Duration.ofMillis(200), policy.getInitialDelay());
        assertEquals(1.5, policy.getBackoffMultiplier());
    }

    @Test
    void testBuilderWithDefaultValues() {
        RetryPolicy policy = RetryPolicy.builder().build();
        
        assertEquals(3, policy.getMaxRetries());
        assertEquals(Duration.ofSeconds(1), policy.getInitialDelay());
        assertEquals(2.0, policy.getBackoffMultiplier());
    }

    @Test
    void testBuilderWithChaining() {
        RetryPolicy policy = RetryPolicy.builder()
            .maxRetries(4)
            .initialDelay(Duration.ofMillis(500))
            .backoffMultiplier(2.5)
            .build();
        
        assertEquals(4, policy.getMaxRetries());
        assertEquals(Duration.ofMillis(500), policy.getInitialDelay());
        assertEquals(2.5, policy.getBackoffMultiplier());
    }

    @Test
    void testBuilderWithInitialDelayUsingChronoUnit() {
        RetryPolicy policy = RetryPolicy.builder()
            .initialDelay(2, java.time.temporal.ChronoUnit.SECONDS)
            .build();
        
        assertEquals(Duration.ofSeconds(2), policy.getInitialDelay());
    }

    @Test
    void testInvalidMaxRetries() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(-1, Duration.ofSeconds(1), 2.0));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(Integer.MIN_VALUE, Duration.ofSeconds(1), 2.0));
    }

    @Test
    void testInvalidInitialDelay() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, Duration.ZERO, 2.0));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, Duration.ofMillis(-1), 2.0));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, Duration.ofSeconds(-1), 2.0));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, null, 2.0));
    }

    @Test
    void testInvalidBackoffMultiplier() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, Duration.ofSeconds(1), 0.9));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, Duration.ofSeconds(1), 0.0));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, Duration.ofSeconds(1), -1.0));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(3, Duration.ofSeconds(1), Double.NEGATIVE_INFINITY));
    }

    @Test
    void testEdgeCases() {
        // 边界情况：maxRetries = 0
        RetryPolicy noRetryPolicy = new RetryPolicy(0, Duration.ofSeconds(1), 2.0);
        assertEquals(0, noRetryPolicy.getMaxRetries());
        assertFalse(noRetryPolicy.canRetry(0));
        
        // 边界情况：backoffMultiplier = 1.0
        RetryPolicy linearRetryPolicy = new RetryPolicy(3, Duration.ofMillis(100), 1.0);
        assertEquals(Duration.ofMillis(100), linearRetryPolicy.getNextDelay(1));
        assertEquals(Duration.ofMillis(100), linearRetryPolicy.getNextDelay(2));
        
        // 边界情况：小延迟
        RetryPolicy smallDelayPolicy = new RetryPolicy(3, Duration.ofMillis(1), 2.0);
        assertEquals(Duration.ofMillis(1), smallDelayPolicy.getNextDelay(1));
    }

    @Test
    void testLargeRetries() {
        RetryPolicy policy = new RetryPolicy(10, Duration.ofMillis(100), 2.0);
        
        // 测试较大的重试次数
        assertEquals(Duration.ofMillis(100), policy.getNextDelay(1));    // 100
        assertEquals(Duration.ofMillis(200), policy.getNextDelay(2));    // 200
        assertEquals(Duration.ofMillis(400), policy.getNextDelay(3));    // 400
        assertEquals(Duration.ofMillis(800), policy.getNextDelay(4));    // 800
        assertEquals(Duration.ofMillis(1600), policy.getNextDelay(5));   // 1600
        assertEquals(Duration.ofMillis(3200), policy.getNextDelay(6));   // 3200
        assertEquals(Duration.ofMillis(6400), policy.getNextDelay(7));   // 6400
        assertEquals(Duration.ofMillis(12800), policy.getNextDelay(8));  // 12800
        assertEquals(Duration.ofMillis(25600), policy.getNextDelay(9));  // 25600
        assertEquals(Duration.ofMillis(51200), policy.getNextDelay(10)); // 51200
    }

    @Test
    void testFloatingPointPrecision() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(100), 1.5);
        
        // 测试浮点数精度
        assertEquals(Duration.ofMillis(100), policy.getNextDelay(1));   // 100
        assertEquals(Duration.ofMillis(150), policy.getNextDelay(2));   // 150
        assertEquals(Duration.ofMillis(225), policy.getNextDelay(3));   // 225
        assertEquals(Duration.ofMillis(337), policy.getNextDelay(4));   // 337.5 -> 337
    }
}