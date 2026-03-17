package com.easyagent4j.example;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.memory.FileMemoryStore;
import com.easyagent4j.core.memory.MemoryStore;
import com.easyagent4j.core.personality.AgentPersonality;
import com.easyagent4j.core.tool.AgentTool;
import com.easyagent4j.example.tools.CalculatorTool;
import com.easyagent4j.example.tools.WeatherTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

/**
 * 自主Agent示例 — 演示带记忆、性格、自主模式的Agent。
 */
@SpringBootApplication
public class AutonomousAgentExample implements CommandLineRunner {

    @Autowired
    private Agent agent;

    @Autowired
    private ChatModel chatModel;

    public static void main(String[] args) {
        SpringApplication.run(AutonomousAgentExample.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 自主Agent示例 ===\n");

        // 步骤1：配置记忆存储
        System.out.println("步骤1：配置记忆存储");
        MemoryStore memoryStore = new FileMemoryStore("./agent-memory", "autonomous-session");
        agent.setMemoryStore(memoryStore);

        // 保存一些用户偏好
        memoryStore.setUserPreference("language", "中文");
        memoryStore.setUserPreference("style", "简洁");
        System.out.println("已设置用户偏好：language=中文, style=简洁\n");

        // 步骤2：设置Agent性格
        System.out.println("步骤2：设置Agent性格");
        AgentPersonality personality = AgentPersonality.builder()
            .name("智能助手")
            .role("专业的AI助手，擅长任务规划和自主执行")
            .tone("友好、专业")
            .addTrait("善于思考")
            .addTrait("注重细节")
            .responseStyle("条理清晰，步骤明确")
            .addPrinciple("总是先理解任务目标")
            .addPrinciple("分解复杂任务为可执行的步骤")
            .addBoundary("不执行有害或违法的操作")
            .build();
        agent.setPersonality(personality);
        System.out.println("已设置Agent性格：" + personality.getName());
        System.out.println("角色：" + personality.getRole());
        System.out.println("语气：" + personality.getTone());
        System.out.println();

        // 步骤3：注册工具
        System.out.println("步骤3：注册工具");
        agent.addTool(new WeatherTool());
        agent.addTool(new CalculatorTool());
        System.out.println("已注册工具：WeatherTool, CalculatorTool\n");

        // 步骤4：执行自主任务
        System.out.println("步骤4：执行自主任务");
        String goal = "帮我查询北京的天气，然后根据温度计算适合穿什么厚度的衣服";
        System.out.println("任务目标：" + goal);
        System.out.println("\n开始执行...\n");

        // 监听事件
        agent.subscribe(event -> {
            if (event instanceof com.easyagent4j.core.event.events.MessageUpdateEvent e) {
                System.out.print(e.getDelta());
            } else if (event instanceof com.easyagent4j.core.event.events.ToolExecutionStartEvent e) {
                System.out.println("\n[工具调用] " + e.getToolCall().getName() + "(" + e.getToolCall().getArguments() + ")");
            } else if (event instanceof com.easyagent4j.core.event.events.ErrorEvent e) {
                System.err.println("\n[错误] " + e.getErrorMessage());
            }
        });

        try {
            agent.executeAutonomous(goal).get();
            System.out.println("\n\n任务执行完成！");
        } catch (Exception e) {
            System.err.println("\n任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }

        // 步骤5：查看记忆
        System.out.println("\n\n步骤5：查看记忆存储");
        System.out.println("--- 用户偏好 ---");
        Map<String, String> preferences = memoryStore.getAllPreferences();
        preferences.forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n--- 长期记忆 ---");
        List<com.easyagent4j.core.memory.MemoryEntry> memories = memoryStore.listLongTerm();
        if (memories.isEmpty()) {
            System.out.println("暂无长期记忆");
        } else {
            memories.forEach(memory -> {
                System.out.println("- " + memory.key() + ": " + memory.content());
            });
        }

        System.out.println("\n=== 示例完成 ===");
    }
}