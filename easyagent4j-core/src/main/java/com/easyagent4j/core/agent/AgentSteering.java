package com.easyagent4j.core.agent;

import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.UserMessage;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent转向机制 — 允许外部线程中断并改变Agent执行方向。
 *
 * 线程安全：所有操作使用原子引用。
 */
public class AgentSteering {

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final Queue<AgentMessage> steeringMessages = new ConcurrentLinkedQueue<>();
    private final Queue<AgentMessage> followUpMessages = new ConcurrentLinkedQueue<>();

    /**
     * 转向：插入一条高优先级用户消息。
     * AgentLoop在当前轮次结束后优先消费该消息，再进入下一轮推理。
     *
     * @param message 转向消息
     */
    public void steer(String message) {
        steer(new UserMessage(message));
    }

    /**
     * 转向：插入一条高优先级消息。
     *
     * @param message 转向消息
     */
    public void steer(AgentMessage message) {
        if (message != null) {
            steeringMessages.add(message);
        }
    }

    /**
     * 添加普通后续消息。
     * 仅当当前轮没有工具调用且不存在待处理的steering消息时才会消费。
     *
     * @param message 后续消息
     */
    public void followUp(String message) {
        followUp(new UserMessage(message));
    }

    /**
     * 添加普通后续消息。
     *
     * @param message 后续消息
     */
    public void followUp(AgentMessage message) {
        if (message != null) {
            followUpMessages.add(message);
        }
    }

    /**
     * 停止当前执行。
     * AgentLoop在当前边界点检测到此信号后停止循环。
     */
    public void stop() {
        stopRequested.set(true);
    }

    /**
     * 清除所有覆盖，恢复正常执行。
     */
    public void clear() {
        stopRequested.set(false);
        steeringMessages.clear();
        followUpMessages.clear();
    }

    /**
     * 获取当前转向命令快照。
     * 为兼容旧接口保留：STOP优先，其次是存在待处理steering消息。
     *
     * @return 当前转向命令
     */
    public SteeringCommand pollCommand() {
        if (stopRequested.getAndSet(false)) {
            return SteeringCommand.STOP;
        }
        return steeringMessages.isEmpty() ? SteeringCommand.CONTINUE : SteeringCommand.STEER;
    }

    /**
     * 轮询停止信号。
     *
     * @return true如果检测到停止请求
     */
    public boolean pollStopRequested() {
        return stopRequested.getAndSet(false);
    }

    /**
     * 获取并移除下一条steering消息。
     *
     * @return 下一条steering消息，或null
     */
    public AgentMessage pollSteeringMessage() {
        return steeringMessages.poll();
    }

    /**
     * 获取并移除下一条follow-up消息。
     *
     * @return 下一条follow-up消息，或null
     */
    public AgentMessage pollFollowUpMessage() {
        return followUpMessages.poll();
    }

    /**
     * 获取待处理的steering文本。
     * 为兼容旧接口保留；仅在消息为UserMessage时返回文本。
     *
     * @return 待处理的steering文本，或null
     */
    public String getOverrideSystemPrompt() {
        AgentMessage message = steeringMessages.peek();
        if (message instanceof UserMessage userMessage) {
            return userMessage.getTextContent();
        }
        return null;
    }

    /**
     * 获取并移除下一条steering文本。
     * 为兼容旧接口保留；仅在消息为UserMessage时返回文本。
     *
     * @return 待处理的steering文本，或null
     */
    public String consumeOverrideSystemPrompt() {
        AgentMessage message = steeringMessages.poll();
        if (message instanceof UserMessage userMessage) {
            return userMessage.getTextContent();
        }
        return null;
    }

    /**
     * 检查当前是否有活跃的转向命令。
     *
     * @return true如果有活跃命令
     */
    public boolean hasActiveCommand() {
        return stopRequested.get() || !steeringMessages.isEmpty();
    }
}
