package com.easyagent4j.example.basic;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.context.AgentContext;
import com.easyagent4j.core.message.AgentMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EasyAgent4j 基础对话示例。
 * 展示最简单的Agent用法：发送一句话，获取回复。
 */
@SpringBootApplication
public class BasicChatExample implements CommandLineRunner {

    @Autowired
    private Agent agent;

    public static void main(String[] args) {
        SpringApplication.run(BasicChatExample.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== EasyAgent4j 基础对话示例 ===");

        AgentContext context = agent.prompt("你好").get();

        // 打印最后一条助手消息
        AgentMessage last = context.getLastMessage();
        System.out.println("Agent回复: " + (last != null ? last.getContent() : "(无回复)"));

        System.out.println("\n完成！");
    }
}
