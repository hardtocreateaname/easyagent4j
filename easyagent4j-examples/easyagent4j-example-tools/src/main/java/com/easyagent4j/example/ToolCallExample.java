package com.easyagent4j.example;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.event.AgentEventListener;
import com.easyagent4j.core.event.events.*;
import com.easyagent4j.core.tool.ToolExecutionMode;
import com.easyagent4j.example.tools.CalculatorTool;
import com.easyagent4j.example.tools.FileReadTool;
import com.easyagent4j.example.tools.WeatherTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EasyAgent4j 工具调用示例。
 */
@SpringBootApplication
public class ToolCallExample implements CommandLineRunner {

    @Autowired
    private Agent agent;

    public static void main(String[] args) {
        SpringApplication.run(ToolCallExample.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // 注册工具
        agent.addTool(new WeatherTool());
        agent.addTool(new CalculatorTool());
        agent.addTool(new FileReadTool());

        // 监听事件 - 先清除旧的监听器，避免重复订阅
        agent.clearListeners();
        agent.subscribe(event -> {
            if (event instanceof MessageUpdateEvent e) {
                System.out.print(e.getDelta());
            } else if (event instanceof ToolExecutionStartEvent e) {
                System.out.println("\n[Tool Call] " + e.getToolCall().getName() + "(" + e.getToolCall().getArguments() + ")");
            } else if (event instanceof ErrorEvent e) {
                System.err.println("\n[Error] " + e.getErrorMessage());
            }
        });

        // 示例1：工具调用
        System.out.println("=== 示例：工具调用 ===");
        agent.prompt("北京今天天气怎么样？帮我算一下 28 * 72 + 156 等于多少").get();

        System.out.println("\n\n完成！");
    }
}
