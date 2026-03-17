package com.easyagent4j.core.task;

/**
 * 任务状态枚举。
 */
public enum TaskStatus {
    /**
     * 规划中：正在生成任务计划
     */
    PLANNING,
    
    /**
     * 执行中：正在执行任务步骤
     */
    EXECUTING,
    
    /**
     * 审查中：正在审查执行结果
     */
    REVIEWING,
    
    /**
     * 完成：任务成功完成
     */
    DONE,
    
    /**
     * 失败：任务执行失败
     */
    FAILED
}