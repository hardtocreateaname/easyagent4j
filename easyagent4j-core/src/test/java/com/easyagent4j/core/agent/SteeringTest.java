package com.easyagent4j.core.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentSteering 单元测试。
 */
class SteeringTest {

    private AgentSteering steering;

    @BeforeEach
    void setUp() {
        steering = new AgentSteering();
    }

    @Test
    @DisplayName("初始状态为CONTINUE")
    void testInitialState() {
        assertFalse(steering.hasActiveCommand());
        assertNull(steering.getOverrideSystemPrompt());
    }

    @Test
    @DisplayName("stop()设置STOP命令")
    void testStop() {
        steering.stop();
        assertTrue(steering.hasActiveCommand());

        SteeringCommand cmd = steering.pollCommand();
        assertEquals(SteeringCommand.STOP, cmd);

        // pollCommand应该清除命令
        assertFalse(steering.hasActiveCommand());
        assertNull(steering.getOverrideSystemPrompt());
    }

    @Test
    @DisplayName("steer()将消息加入steering队列")
    void testSteer() {
        String newPrompt = "切换到代码模式";
        steering.steer(newPrompt);
        assertTrue(steering.hasActiveCommand());
        assertEquals(newPrompt, steering.getOverrideSystemPrompt());

        SteeringCommand cmd = steering.pollCommand();
        assertEquals(SteeringCommand.STEER, cmd);

        // pollCommand不消费消息，待处理的steering仍然存在
        assertTrue(steering.hasActiveCommand());
        assertEquals(newPrompt, steering.getOverrideSystemPrompt());

        // consumeOverrideSystemPrompt消费steering消息
        String consumed = steering.consumeOverrideSystemPrompt();
        assertEquals(newPrompt, consumed);
        assertFalse(steering.hasActiveCommand());
        assertNull(steering.getOverrideSystemPrompt());
    }

    @Test
    @DisplayName("clear()重置所有状态")
    void testClear() {
        steering.steer("new prompt");
        steering.clear();

        assertFalse(steering.hasActiveCommand());
        assertNull(steering.getOverrideSystemPrompt());
    }

    @Test
    @DisplayName("多次steer按队列顺序保留所有消息")
    void testMultipleSteer() {
        steering.steer("prompt1");
        steering.steer("prompt2");
        steering.steer("prompt3");

        assertEquals("prompt1", steering.getOverrideSystemPrompt());
        assertEquals("prompt1", steering.consumeOverrideSystemPrompt());
        assertEquals("prompt2", steering.consumeOverrideSystemPrompt());
        assertEquals("prompt3", steering.consumeOverrideSystemPrompt());
    }

    @Test
    @DisplayName("stop覆盖steer命令")
    void testStopOverridesSteer() {
        steering.steer("some prompt");
        steering.stop();

        SteeringCommand cmd = steering.pollCommand();
        assertEquals(SteeringCommand.STOP, cmd);

        // stop不清除待处理的steering消息
        assertEquals("some prompt", steering.getOverrideSystemPrompt());
    }

    @Test
    @DisplayName("pollCommand是原子操作，多次poll返回CONTINUE")
    void testMultiplePoll() {
        steering.stop();

        SteeringCommand first = steering.pollCommand();
        assertEquals(SteeringCommand.STOP, first);

        SteeringCommand second = steering.pollCommand();
        assertEquals(SteeringCommand.CONTINUE, second);

        SteeringCommand third = steering.pollCommand();
        assertEquals(SteeringCommand.CONTINUE, third);
    }

    @Test
    @DisplayName("consumeOverrideSystemPrompt是原子操作")
    void testConsumeOverrideSystemPrompt() {
        assertNull(steering.consumeOverrideSystemPrompt());

        steering.steer("test prompt");
        assertEquals("test prompt", steering.consumeOverrideSystemPrompt());
        assertNull(steering.consumeOverrideSystemPrompt());
    }

    @Test
    @DisplayName("典型使用场景：AgentLoop检查转向")
    void testTypicalUsage() {
        // 初始检查：无命令
        SteeringCommand cmd = steering.pollCommand();
        assertEquals(SteeringCommand.CONTINUE, cmd);

        // 外部调用steer
        steering.steer("切换到代码模式");

        // 下一轮循环检查
        cmd = steering.pollCommand();
        assertEquals(SteeringCommand.STEER, cmd);

        // 消费steering消息
        String override = steering.consumeOverrideSystemPrompt();
        assertEquals("切换到代码模式", override);

        // 之后的循环检查：无命令
        cmd = steering.pollCommand();
        assertEquals(SteeringCommand.CONTINUE, cmd);
    }

    @Test
    @DisplayName("followUp单独排队，不会触发STEER命令")
    void testFollowUpQueue() {
        steering.followUp("继续处理剩余任务");

        assertFalse(steering.hasActiveCommand());
        assertEquals(SteeringCommand.CONTINUE, steering.pollCommand());
        assertNull(steering.pollSteeringMessage());
        assertNotNull(steering.pollFollowUpMessage());
    }
}
