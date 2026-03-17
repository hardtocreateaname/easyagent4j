package com.easyagent4j.core.agent;

/**
 * 转向命令枚举 — 控制Agent循环的执行方向。
 */
public enum SteeringCommand {
    /**
     * 继续正常执行。
     */
    CONTINUE,

    /**
     * 停止当前执行循环。
     */
    STOP,

    /**
     * 使用新的system prompt转向（替换system prompt后继续）。
     */
    STEER
}
