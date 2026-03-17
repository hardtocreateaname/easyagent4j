package com.easyagent4j.core.agent;

import com.easyagent4j.core.*;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.context.MessageConverter;
import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.context.converter.DefaultMessageConverter;
import com.easyagent4j.core.event.AgentEventPublisher;
import com.easyagent4j.core.message.AssistantMessage;
import com.easyagent4j.core.message.MessageRole;
import com.easyagent4j.core.message.ToolCall;
import com.easyagent4j.core.message.UserMessage;
import com.easyagent4j.core.tool.AgentTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并行工具执行测试。
 */
class ParallelExecutionTest {

    private MockChatModel mockChatModel;
    private AgentEventPublisher eventPublisher;
    private List<AgentTool> tools;

    @BeforeEach
    void setUp() {
        mockChatModel = new MockChatModel();
        eventPublisher = new AgentEventPublisher();
        tools = new ArrayList<>();
    }

    private AgentLoop createLoop(AgentConfig config, AgentSteering steering) {
        MessageTransformer transformer = messages -> messages;
        MessageConverter converter = new DefaultMessageConverter();
        return new AgentLoop(mockChatModel, config, tools, transformer, converter,
            null, eventPublisher, steering);
    }

    @Test
    @DisplayName("parallelMode: 多个工具调用并行执行")
    void testParallelExecution() {
        // 创建两个有延迟的工具
        DelayTool delayTool1 = new DelayTool("delay_tool_1", 300);
        DelayTool delayTool2 = new DelayTool("delay_tool_2", 300);
        tools.add(delayTool1);
        tools.add(delayTool2);

        // LLM一次返回两个工具调用
        mockChatModel.addToolCallResponse(List.of(
            new ToolCall("call_1", "delay_tool_1", "{}"),
            new ToolCall("call_2", "delay_tool_2", "{}")
        ));
        mockChatModel.addTextResponse("两个延迟工具执行完毕");

        AgentConfig config = AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(10)
            .toolExecutionMode(com.easyagent4j.core.tool.ToolExecutionMode.PARALLEL)
            .streamingEnabled(false)
            .build();

        AgentLoop loop = createLoop(config, null);
        AgentContext context = new AgentContext("test-session");

        long startTime = System.currentTimeMillis();
        UserMessage userMsg = new UserMessage("并行执行两个延迟工具");
        AgentContext result = loop.execute(userMsg, context);
        long elapsed = System.currentTimeMillis() - startTime;

        // 并行执行：两个300ms的工具应该约300ms完成，而非600ms
        // 给一些余量，500ms以内认为并行生效
        assertTrue(elapsed < 500,
            "Parallel execution should be faster than sequential. Elapsed: " + elapsed + "ms");

        // 两个工具都应该被调用
        assertEquals(1, delayTool1.getCallCount());
        assertEquals(1, delayTool2.getCallCount());

        // 验证结果：user(1) + assistant_toolcall(1) + tool_result(2) + assistant_final(1) = 5
        assertEquals(5, result.getMessageCount());
    }

    @Test
    @DisplayName("sequentialMode: 多个工具调用串行执行")
    void testSequentialExecution() {
        DelayTool delayTool1 = new DelayTool("delay_tool_1", 100);
        DelayTool delayTool2 = new DelayTool("delay_tool_2", 100);
        tools.add(delayTool1);
        tools.add(delayTool2);

        mockChatModel.addToolCallResponse(List.of(
            new ToolCall("call_1", "delay_tool_1", "{}"),
            new ToolCall("call_2", "delay_tool_2", "{}")
        ));
        mockChatModel.addTextResponse("串行执行完毕");

        AgentConfig config = AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(10)
            .toolExecutionMode(com.easyagent4j.core.tool.ToolExecutionMode.SEQUENTIAL)
            .streamingEnabled(false)
            .build();

        AgentLoop loop = createLoop(config, null);
        AgentContext context = new AgentContext("test-session");

        long startTime = System.currentTimeMillis();
        UserMessage userMsg = new UserMessage("串行执行两个延迟工具");
        AgentContext result = loop.execute(userMsg, context);
        long elapsed = System.currentTimeMillis() - startTime;

        // 串行执行：两个100ms的工具应该约200ms
        assertTrue(elapsed >= 150,
            "Sequential execution should take at least ~200ms. Elapsed: " + elapsed + "ms");

        assertEquals(1, delayTool1.getCallCount());
        assertEquals(1, delayTool2.getCallCount());
    }

    @Test
    @DisplayName("parallelMode单个工具调用无需并行")
    void testSingleToolCallNoParallelOverhead() {
        DelayTool delayTool = new DelayTool("delay_tool", 100);
        tools.add(delayTool);

        mockChatModel.addToolCallResponse("call_1", "delay_tool", "{}");
        mockChatModel.addTextResponse("单个工具执行完毕");

        AgentConfig config = AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(10)
            .toolExecutionMode(com.easyagent4j.core.tool.ToolExecutionMode.PARALLEL)
            .streamingEnabled(false)
            .build();

        AgentLoop loop = createLoop(config, null);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("执行单个工具");
        AgentContext result = loop.execute(userMsg, context);

        assertEquals(1, delayTool.getCallCount());
        // 验证结果：user(1) + assistant_toolcall(1) + tool_result(1) + assistant_final(1) = 4
        assertEquals(4, result.getMessageCount());
    }

    @Test
    @DisplayName("parallelMode: 一个工具抛异常不影响其他工具")
    void testParallelErrorIsolation() {
        // 一个正常工具和一个抛异常的工具
        DummyTool normalTool = new DummyTool("normal_tool", "A normal tool", "ok");
        FailingTool failTool = new FailingTool("failing_tool", "A tool that fails");
        tools.add(normalTool);
        tools.add(failTool);

        mockChatModel.addToolCallResponse(List.of(
            new ToolCall("call_1", "normal_tool", "{}"),
            new ToolCall("call_2", "failing_tool", "{}")
        ));
        mockChatModel.addTextResponse("部分工具失败但仍继续");

        AgentConfig config = AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(10)
            .toolExecutionMode(com.easyagent4j.core.tool.ToolExecutionMode.PARALLEL)
            .streamingEnabled(false)
            .build();

        AgentLoop loop = createLoop(config, null);
        AgentContext context = new AgentContext("test-session");

        // 不应抛出异常
        UserMessage userMsg = new UserMessage("执行两个工具（一个会失败）");
        AgentContext result = loop.execute(userMsg, context);

        // 正常工具应该被调用
        assertEquals(1, normalTool.getCallCount());

        // 两个工具结果都应该在上下文中
        long toolResultCount = result.getMessages().stream()
            .filter(m -> m.getRole() == MessageRole.TOOL_RESULT)
            .count();
        assertEquals(2, toolResultCount, "Both tool results should be in context");

        // 最终回复应该存在
        assertTrue(result.getMessageCount() >= 4);
    }

    /**
     * 会抛出异常的工具。
     */
    private static class FailingTool extends com.easyagent4j.core.tool.AbstractAgentTool {
        public FailingTool(String name, String description) {
            super(name, description);
        }

        @Override
        protected com.easyagent4j.core.tool.ToolResult doExecute(com.easyagent4j.core.tool.ToolContext context) {
            throw new RuntimeException("Tool execution failed intentionally");
        }
    }
}
