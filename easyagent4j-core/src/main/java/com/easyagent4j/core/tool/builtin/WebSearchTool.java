package com.easyagent4j.core.tool.builtin;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Web搜索工具 — 通过HTTP GET请求获取网页内容，模拟搜索功能。
 * <p>
 * 支持配置允许访问的域名白名单。
 */
public class WebSearchTool extends AbstractAgentTool {

    private final Set<String> allowedDomains;
    private final int maxContentLength;
    private final int connectTimeout;
    private final int readTimeout;

    /**
     * 创建Web搜索工具（允许所有域名）。
     */
    public WebSearchTool() {
        super("web_search", "通过HTTP GET请求获取网页内容。可用于搜索和获取网络信息。");
        this.allowedDomains = Set.of(); // 空集合表示允许所有域名
        this.maxContentLength = 50000; // 最大50KB
        this.connectTimeout = 5000;
        this.readTimeout = 10000;
    }

    /**
     * 创建Web搜索工具，限制域名白名单。
     *
     * @param allowedDomains 允许访问的域名集合（空集合表示允许所有）
     */
    public WebSearchTool(Set<String> allowedDomains) {
        super("web_search", "通过HTTP GET请求获取网页内容。可用于搜索和获取网络信息。");
        this.allowedDomains = allowedDomains != null ? allowedDomains : Set.of();
        this.maxContentLength = 50000;
        this.connectTimeout = 5000;
        this.readTimeout = 10000;
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                    "type": "object",
                    "properties": {
                        "url": {
                            "type": "string",
                            "description": "要请求的URL地址"
                        },
                        "method": {
                            "type": "string",
                            "description": "HTTP方法，默认GET"
                        },
                        "headers": {
                            "type": "string",
                            "description": "自定义请求头（JSON格式，可选）"
                        }
                    },
                    "required": ["url"]
                }
                """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String urlStr = context.getStringArg("url");
        String method = context.getStringArg("method");

        if (urlStr == null || urlStr.isBlank()) {
            return ToolResult.error("参数 url 不能为空");
        }

        // URL合法性校验
        try {
            URI.create(urlStr).toURL();
        } catch (Exception e) {
            return ToolResult.error("无效的URL: " + urlStr);
        }

        // 域名白名单检查
        if (!allowedDomains.isEmpty()) {
            String host = URI.create(urlStr).getHost();
            if (host == null || !allowedDomains.contains(host)) {
                return ToolResult.error("域名 '" + host + "' 不在白名单中。允许的域名: " + allowedDomains);
            }
        }

        // 只允许HTTP/HTTPS
        String protocol = URI.create(urlStr).getScheme();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            return ToolResult.error("只允许 http/https 协议");
        }

        String httpMethod = (method != null && !method.isBlank()) ? method.toUpperCase() : "GET";

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod(httpMethod);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("User-Agent", "EasyAgent4j-WebSearch/1.0");
            conn.setRequestProperty("Accept", "text/html,text/plain,application/json,*/*");

            int responseCode = conn.getResponseCode();
            StringBuilder content = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                int length = 0;
                while ((line = reader.readLine()) != null && length < maxContentLength) {
                    content.append(line).append("\n");
                    length += line.length();
                }
                if (length >= maxContentLength) {
                    content.append("\n... (内容已截断，最大 ").append(maxContentLength / 1024).append("KB)\n");
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                return ToolResult.success(
                        "[HTTP " + responseCode + "]\n" + content.toString().trim());
            } else {
                return ToolResult.error(
                        "HTTP请求失败，状态码: " + responseCode + "\n" + content);
            }
        } catch (Exception e) {
            return ToolResult.error("请求失败: " + e.getMessage());
        }
    }
}
