package com.easyagent4j.example.tools;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

/**
 * 天气查询工具示例。
 */
public class WeatherTool extends AbstractAgentTool {

    public WeatherTool() {
        super("get_weather", "查询指定城市的天气信息");
    }

    @Override
    public String getParameterSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名称"}
                },
                "required": ["city"]
            }
            """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String city = context.getStringArg("city");
        // 模拟天气数据
        String weather = String.format(
            "%s今日天气：晴，温度18°C，湿度45%%，风力东南风3级。适合外出。",
            city);
        return ToolResult.success(weather);
    }
}
