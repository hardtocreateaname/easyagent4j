package com.easyagent4j.example.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EasyAgent4j Web API 示例应用。
 * <p>
 * 提供REST API和SSE流式对话接口。
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
