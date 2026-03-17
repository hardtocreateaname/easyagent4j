package com.easyagent4j.core.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 任务计划。
 * 包含任务目标、步骤列表和当前执行状态。
 */
public class TaskPlan {
    private final String goal;
    private final List<TaskStep> steps;
    private int currentStepIndex;
    private TaskStatus status;

    public TaskPlan(String goal, List<TaskStep> steps) {
        this.goal = goal;
        this.steps = new ArrayList<>(steps);
        this.currentStepIndex = 0;
        this.status = TaskStatus.PLANNING;
    }

    public String getGoal() {
        return goal;
    }

    public List<TaskStep> getSteps() {
        return new ArrayList<>(steps);
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * 完成当前步骤。
     */
    public void completeStep(String result) {
        if (currentStepIndex < steps.size()) {
            steps.set(currentStepIndex, steps.get(currentStepIndex).markCompleted(result));
            currentStepIndex++;
        }
    }

    /**
     * 标记当前步骤失败。
     */
    public void failStep(String error) {
        if (currentStepIndex < steps.size()) {
            steps.set(currentStepIndex, steps.get(currentStepIndex).markFailed(error));
        }
    }

    /**
     * 增加当前步骤的重试次数。
     */
    public void incrementStepRetry() {
        if (currentStepIndex < steps.size()) {
            steps.set(currentStepIndex, steps.get(currentStepIndex).incrementRetry());
        }
    }

    /**
     * 检查是否所有步骤都已完成。
     */
    public boolean isAllCompleted() {
        return steps.stream().allMatch(TaskStep::completed);
    }

    /**
     * 获取下一个待执行的步骤。
     */
    public Optional<TaskStep> getNextPendingStep() {
        return steps.stream()
            .filter(step -> !step.completed())
            .findFirst();
    }

    /**
     * 获取当前步骤。
     */
    public Optional<TaskStep> getCurrentStep() {
        if (currentStepIndex < steps.size()) {
            return Optional.of(steps.get(currentStepIndex));
        }
        return Optional.empty();
    }
}