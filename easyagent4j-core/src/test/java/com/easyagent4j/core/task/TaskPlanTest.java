package com.easyagent4j.core.task;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskPlan 单元测试。
 */
class TaskPlanTest {

    @Test
    void testCreateTaskPlan() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1"),
            new TaskStep("Step 2", "Action 2"),
            new TaskStep("Step 3", "Action 3")
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        assertEquals("Test Goal", plan.getGoal());
        assertEquals(3, plan.getSteps().size());
        assertEquals(0, plan.getCurrentStepIndex());
        assertEquals(TaskStatus.PLANNING, plan.getStatus());
    }

    @Test
    void testCompleteStep() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1"),
            new TaskStep("Step 2", "Action 2")
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        plan.completeStep("Result 1");
        
        assertEquals(1, plan.getCurrentStepIndex());
        assertTrue(plan.getSteps().get(0).completed());
        assertEquals("Result 1", plan.getSteps().get(0).result());
        assertFalse(plan.getSteps().get(1).completed());
    }

    @Test
    void testFailStep() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1"),
            new TaskStep("Step 2", "Action 2")
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        plan.failStep("Error 1");
        
        assertFalse(plan.getSteps().get(0).completed());
        assertEquals("Error 1", plan.getSteps().get(0).result());
    }

    @Test
    void testIncrementStepRetry() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1", 3)
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        assertEquals(0, plan.getSteps().get(0).retryCount());
        
        plan.incrementStepRetry();
        assertEquals(1, plan.getSteps().get(0).retryCount());
        
        plan.incrementStepRetry();
        assertEquals(2, plan.getSteps().get(0).retryCount());
    }

    @Test
    void testIsAllCompleted() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1"),
            new TaskStep("Step 2", "Action 2")
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        assertFalse(plan.isAllCompleted());
        
        plan.completeStep("Result 1");
        assertFalse(plan.isAllCompleted());
        
        plan.completeStep("Result 2");
        assertTrue(plan.isAllCompleted());
    }

    @Test
    void testGetNextPendingStep() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1"),
            new TaskStep("Step 2", "Action 2"),
            new TaskStep("Step 3", "Action 3")
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        Optional<TaskStep> nextStep = plan.getNextPendingStep();
        assertTrue(nextStep.isPresent());
        assertEquals("Step 1", nextStep.get().description());
        
        plan.completeStep("Result 1");
        nextStep = plan.getNextPendingStep();
        assertTrue(nextStep.isPresent());
        assertEquals("Step 2", nextStep.get().description());
        
        plan.completeStep("Result 2");
        plan.completeStep("Result 3");
        nextStep = plan.getNextPendingStep();
        assertFalse(nextStep.isPresent());
    }

    @Test
    void testGetCurrentStep() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1"),
            new TaskStep("Step 2", "Action 2")
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        Optional<TaskStep> currentStep = plan.getCurrentStep();
        assertTrue(currentStep.isPresent());
        assertEquals("Step 1", currentStep.get().description());
        
        plan.completeStep("Result 1");
        currentStep = plan.getCurrentStep();
        assertTrue(currentStep.isPresent());
        assertEquals("Step 2", currentStep.get().description());
        
        plan.completeStep("Result 2");
        currentStep = plan.getCurrentStep();
        assertFalse(currentStep.isPresent());
    }

    @Test
    void testSetStatus() {
        List<TaskStep> steps = List.of(
            new TaskStep("Step 1", "Action 1")
        );
        
        TaskPlan plan = new TaskPlan("Test Goal", steps);
        
        assertEquals(TaskStatus.PLANNING, plan.getStatus());
        
        plan.setStatus(TaskStatus.EXECUTING);
        assertEquals(TaskStatus.EXECUTING, plan.getStatus());
        
        plan.setStatus(TaskStatus.REVIEWING);
        assertEquals(TaskStatus.REVIEWING, plan.getStatus());
        
        plan.setStatus(TaskStatus.DONE);
        assertEquals(TaskStatus.DONE, plan.getStatus());
        
        plan.setStatus(TaskStatus.FAILED);
        assertEquals(TaskStatus.FAILED, plan.getStatus());
    }

    @Test
    void testTaskStepDefaultRetries() {
        TaskStep step = new TaskStep("Step 1", "Action 1");
        
        assertEquals(3, step.maxRetries());
        assertEquals(0, step.retryCount());
        assertFalse(step.completed());
        assertNull(step.result());
    }

    @Test
    void testTaskStepCustomRetries() {
        TaskStep step = new TaskStep("Step 1", "Action 1", 5);
        
        assertEquals(5, step.maxRetries());
        assertEquals(0, step.retryCount());
    }

    @Test
    void testTaskStepMarkCompleted() {
        TaskStep step = new TaskStep("Step 1", "Action 1");
        
        assertFalse(step.completed());
        
        TaskStep completedStep = step.markCompleted("Success");
        
        assertTrue(completedStep.completed());
        assertEquals("Success", completedStep.result());
    }

    @Test
    void testTaskStepMarkFailed() {
        TaskStep step = new TaskStep("Step 1", "Action 1");
        
        TaskStep failedStep = step.markFailed("Error");
        
        assertFalse(failedStep.completed());
        assertEquals("Error", failedStep.result());
    }

    @Test
    void testTaskStepIncrementRetry() {
        TaskStep step = new TaskStep("Step 1", "Action 1", 3);
        
        assertEquals(0, step.retryCount());
        assertTrue(step.canRetry());
        
        TaskStep retriedStep = step.incrementRetry();
        assertEquals(1, retriedStep.retryCount());
        assertTrue(retriedStep.canRetry());
        
        retriedStep = retriedStep.incrementRetry();
        assertEquals(2, retriedStep.retryCount());
        assertTrue(retriedStep.canRetry());
        
        retriedStep = retriedStep.incrementRetry();
        assertEquals(3, retriedStep.retryCount());
        assertFalse(retriedStep.canRetry());
    }

    @Test
    void testTaskStepImmutable() {
        TaskStep step = new TaskStep("Step 1", "Action 1");
        
        TaskStep completedStep = step.markCompleted("Success");
        
        // 原始对象不应该被修改
        assertFalse(step.completed());
        assertNull(step.result());
        
        // 新对象应该有修改
        assertTrue(completedStep.completed());
        assertEquals("Success", completedStep.result());
    }

    @Test
    void testEmptyTaskPlan() {
        TaskPlan plan = new TaskPlan("Empty Goal", List.of());
        
        assertTrue(plan.isAllCompleted());
        assertEquals(0, plan.getCurrentStepIndex());
        assertFalse(plan.getNextPendingStep().isPresent());
        assertFalse(plan.getCurrentStep().isPresent());
    }

    @Test
    void testSingleStepTaskPlan() {
        List<TaskStep> steps = List.of(
            new TaskStep("Single Step", "Single Action")
        );
        
        TaskPlan plan = new TaskPlan("Single Goal", steps);
        
        assertEquals(1, plan.getSteps().size());
        assertFalse(plan.isAllCompleted());
        
        plan.completeStep("Result");
        assertTrue(plan.isAllCompleted());
    }
}