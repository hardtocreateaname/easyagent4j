package com.easyagent4j.core.agent;

import com.easyagent4j.core.*;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.context.MessageConverter;
import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.context.converter.DefaultMessageConverter;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.event.AgentEventPublisher;
import com.easyagent4j.core.event.events.*;
import com.easyagent4j.core.message.*;
import com.easyagent4j.core.tool.AgentTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentLoop 集成测试 — 使用 MockChatModel 测试完整的 Agent 循环。
 * 不依赖真实 LLM，所有行为通过 Mock 控制。
 */
class AgentLoopIntegrationTest {

    private MockChatModel mockChatModel;
    private RecordingEventListener eventListener;
    private AgentEventPublisher eventPublisher;
    private List<AgentTool> tools;
    private DummyTool dummyTool;

    @BeforeEach
    void setUp() {
        mockChatModel = new MockChatModel();
        eventListener = new RecordingEventListener();
        eventPublisher = new AgentEventPublisher();
        eventPublisher.subscribe(eventListener);
        dummyTool = new DummyTool("test_tool", "A test tool", "tool_result_ok");
        tools = new ArrayList<>();
        tools.add(dummyTool);
    }

    private AgentLoop createLoop(AgentConfig config, AgentSteering steering) {
        MessageTransformer transformer = messages -> messages;
        MessageConverter converter = new DefaultMessageConverter();
        return new AgentLoop(mockChatModel, config, tools, transformer, converter,
            null, eventPublisher, steering);
    }

    private AgentLoop createLoop(AgentConfig config) {
        return createLoop(config, null);
    }

    private AgentConfig defaultConfig() {
        return AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(10)
            .streamingEnabled(false)
            .build();
    }

    // ========== Test 1: Simple Conversation ==========

    @Test
    @DisplayName("testSimpleConversation: 纯文本对话，验证上下文消息数")
    void testSimpleConversation() {
        mockChatModel.addTextResponse("你好！有什么可以帮你的吗？");
        AgentConfig config = defaultConfig();
        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("你好");
        AgentContext result = loop.execute(userMsg, context);

        // 验证上下文有2条消息（user + assistant）
        assertEquals(2, result.getMessageCount());

        // 验证消息类型
        assertEquals(MessageRole.USER, result.getMessages().get(0).getRole());
        assertEquals(MessageRole.ASSISTANT, result.getMessages().get(1).getRole());

        // 验证回复内容
        AssistantMessage assistantMsg = (AssistantMessage) result.getMessages().get(1);
        assertEquals("你好！有什么可以帮你的吗？", assistantMsg.getTextContent());
        assertFalse(assistantMsg.hasToolCalls());
    }

    // ========== Test 2: Tool Execution ==========

    @Test
    @DisplayName("testToolExecution: LLM请求工具调用 → 执行工具 → 最终文本回复")
    void testToolExecution() {
        // 第一次调用：LLM请求工具调用
        mockChatModel.addToolCallResponse("call_1", "test_tool", "{}");
        // 第二次调用：LLM根据工具结果返回文本
        mockChatModel.addTextResponse("工具执行结果如下：tool_result_ok");

        AgentConfig config = defaultConfig();
        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("请执行test_tool");
        AgentContext result = loop.execute(userMsg, context);

        // 验证上下文有4条消息：user + assistant(tool_call) + tool_result + assistant(text)
        assertEquals(4, result.getMessageCount());

        // 验证消息类型序列
        assertEquals(MessageRole.USER, result.getMessages().get(0).getRole());
        assertEquals(MessageRole.ASSISTANT, result.getMessages().get(1).getRole());
        assertEquals(MessageRole.TOOL_RESULT, result.getMessages().get(2).getRole());
        assertEquals(MessageRole.ASSISTANT, result.getMessages().get(3).getRole());

        // 验证第一次assistant消息包含工具调用
        AssistantMessage firstAssistant = (AssistantMessage) result.getMessages().get(1);
        assertTrue(firstAssistant.hasToolCalls());
        assertEquals("test_tool", firstAssistant.getToolCalls().get(0).getName());

        // 验证工具被调用了
        assertEquals(1, dummyTool.getCallCount());

        // 验证工具结果
        ToolResultMessage toolResult = (ToolResultMessage) result.getMessages().get(2);
        assertEquals("tool_result_ok", toolResult.getResultContent());
        assertFalse(toolResult.isError());

        // 验证最终回复
        AssistantMessage finalAssistant = (AssistantMessage) result.getMessages().get(3);
        assertEquals("工具执行结果如下：tool_result_ok", finalAssistant.getTextContent());
        assertFalse(finalAssistant.hasToolCalls());
    }

    @Test
    @DisplayName("testSteeringAfterToolExecution: tool结束后优先消费steering消息")
    void testSteeringAfterToolExecution() {
        mockChatModel.addToolCallResponse("call_1", "test_tool", "{}");
        mockChatModel.addTextResponse("已按新的方向继续处理");

        AgentConfig config = defaultConfig();
        AgentSteering steering = new AgentSteering();
        steering.steer("改为输出排查结论");

        AgentLoop loop = createLoop(config, steering);
        AgentContext context = new AgentContext("test-session");

        AgentContext result = loop.execute(new UserMessage("先执行工具"), context);

        assertEquals(5, result.getMessageCount());
        assertEquals(MessageRole.USER, result.getMessages().get(3).getRole());
        assertEquals("改为输出排查结论", ((UserMessage) result.getMessages().get(3)).getTextContent());
        assertEquals("已按新的方向继续处理",
            ((AssistantMessage) result.getMessages().get(4)).getTextContent());
    }

    @Test
    @DisplayName("testFollowUpAfterPlainText: 无tool call时消费follow-up消息")
    void testFollowUpAfterPlainText() {
        mockChatModel.addTextResponse("第一轮先确认状态");
        mockChatModel.addTextResponse("第二轮处理follow-up");

        AgentConfig config = defaultConfig();
        AgentSteering steering = new AgentSteering();
        steering.followUp("继续输出最终总结");

        AgentLoop loop = createLoop(config, steering);
        AgentContext context = new AgentContext("test-session");

        AgentContext result = loop.execute(new UserMessage("开始"), context);

        assertEquals(4, result.getMessageCount());
        assertEquals(MessageRole.USER, result.getMessages().get(2).getRole());
        assertEquals("继续输出最终总结", ((UserMessage) result.getMessages().get(2)).getTextContent());
        assertEquals("第二轮处理follow-up",
            ((AssistantMessage) result.getMessages().get(3)).getTextContent());
    }

    // ========== Test 3: Max Iterations ==========

    @Test
    @DisplayName("testMaxIterations: LLM总是请求工具调用，验证达到最大迭代次数时停止")
    void testMaxIterations() {
        // LLM总是请求工具调用，连续3次
        mockChatModel.addToolCallResponse("call_1", "test_tool", "{}");
        mockChatModel.addToolCallResponse("call_2", "test_tool", "{}");
        mockChatModel.addToolCallResponse("call_3", "test_tool", "{}");

        AgentConfig config = AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(2)  // 最多2次工具迭代
            .streamingEnabled(false)
            .build();

        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("请执行test_tool");
        AgentContext result = loop.execute(userMsg, context);

        // 验证工具只被调用了2次（maxToolIterations=2）
        assertEquals(2, dummyTool.getCallCount());

        // 验证收到了error事件
        long errorCount = eventListener.getEvents().stream()
            .filter(e -> e instanceof ErrorEvent)
            .count();
        assertTrue(errorCount > 0, "Expected at least one ErrorEvent when max iterations exceeded");

        // 验证error消息
        ErrorEvent errorEvent = (ErrorEvent) eventListener.getEvents().stream()
            .filter(e -> e instanceof ErrorEvent)
            .findFirst().orElseThrow();
        assertTrue(errorEvent.getErrorMessage().contains("Max tool iterations"),
            "Error message should mention max iterations: " + errorEvent.getErrorMessage());
    }

    // ========== Test 4: Streaming ==========

    @Test
    @DisplayName("testStreaming: 验证stream模式下MessageUpdateEvent被正确发布")
    void testStreaming() {
        mockChatModel.addTextResponse("这是一段流式回复的内容");

        AgentConfig config = AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(10)
            .streamingEnabled(true)  // 启用流式
            .build();

        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("你好");
        AgentContext result = loop.execute(userMsg, context);

        // 验证收到MessageUpdateEvent
        long updateCount = eventListener.getEvents().stream()
            .filter(e -> e instanceof MessageUpdateEvent)
            .count();
        assertTrue(updateCount > 0, "Expected at least one MessageUpdateEvent in streaming mode");

        // 验证回复内容正确
        AssistantMessage assistantMsg = (AssistantMessage) result.getMessages().get(1);
        assertEquals("这是一段流式回复的内容", assistantMsg.getTextContent());
    }

    // ========== Test 5: Abort ==========

    @Test
    @DisplayName("testAbort: 启动agent循环后调用abort()，验证中断")
    void testAbort() throws Exception {
        // LLM总是请求工具调用，会持续循环
        // 使用延迟工具让循环有足够的时间等待abort
        tools.clear();
        tools.add(new DelayTool("slow_tool", 500));

        mockChatModel.addToolCallResponse("call_1", "slow_tool", "{}");
        mockChatModel.addToolCallResponse("call_2", "slow_tool", "{}");
        mockChatModel.addToolCallResponse("call_3", "slow_tool", "{}");

        AgentConfig config = AgentConfig.builder()
            .systemPrompt("You are a test assistant.")
            .maxToolIterations(10)
            .streamingEnabled(false)
            .build();

        AgentSteering steering = new AgentSteering();
        AgentLoop loop = createLoop(config, steering);
        AgentContext context = new AgentContext("test-session");

        // 在另一个线程中执行agent循环
        CompletableFuture<AgentContext> future = CompletableFuture.supplyAsync(() -> {
            UserMessage userMsg = new UserMessage("请执行slow_tool");
            return loop.execute(userMsg, context);
        });

        // 等待一小段时间让第一次工具执行开始
        Thread.sleep(100);

        // 调用abort
        loop.setAborted(true);

        // 等待执行完成
        AgentContext result = future.get(5, TimeUnit.SECONDS);

        // 验证收到了aborted相关的error事件或迭代次数少于最大值
        boolean hasAbortError = eventListener.getEvents().stream()
            .filter(e -> e instanceof ErrorEvent)
            .map(e -> ((ErrorEvent) e).getErrorMessage())
            .anyMatch(msg -> msg.toLowerCase().contains("aborted"));

        // 或者验证工具调用次数少于最大迭代次数（证明被中断）
        DelayTool slowTool = (DelayTool) tools.get(0);
        assertTrue(hasAbortError || slowTool.getCallCount() < 3,
            "Expected abort error or fewer than 3 tool calls. Events: " + eventListener.getEventTypeSequence());
    }

    // ========== Test 6: Event Flow ==========

    @Test
    @DisplayName("testEventFlow: 验证完整事件流序列")
    void testEventFlow() {
        mockChatModel.addTextResponse("你好！");

        AgentConfig config = defaultConfig();
        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("你好");
        loop.execute(userMsg, context);

        List<String> eventTypes = eventListener.getEventTypeSequence();

        // 打印事件序列（便于调试）
        System.out.println("Event sequence: " + eventTypes);

        // 验证关键事件顺序
        // 预期：message_start(user) → message_end(user) → turn_start → message_start(assistant) → message_end(assistant) → turn_end
        assertTrue(eventTypes.contains("message_start"), "Should have message_start event");
        assertTrue(eventTypes.contains("message_end"), "Should have message_end event");
        assertTrue(eventTypes.contains("turn_start"), "Should have turn_start event");
        assertTrue(eventTypes.contains("turn_end"), "Should have turn_end event");

        // 验证message_start在turn_start之前（至少第一个message_start应该在第一个turn_start之前）
        int firstMessageStart = eventTypes.indexOf("message_start");
        int firstTurnStart = eventTypes.indexOf("turn_start");
        assertTrue(firstMessageStart >= 0 && firstTurnStart > firstMessageStart,
            "First turn_start should come after first message_start");
    }

    @Test
    @DisplayName("testEventFlowWithTools: 工具调用场景的完整事件流")
    void testEventFlowWithTools() {
        mockChatModel.addToolCallResponse("call_1", "test_tool", "{}");
        mockChatModel.addTextResponse("工具执行完毕");

        AgentConfig config = defaultConfig();
        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("执行test_tool");
        loop.execute(userMsg, context);

        List<String> eventTypes = eventListener.getEventTypeSequence();
        System.out.println("Tool event sequence: " + eventTypes);

        // 验证工具相关事件
        assertTrue(eventTypes.contains("tool_execution_start"), "Should have tool_execution_start");
        assertTrue(eventTypes.contains("tool_execution_end"), "Should have tool_execution_end");

        // 验证turn_end
        assertTrue(eventTypes.contains("turn_end"), "Should have turn_end");
    }

    @Test
    @DisplayName("testToolNotFound: 请求不存在的工具，验证优雅处理")
    void testToolNotFound() {
        // LLM请求一个不存在的工具
        mockChatModel.addToolCallResponse("call_1", "nonexistent_tool", "{}");
        mockChatModel.addTextResponse("工具不存在");

        AgentConfig config = defaultConfig();
        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("请执行nonexistent_tool");
        AgentContext result = loop.execute(userMsg, context);

        // 验证上下文有4条消息（即使工具不存在，也会返回错误结果并继续）
        assertEquals(4, result.getMessageCount());

        // 验证工具结果消息是错误
        ToolResultMessage toolResult = (ToolResultMessage) result.getMessages().get(2);
        assertTrue(toolResult.isError());
        assertTrue(toolResult.getResultContent().contains("Tool not found"));

        // dummyTool不应被调用
        assertEquals(0, dummyTool.getCallCount());
    }

    @Test
    @DisplayName("testMultipleToolCalls: LLM一次返回多个工具调用")
    void testMultipleToolCalls() {
        // 添加第二个工具
        DummyTool tool2 = new DummyTool("test_tool_2", "Second test tool", "tool2_result");
        tools.add(tool2);

        // LLM一次返回两个工具调用
        mockChatModel.addToolCallResponse(List.of(
            new ToolCall("call_1", "test_tool", "{}"),
            new ToolCall("call_2", "test_tool_2", "{}")
        ));
        mockChatModel.addTextResponse("两个工具都执行完毕");

        AgentConfig config = defaultConfig();  // 默认PARALLEL模式
        AgentLoop loop = createLoop(config);
        AgentContext context = new AgentContext("test-session");

        UserMessage userMsg = new UserMessage("请执行两个工具");
        AgentContext result = loop.execute(userMsg, context);

        // 验证两个工具都被调用
        assertEquals(1, dummyTool.getCallCount());
        assertEquals(1, tool2.getCallCount());

        // 验证有2条tool_result消息
        long toolResultCount = result.getMessages().stream()
            .filter(m -> m.getRole() == MessageRole.TOOL_RESULT)
            .count();
        assertEquals(2, toolResultCount);
    }
}
