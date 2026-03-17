package com.easyagent4j.core.task;

import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.tool.AgentTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认任务规划器实现。
 * 使用 LLM 将目标分解为可执行的步骤列表。
 */
public class DefaultTaskPlanner implements TaskPlanner {
    private static final Logger log = LoggerFactory.getLogger(DefaultTaskPlanner.class);
    
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public DefaultTaskPlanner(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public TaskPlan plan(String goal, List<AgentTool> tools) {
        String prompt = buildPlanningPrompt(goal, tools);
        
        try {
            ChatRequest request = ChatRequest.builder()
                .messages(List.of(Map.of("role", "user", "content", prompt)))
                .systemPrompt("You are a task planning assistant. Break down goals into executable steps.")
                .streaming(false)
                .build();
            
            ChatResponse response = chatModel.call(request);
            String responseText = response.getText();
            
            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from LLM for planning, creating default plan");
                return createDefaultPlan(goal);
            }
            
            // 提取 JSON 部分
            String jsonContent = extractJsonContent(responseText);
            if (jsonContent == null) {
                log.warn("Failed to extract JSON from LLM response: {}", responseText);
                return createDefaultPlan(goal);
            }
            
            List<StepData> stepDataList = objectMapper.readValue(jsonContent, new TypeReference<List<StepData>>() {});
            List<TaskStep> steps = new ArrayList<>();
            
            for (StepData data : stepDataList) {
                steps.add(new TaskStep(
                    data.description(),
                    data.action(),
                    3  // default maxRetries
                ));
            }
            
            return new TaskPlan(goal, steps);
            
        } catch (Exception e) {
            log.error("Failed to create task plan: {}", e.getMessage(), e);
            return createDefaultPlan(goal);
        }
    }

    @Override
    public TaskPlan replan(TaskPlan originalPlan, String failure) {
        String prompt = buildReplanningPrompt(originalPlan, failure);
        
        try {
            ChatRequest request = ChatRequest.builder()
                .messages(List.of(Map.of("role", "user", "content", prompt)))
                .systemPrompt("You are a task planning assistant. Adjust plans based on failures.")
                .streaming(false)
                .build();
            
            ChatResponse response = chatModel.call(request);
            String responseText = response.getText();
            
            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from LLM for replanning, using original plan");
                return originalPlan;
            }
            
            // 提取 JSON 部分
            String jsonContent = extractJsonContent(responseText);
            if (jsonContent == null) {
                log.warn("Failed to extract JSON from LLM replanning response: {}", responseText);
                return originalPlan;
            }
            
            List<StepData> stepDataList = objectMapper.readValue(jsonContent, new TypeReference<List<StepData>>() {});
            List<TaskStep> steps = new ArrayList<>();
            
            // 保留已完成的步骤
            for (int i = 0; i < originalPlan.getCurrentStepIndex(); i++) {
                steps.add(originalPlan.getSteps().get(i));
            }
            
            // 添加新的步骤
            for (StepData data : stepDataList) {
                steps.add(new TaskStep(
                    data.description(),
                    data.action(),
                    3  // default maxRetries
                ));
            }
            
            TaskPlan newPlan = new TaskPlan(originalPlan.getGoal(), steps);
            newPlan.setStatus(TaskStatus.EXECUTING);
            return newPlan;
            
        } catch (Exception e) {
            log.error("Failed to replan: {}", e.getMessage(), e);
            return originalPlan;
        }
    }

    private String buildPlanningPrompt(String goal, List<AgentTool> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a task planning assistant. Your job is to break down a goal into executable steps.\n\n");
        sb.append("Goal: ").append(goal).append("\n\n");
        
        if (tools != null && !tools.isEmpty()) {
            sb.append("Available tools:\n");
            for (AgentTool tool : tools) {
                sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("Break down the goal into 3-7 clear, executable steps. For each step, provide:\n");
        sb.append("- description: what needs to be done\n");
        sb.append("- action: specific action to perform (can reference available tools)\n\n");
        
        sb.append("Respond ONLY with a JSON array in this exact format:\n");
        sb.append("[\n");
        sb.append("  {\"description\": \"...\", \"action\": \"...\"},\n");
        sb.append("  {\"description\": \"...\", \"action\": \"...\"}\n");
        sb.append("]\n");
        
        return sb.toString();
    }

    private String buildReplanningPrompt(TaskPlan originalPlan, String failure) {
        StringBuilder sb = new StringBuilder();
        sb.append("The following task plan has encountered a failure. Please adjust the remaining steps.\n\n");
        
        sb.append("Goal: ").append(originalPlan.getGoal()).append("\n\n");
        sb.append("Current status:\n");
        
        List<TaskStep> steps = originalPlan.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            TaskStep step = steps.get(i);
            sb.append("Step ").append(i + 1).append(": ").append(step.description());
            if (step.completed()) {
                sb.append(" [COMPLETED]");
            }
            sb.append("\n");
        }
        
        sb.append("\n");
        sb.append("Failure: ").append(failure).append("\n\n");
        sb.append("Please adjust the remaining steps to address this failure. Provide the updated steps as a JSON array:\n");
        sb.append("[\n");
        sb.append("  {\"description\": \"...\", \"action\": \"...\"},\n");
        sb.append("  {\"description\": \"...\", \"action\": \"...\"}\n");
        sb.append("]\n");
        
        return sb.toString();
    }

    private String extractJsonContent(String text) {
        // 尝试提取 JSON 数组 [...]
        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        
        if (arrayStart != -1 && arrayEnd != -1 && arrayEnd > arrayStart) {
            return text.substring(arrayStart, arrayEnd + 1);
        }
        
        return null;
    }

    private TaskPlan createDefaultPlan(String goal) {
        // 创建一个简单的默认计划，只包含一个步骤
        List<TaskStep> steps = new ArrayList<>();
        steps.add(new TaskStep("Execute goal", goal));
        return new TaskPlan(goal, steps);
    }

    /**
     * 用于 JSON 反序列化的数据类。
     */
    private record StepData(
        String description,
        String action
    ) {}
}
