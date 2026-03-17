package com.easyagent4j.core.task;

import com.easyagent4j.core.tool.AgentTool;

import java.util.List;

/**
 * 任务规划器接口。
 * 负责将用户目标分解为可执行的步骤计划。
 */
public interface TaskPlanner {
    
    /**
     * 根据目标规划任务步骤。
     * 
     * @param goal 用户目标
     * @param tools 可用工具列表
     * @return 任务计划
     */
    TaskPlan plan(String goal, List<AgentTool> tools);
    
    /**
     * 根据失败原因重新规划任务。
     * 
     * @param originalPlan 原始计划
     * @param failure 失败原因
     * @return 重新规划的任务计划
     */
    TaskPlan replan(TaskPlan originalPlan, String failure);
}