package com.easyagent4j.core.tool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolResult的单元测试。
 */
class ToolResultTest {

    @Test
    void success_shouldReturnTrue() {
        ToolResult result = ToolResult.success("done");
        assertTrue(result.isSuccess());
    }

    @Test
    void success_shouldReturnContent() {
        ToolResult result = ToolResult.success("hello world");
        assertEquals("hello world", result.getContent());
    }

    @Test
    void success_shouldReturnEmptyDetailsWhenNull() {
        ToolResult result = ToolResult.success("done");
        assertNotNull(result.getDetails());
        assertTrue(result.getDetails().isEmpty());
    }

    @Test
    void success_withDetails_shouldReturnDetails() {
        Map<String, Object> details = Map.of("count", 3, "source", "db");
        ToolResult result = ToolResult.success("done", details);

        assertTrue(result.isSuccess());
        assertEquals("done", result.getContent());
        assertEquals(3, result.getDetails().get("count"));
        assertEquals("db", result.getDetails().get("source"));
    }

    @Test
    void error_shouldReturnFalse() {
        ToolResult result = ToolResult.error("something went wrong");
        assertFalse(result.isSuccess());
    }

    @Test
    void error_shouldReturnErrorMessage() {
        ToolResult result = ToolResult.error("timeout");
        assertEquals("timeout", result.getContent());
    }

    @Test
    void error_shouldReturnEmptyDetails() {
        ToolResult result = ToolResult.error("fail");
        assertNotNull(result.getDetails());
        assertTrue(result.getDetails().isEmpty());
    }
}
