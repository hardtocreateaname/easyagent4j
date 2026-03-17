package com.easyagent4j.core.task;

import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.agent.AgentLoop;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.context.MessageConverter;
import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.event.AgentEventPublisher;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.UserMessage;
import com.easyagent4j.core.resilience.RetryPolicy;
import com.easyagent4j.core.tool.AgentTool;
import com.easyagent4j.core.tool.ToolHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 自主执行循环。
 * 继承 AgentLoop，添加任务规划和自主执行能力。
 */
public class AutonomousAgentLoop extends AgentLoop {
    private static final Logger log = LoggerFactory.getLogger(AutonomousAgentLoop.class);
    
    private final TaskPlanner taskPlanner;
    private final RetryPolicy retryPolicy;
    private final ChatModel chatModel;

    public AutonomousAgentLoop(ChatModel chatModel, AgentConfig config, List<AgentTool> tools,
                               MessageTransformer transformer, MessageConverter converter,
                               ToolHook toolHook, AgentEventPublisher eventPublisher,
                               TaskPlanner taskPlanner, RetryPolicy retryPolicy) {
        super(chatModel, config, tools, transformer, converter, toolHook, eventPublisher, null, retryPolicy);
        this.chatModel = chatModel;
        this.taskPlanner = taskPlanner;
        this.retryPolicy = retryPolicy != null ? retryPolicy : new RetryPolicy();
    }

    /**
     * 执行自主任务。
     * 
     * @param goal 任务目标
     * @return 执行完成的上下文
     */
    public AgentContext executeAutonomous(String goal) {
        log.info("Starting autonomous execution for goal: {}", goal);
        
        AgentContext context = new AgentContext();
        
        // 步骤 1: 规划任务
        TaskPlan plan = taskPlanner.plan(goal, getTools());
        plan.setStatus(TaskStatus.EXECUTING);
        log.info("Created task plan with {} steps", plan.getSteps().size());
        
        // 步骤 2: 执行计划
        while (!plan.isAllCompleted() && plan.getStatus() != TaskStatus.FAILED) {
            Optional<TaskStep> nextStep = plan.getNextPendingStep();
            
            if (nextStep.isEmpty()) {
                break;
            }
            
            TaskStep step = nextStep.get();
            log.info("Executing step: {} - {}", step.description(), step.action());
            
            try {
                // 构建用户消息
                String stepPrompt = buildStepPrompt(goal, step, plan);
                UserMessage userMessage = new UserMessage(stepPrompt);
                
                // 调用父类 execute 处理步骤
                context = execute(userMessage, context);
                
                // 步骤成功完成
                String stepResult = extractStepResult(context);
                plan.completeStep(stepResult);
                log.info("Step completed: {}", step.description());
                
                // 步骤 3: 审查结果
                if (!reviewStepResult(plan, step, stepResult)) {
                    log.warn("Step review failed, replanning...");
                    plan = taskPlanner.replan(plan, "Step review failed: " + step.description());
                    plan.setStatus(TaskStatus.EXECUTING);
                }
                
            } catch (Exception e) {
                log.error("Step execution failed: {}", step.description(), e);
                
                // 重试逻辑
                if (step.canRetry()) {
                    plan.incrementStepRetry();
                    int retryDelay = (int) retryPolicy.getNextDelay(step.retryCount()).toMillis();
                    log.info("Retrying step {} after {}ms (attempt {}/{})", 
                        step.description(), retryDelay, step.retryCount(), step.maxRetries());
                    
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        plan.setStatus(TaskStatus.FAILED);
                        break;
                    }
                } else {
                    // 重试失败，重新规划
                    log.error("Max retries exceeded for step: {}", step.description());
                    plan.failStep(e.getMessage());
                    plan = taskPlanner.replan(plan, 
                        "Step failed after max retries: " + step.description() + 
                        ". Error: " + e.getMessage());
                    plan.setStatus(TaskStatus.EXECUTING);
                }
            }
        }
        
        // 步骤 4: 最终审查
        if (plan.isAllCompleted()) {
            plan.setStatus(TaskStatus.REVIEWING);
            if (reviewFinalResult(goal, plan, context)) {
                plan.setStatus(TaskStatus.DONE);
                log.info("Task completed successfully: {}", goal);
            } else {
                log.warn("Final review failed, task may not be fully completed");
                plan.setStatus(TaskStatus.FAILED);
            }
        } else {
            plan.setStatus(TaskStatus.FAILED);
            log.error("Task failed: {}", goal);
        }
        
        return context;
    }

    /**
     * 构建步骤执行提示。
     */
    private String buildStepPrompt(String goal, TaskStep step, TaskPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal).append("\n\n");
        sb.append("Current Progress:\n");
        
        List<TaskStep> steps = plan.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            TaskStep s = steps.get(i);
            sb.append("Step ").append(i + 1).append(": ").append(s.description());
            if (s.completed()) {
                sb.append(" [DONE]");
            } else if (s == step) {
                sb.append(" [CURRENT]");
            }
            sb.append("\n");
        }
        
        sb.append("\n");
        sb.append("Current Step: ").append(step.description()).append("\n");
        sb.append("Action: ").append(step.action()).append("\n");
        sb.append("\n");
        sb.append("Please execute this step using the available tools.");
        
        return sb.toString();
    }

    /**
     * 从上下文中提取步骤结果。
     */
    private String extractStepResult(AgentContext context) {
        // 获取最后一条助手消息作为步骤结果
        if (context.getMessages().isEmpty()) {
            return "No result";
        }
        
        return context.getMessages().get(context.getMessages().size() - 1).toString();
    }

    /**
     * 审查单个步骤的执行结果。
     */
    private boolean reviewStepResult(TaskPlan plan, TaskStep step, String result) {
        try {
            String prompt = buildReviewPrompt(plan.getGoal(), step, result);
            
            ChatRequest request = ChatRequest.builder()
                .messages(List.of(Map.of("role", "user", "content", prompt)))
                .systemPrompt("You are a task reviewer. Evaluate if a step was completed successfully.")
                .streaming(false)
                .build();
            
            ChatResponse response = chatModel.call(request);
            String responseText = response.getText();
            
            // 检查响应中是否包含肯定结果
            return responseText != null && 
                (responseText.toLowerCase().contains("yes") || 
                 responseText.toLowerCase().contains("success") ||
                 responseText.toLowerCase().contains("completed"));
            
        } catch (Exception e) {
            log.error("Failed to review step result: {}", e.getMessage(), e);
            // 审查失败时，保守地认为步骤成功
            return true;
        }
    }

    /**
     * 审查最终结果。
     */
    private boolean reviewFinalResult(String goal, TaskPlan plan, AgentContext context) {
        try {
            String prompt = buildFinalReviewPrompt(goal, plan, context);
            
            ChatRequest request = ChatRequest.builder()
                .messages(List.of(Map.of("role", "user", "content", prompt)))
                .systemPrompt("You are a task reviewer. Evaluate if the entire task was completed successfully.")
                .streaming(false)
                .build();
            
            ChatResponse response = chatModel.call(request);
            String responseText = response.getText();
            
            return responseText != null && 
                (responseText.toLowerCase().contains("yes") || 
                 responseText.toLowerCase().contains("success") ||
                 responseText.toLowerCase().contains("completed"));
            
        } catch (Exception e) {
            log.error("Failed to review final result: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建步骤审查提示。
     */
    private String buildReviewPrompt(String goal, TaskStep step, String result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal).append("\n\n");
        sb.append("Step: ").append(step.description()).append("\n");
        sb.append("Action: ").append(step.action()).append("\n\n");
        sb.append("Result: ").append(result).append("\n\n");
        sb.append("Was this step completed successfully? Respond with 'yes' or 'no' and explain briefly.");
        
        return sb.toString();
    }

    /**
     * 构建最终审查提示。
     */
    private String buildFinalReviewPrompt(String goal, TaskPlan plan, AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(goal).append("\n\n");
        sb.append("Completed Steps:\n");
        
        List<TaskStep> steps = plan.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            TaskStep step = steps.get(i);
            sb.append("Step ").append(i + 1).append(": ").append(step.description());
            if (step.completed()) {
                sb.append(" [DONE] - ").append(step.result());
            }
            sb.append("\n");
        }
        
        sb.append("\nFinal Context: ").append(context.getMessages()).append("\n\n");
        sb.append("Was the entire task completed successfully? Respond with 'yes' or 'no' and explain briefly.");
        
        return sb.toString();
    }

    /**
     * 获取工具列表。
     */
    private List<AgentTool> getTools() {
        // 通过反射获取父类的 tools 字段
        try {
            java.lang.reflect.Field field = AgentLoop.class.getDeclaredField("tools");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<AgentTool> tools = (List<AgentTool>) field.get(this);
            return tools;
        } catch (Exception e) {
            log.error("Failed to get tools: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
