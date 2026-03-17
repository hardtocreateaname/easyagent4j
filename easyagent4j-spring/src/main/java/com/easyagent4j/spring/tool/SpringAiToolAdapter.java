package com.easyagent4j.spring.tool;

import com.easyagent4j.core.tool.AgentTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * AgentTool → Spring AI Tool 注解适配辅助类。
 */
public class SpringAiToolAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 从AgentTool列表提取工具定义（用于构建Spring AI的ToolCallback）。
     */
    public static Map<String, AgentTool> buildToolMap(java.util.List<AgentTool> tools) {
        Map<String, AgentTool> map = new HashMap<>();
        for (AgentTool tool : tools) {
            map.put(tool.getName(), tool);
        }
        return map;
    }
}
