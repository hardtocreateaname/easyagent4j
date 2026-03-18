package com.easyagent4j.provider.zhipu;

import com.easyagent4j.core.chat.ChatRequest;
import com.easyagent4j.core.chat.ChatResponse;
import com.easyagent4j.core.chat.ChatResponseChunk;
import com.easyagent4j.core.chat.Usage;
import com.easyagent4j.core.message.*;
import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.core.provider.LlmProviderConfig;
import com.easyagent4j.core.tool.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zhipu Provider实现 — 使用JDK HttpClient调用智谱GLM API。
 * 智谱GLM使用OpenAI兼容协议，API: POST https://open.bigmodel.cn/api/paas/v4/chat/completions
 * 支持GLM-4等系列模型。
 */
public class ZhipuProvider implements LlmProvider {
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final Pattern SSE_LINE_PATTERN = Pattern.compile("^data:\\s*(.+)$");

    private final LlmProviderConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ZhipuProvider(LlmProviderConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "zhipu";
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        try {
            String requestBody = buildRequestBody(request, false);
            HttpRequest httpRequest = buildHttpRequest(requestBody);

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Zhipu API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call Zhipu API", e);
        }
    }

    @Override
    public void stream(ChatRequest request, Consumer<ChatResponseChunk> callback) {
        try {
            String requestBody = buildRequestBody(request, true);
            HttpRequest httpRequest = buildHttpRequest(requestBody);

            HttpResponse<InputStream> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("Zhipu API error: " + response.statusCode() + " - " + errorBody);
            }

            parseStreamResponse(response.body(), callback);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to stream Zhipu API", e);
        }
    }

    @Override
    public boolean supportsToolCalling() {
        return true;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    private String buildRequestBody(ChatRequest request, boolean stream) throws IOException {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("model", config.getModel());
        rootNode.put("stream", stream);
        rootNode.put("temperature", config.getTemperature());
        rootNode.put("max_tokens", config.getMaxTokens());

        // Build messages array
        ArrayNode messagesArray = rootNode.putArray("messages");

        // Add system prompt if present
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            ObjectNode systemMsg = messagesArray.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
        }

        // Add messages from request
        if (request.getMessages() != null) {
            for (Object msgObj : request.getMessages()) {
                if (msgObj instanceof AgentMessage) {
                    AgentMessage agentMsg = (AgentMessage) msgObj;
                    ObjectNode message = messagesArray.addObject();
                    message.put("role", agentMsg.getRole().name().toLowerCase());

                    if (agentMsg instanceof UserMessage) {
                        UserMessage userMsg = (UserMessage) agentMsg;
                        if (userMsg.getParts() != null && !userMsg.getParts().isEmpty()) {
                            // Multi-modal content
                            ArrayNode contentArray = message.putArray("content");
                            if (userMsg.getTextContent() != null) {
                                ObjectNode textPart = contentArray.addObject();
                                textPart.put("type", "text");
                                textPart.put("text", userMsg.getTextContent());
                            }
                            for (ContentPart part : userMsg.getParts()) {
                                if (part.getType() == ContentPart.Type.IMAGE && part.getImageData() != null) {
                                    ObjectNode imagePart = contentArray.addObject();
                                    imagePart.put("type", "image_url");
                                    ObjectNode imageUrl = imagePart.putObject("image_url");
                                    String base64Image = Base64.getEncoder().encodeToString(part.getImageData());
                                    imageUrl.put("url", "data:" + part.getMimeType() + ";base64," + base64Image);
                                }
                            }
                        } else {
                            message.put("content", userMsg.getTextContent());
                        }
                    } else if (agentMsg instanceof AssistantMessage) {
                        AssistantMessage assistantMsg = (AssistantMessage) agentMsg;
                        if (assistantMsg.hasToolCalls()) {
                            ArrayNode toolCallsArray = message.putArray("tool_calls");
                            for (ToolCall toolCall : assistantMsg.getToolCalls()) {
                                ObjectNode toolCallObj = toolCallsArray.addObject();
                                toolCallObj.put("id", toolCall.getId());
                                toolCallObj.put("type", "function");
                                ObjectNode function = toolCallObj.putObject("function");
                                function.put("name", toolCall.getName());
                                function.put("arguments", toolCall.getArguments());
                            }
                        } else {
                            message.put("content", assistantMsg.getTextContent());
                        }
                    } else if (agentMsg instanceof ToolResultMessage) {
                        ToolResultMessage toolResultMsg = (ToolResultMessage) agentMsg;
                        message.put("content", toolResultMsg.getResultContent());
                        message.put("tool_call_id", toolResultMsg.getToolCall().getId());
                    }
                }
            }
        }

        // Add tools if present
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            ArrayNode toolsArray = rootNode.putArray("tools");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode toolObj = toolsArray.addObject();
                toolObj.put("type", "function");
                ObjectNode function = toolObj.putObject("function");
                function.put("name", tool.getName());
                function.put("description", tool.getDescription());
                function.set("parameters", objectMapper.readTree(tool.getParameterSchema()));
            }
        }

        return objectMapper.writeValueAsString(rootNode);
    }

    private HttpRequest buildHttpRequest(String requestBody) {
        String url = config.getBaseUrl();
        if (!url.endsWith(CHAT_COMPLETIONS_ENDPOINT)) {
            url = url + CHAT_COMPLETIONS_ENDPOINT;
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);

        ChatResponse response = new ChatResponse();

        // Extract text content
        JsonNode choices = rootNode.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode choice = choices.get(0);
            JsonNode message = choice.get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    response.setText(content.asText());
                }

                // Extract tool calls
                JsonNode toolCalls = message.get("tool_calls");
                if (toolCalls != null && toolCalls.isArray()) {
                    List<ToolCall> toolCallList = new ArrayList<>();
                    for (JsonNode toolCall : toolCalls) {
                        JsonNode function = toolCall.get("function");
                        if (function != null) {
                            String id = toolCall.get("id").asText();
                            String name = function.get("name").asText();
                            String arguments = function.get("arguments").asText();
                            toolCallList.add(new ToolCall(id, name, arguments));
                        }
                    }
                    response.setToolCalls(toolCallList);
                }
            }
        }

        // Extract usage
        JsonNode usage = rootNode.get("usage");
        if (usage != null) {
            int promptTokens = usage.get("prompt_tokens").asInt();
            int completionTokens = usage.get("completion_tokens").asInt();
            int totalTokens = usage.get("total_tokens").asInt();
            response.setUsage(new Usage(promptTokens, completionTokens, totalTokens));
        }

        return response;
    }

    private void parseStreamResponse(InputStream inputStream, Consumer<ChatResponseChunk> callback) throws IOException {
        StringBuilder currentLine = new StringBuilder();

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            for (int i = 0; i < chunk.length(); i++) {
                char c = chunk.charAt(i);
                if (c == '\n') {
                    processSSELine(currentLine.toString(), callback);
                    currentLine.setLength(0);
                } else if (c != '\r') {
                    currentLine.append(c);
                }
            }
        }

        // Process last line if exists
        if (currentLine.length() > 0) {
            processSSELine(currentLine.toString(), callback);
        }
    }

    private void processSSELine(String line, Consumer<ChatResponseChunk> callback) {
        if (line == null || line.isEmpty()) {
            return;
        }

        String data = null;
        Matcher matcher = SSE_LINE_PATTERN.matcher(line);
        if (matcher.matches()) {
            data = matcher.group(1);
        } else if (line.startsWith(SSE_DATA_PREFIX)) {
            data = line.substring(SSE_DATA_PREFIX.length());
        }

        if (data == null || data.isEmpty()) {
            return;
        }

        // Check for [DONE] marker
        if (data.trim().equals("[DONE]")) {
            ChatResponseChunk chunk = new ChatResponseChunk();
            chunk.setFinished(true);
            callback.accept(chunk);
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(data);
            ChatResponseChunk chunk = new ChatResponseChunk();

            JsonNode choices = rootNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                JsonNode delta = choice.get("delta");

                if (delta != null) {
                    JsonNode content = delta.get("content");
                    if (content != null && !content.isNull()) {
                        chunk.setText(content.asText());
                    }

                    // Extract tool calls (for streaming tool calls)
                    JsonNode toolCalls = delta.get("tool_calls");
                    if (toolCalls != null && toolCalls.isArray()) {
                        List<ToolCall> toolCallList = new ArrayList<>();
                        for (JsonNode toolCall : toolCalls) {
                            JsonNode function = toolCall.get("function");
                            if (function != null) {
                                String id = toolCall.has("id") && !toolCall.get("id").isNull()
                                        ? toolCall.get("id").asText() : null;
                                String name = function.has("name") && !function.get("name").isNull()
                                        ? function.get("name").asText() : null;
                                String arguments = function.has("arguments") && !function.get("arguments").isNull()
                                        ? function.get("arguments").asText() : null;
                                if (id != null && name != null) {
                                    toolCallList.add(new ToolCall(id, name, arguments != null ? arguments : ""));
                                }
                            }
                        }
                    }
                }

                // Check if finished
                JsonNode finishReason = choice.get("finish_reason");
                if (finishReason != null && !finishReason.isNull()) {
                    chunk.setFinished(true);
                }
            }

            callback.accept(chunk);
        } catch (IOException e) {
            // Ignore parsing errors for incomplete chunks
        }
    }
}