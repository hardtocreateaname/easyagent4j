package com.easyagent4j.example.web.config;

import com.easyagent4j.core.tool.AgentTool;
import com.easyagent4j.core.tool.builtin.BuiltinTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 示例应用工具配置。
 */
@Configuration
public class ToolConfiguration {

    @Bean
    public AgentTool webSearchTool() {
        return BuiltinTools.webSearchTool();
    }
}
