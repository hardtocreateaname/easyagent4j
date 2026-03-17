package com.easyagent4j.core.agent;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent转向机制 — 允许外部线程中断并改变Agent执行方向。
 *
 * 线程安全：所有操作使用原子引用。
 */
public class AgentSteering {

    private final AtomicReference<SteeringCommand> command = new AtomicReference<>(SteeringCommand.CONTINUE);
    private final AtomicReference<String> overrideSystemPrompt = new AtomicReference<>();

    /**
     * 转向：中断当前执行并设置新的system prompt。
     * AgentLoop在下一轮循环开始时检测到此命令，将替换system prompt并继续执行。
     *
     * @param newSystemPrompt 新的system prompt
     */
    public void steer(String newSystemPrompt) {
        overrideSystemPrompt.set(newSystemPrompt);
        command.set(SteeringCommand.STEER);
    }

    /**
     * 停止当前执行。
     * AgentLoop在下一轮循环开始时检测到此命令，将停止循环。
     */
    public void stop() {
        command.set(SteeringCommand.STOP);
    }

    /**
     * 清除所有覆盖，恢复正常执行。
     */
    public void clear() {
        command.set(SteeringCommand.CONTINUE);
        overrideSystemPrompt.set(null);
    }

    /**
     * 获取并清除当前转向命令（CAS操作）。
     * AgentLoop调用此方法检查是否需要转向。
     *
     * @return 当前转向命令
     */
    public SteeringCommand pollCommand() {
        return command.getAndSet(SteeringCommand.CONTINUE);
    }

    /**
     * 获取覆盖的system prompt（如果存在）。
     *
     * @return 覆盖的system prompt，或null
     */
    public String getOverrideSystemPrompt() {
        return overrideSystemPrompt.get();
    }

    /**
     * 获取并清除覆盖的system prompt。
     *
     * @return 覆盖的system prompt，或null
     */
    public String consumeOverrideSystemPrompt() {
        return overrideSystemPrompt.getAndSet(null);
    }

    /**
     * 检查当前是否有活跃的转向命令。
     *
     * @return true如果有活跃命令
     */
    public boolean hasActiveCommand() {
        return command.get() != SteeringCommand.CONTINUE;
    }
}
