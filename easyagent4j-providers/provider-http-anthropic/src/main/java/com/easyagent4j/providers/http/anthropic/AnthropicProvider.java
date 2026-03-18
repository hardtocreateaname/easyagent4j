package com.easyagent4j.providers.http.anthropic;

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
 * Anthropic Provider实现 — 使用JDK HttpClient调用Anthropic Messages API。
 * 支持Claude系列模型（Claude 3.5 Sonnet, Claude 3 Opus, Claude 3 Haiku等）。
 */
public class AnthropicProvider implements LlmProvider {
    private static final String MESSAGES_ENDPOINT = "/v1/messages";
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final Pattern SSE_LINE_PATTERN = Pattern.compile("^data:\\s*(.+)$");
    private static final Pattern EVENT_LINE_PATTERN = Pattern.compile("^event:\\s*(.+)$");

    private final LlmProviderConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicProvider(LlmProviderConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        try {
            String requestBody = buildRequestBody(request, false);
            HttpRequest httpRequest = buildHttpRequest(requestBody);

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Anthropic API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call Anthropic API", e);
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
                throw new RuntimeException("Anthropic API error: " + response.statusCode() + " - " + errorBody);
            }

            parseStreamResponse(response.body(), callback);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to stream Anthropic API", e);
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
        rootNode.put("max_tokens", config.getMaxTokens());
        
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            rootNode.put("system", request.getSystemPrompt());
        }

        // Build messages array
        ArrayNode messagesArray = rootNode.putArray("messages");

        // Add messages from request
        if (request.getMessages() != null) {
            for (Object msgObj : request.getMessages()) {
                if (msgObj instanceof AgentMessage) {
                    AgentMessage agentMsg = (AgentMessage) msgObj;
                    String role = convertRole(agentMsg.getRole());
                    
                    ObjectNode message = messagesArray.addObject();
                    message.put("role", role);

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
                                    imagePart.put("type", "image");
                                    ObjectNode source = imagePart.putObject("source");
                                    String base64Image = Base64.getEncoder().encodeToString(part.getImageData());
                                    source.put("type", "base64");
                                    source.put("media_type", part.getMimeType());
                                    source.put("data", base64Image);
                                }
                            }
                        } else {
                            message.put("content", userMsg.getTextContent());
                        }
                    } else if (agentMsg instanceof AssistantMessage) {
                        AssistantMessage assistantMsg = (AssistantMessage) agentMsg;
                        if (assistantMsg.hasToolCalls()) {
                            ArrayNode contentArray = message.putArray("content");
                            for (ToolCall toolCall : assistantMsg.getToolCalls()) {
                                ObjectNode toolUseContent = contentArray.addObject();
                                toolUseContent.put("type", "tool_use");
                                toolUseContent.put("id", toolCall.getId());
                                toolUseContent.put("name", toolCall.getName());
                                toolUseContent.put("input", objectMapper.readTree(toolCall.getArguments()));
                            }
                        } else {
                            message.put("content", assistantMsg.getTextContent());
                        }
                    } else if (agentMsg instanceof ToolResultMessage) {
                        ToolResultMessage toolResultMsg = (ToolResultMessage) agentMsg;
                        ArrayNode contentArray = message.putArray("content");
                        ObjectNode toolResultContent = contentArray.addObject();
                        toolResultContent.put("type", "tool_result");
                        toolResultContent.put("tool_use_id", toolResultMsg.getToolCall().getId());
                        
                        String resultContent = toolResultMsg.getResultContent();
                        if (resultContent != null) {
                            // Try to parse as JSON first, fallback to text
                            try {
                                toolResultContent.set("content", objectMapper.readTree(resultContent));
                            } catch (IOException e) {
                                toolResultContent.put("content", resultContent);
                            }
                        }
                    }
                }
            }
        }

        // Add tools if present
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            ArrayNode toolsArray = rootNode.putArray("tools");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode toolObj = toolsArray.addObject();
                toolObj.put("name", tool.getName());
                toolObj.put("description", tool.getDescription());
                toolObj.set("input_schema", objectMapper.readTree(tool.getParameterSchema()));
            }
        }

        return objectMapper.writeValueAsString(rootNode);
    }

    private String convertRole(MessageRole role) {
        // Anthropic uses "user" and "assistant" roles
        switch (role) {
            case USER:
                return "user";
            case ASSISTANT:
                return "assistant";
            case SYSTEM:
                return "user"; // System prompts are handled separately in Anthropic
            default:
                return "user";
        }
    }

    private HttpRequest buildHttpRequest(String requestBody) {
        String url = config.getBaseUrl();
        if (!url.endsWith(MESSAGES_ENDPOINT)) {
            url = url + MESSAGES_ENDPOINT;
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);

        ChatResponse response = new ChatResponse();

        // Extract content and tool use blocks
        JsonNode contentArray = rootNode.get("content");
        if (contentArray != null && contentArray.isArray()) {
            StringBuilder textBuilder = new StringBuilder();
            List<ToolCall> toolCallList = new ArrayList<>();
            
            for (JsonNode content : contentArray) {
                String type = content.get("type").asText();
                
                if ("text".equals(type)) {
                    JsonNode textNode = content.get("text");
                    if (textNode != null) {
                        textBuilder.append(textNode.asText());
                    }
                } else if ("tool_use".equals(type)) {
                    String id = content.get("id").asText();
                    String name = content.get("name").asText();
                    JsonNode inputNode = content.get("input");
                    String arguments = inputNode != null ? inputNode.toString() : "{}";
                    toolCallList.add(new ToolCall(id, name, arguments));
                }
            }
            
            response.setText(textBuilder.toString());
            if (!toolCallList.isEmpty()) {
                response.setToolCalls(toolCallList);
            }
        }

        // Extract usage
        JsonNode usage = rootNode.get("usage");
        if (usage != null) {
            int promptTokens = usage.get("input_tokens").asInt();
            int completionTokens = usage.get("output_tokens").asInt();
            int totalTokens = promptTokens + completionTokens;
            response.setUsage(new Usage(promptTokens, completionTokens, totalTokens));
        }

        return response;
    }

    private void parseStreamResponse(InputStream inputStream, Consumer<ChatResponseChunk> callback) throws IOException {
        StringBuilder currentLine = new StringBuilder();
        String currentEvent = null;

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            for (int i = 0; i < chunk.length(); i++) {
                char c = chunk.charAt(i);
                if (c == '\n') {
                    String line = currentLine.toString();
                    processSSELine(line, currentEvent, callback);
                    currentLine.setLength(0);
                    // Reset event after processing data
                    if (line.startsWith("event:")) {
                        currentEvent = null;
                    }
                } else if (c != '\r') {
                    currentLine.append(c);
                }
            }
        }

        // Process last line if exists
        if (currentLine.length() > 0) {
            processSSELine(currentLine.toString(), currentEvent, callback);
        }
    }

    private void processSSELine(String line, String currentEvent, Consumer<ChatResponseChunk> callback) {
        if (line == null || line.isEmpty()) {
            return;
        }

        // Check for event type
        Matcher eventMatcher = EVENT_LINE_PATTERN.matcher(line);
        if (eventMatcher.matches()) {
            currentEvent = eventMatcher.group(1);
            return;
        }

        // Check for data
        String data = null;
        Matcher dataMatcher = SSE_LINE_PATTERN.matcher(line);
        if (dataMatcher.matches()) {
            data = dataMatcher.group(1);
        } else if (line.startsWith(SSE_DATA_PREFIX)) {
            data = line.substring(SSE_DATA_PREFIX.length());
        }

        if (data == null || data.isEmpty()) {
            return;
        }

        // Process based on event type
        if ("message_stop".equals(currentEvent)) {
            ChatResponseChunk chunk = new ChatResponseChunk();
            chunk.setFinished(true);
            callback.accept(chunk);
            return;
        }

        if ("content_block_start".equals(currentEvent) || "content_block_delta".equals(currentEvent)) {
            try {
                JsonNode rootNode = objectMapper.readTree(data);
                ChatResponseChunk chunk = new ChatResponseChunk();

                JsonNode delta = rootNode.get("delta");
                if (delta != null) {
                    JsonNode type = delta.get("type");
                    
                    if (type != null && "text_delta".equals(type.asText())) {
                        JsonNode textNode = delta.get("text");
                        if (textNode != null) {
                            chunk.setText(textNode.asText());
                        }
                    } else if (type != null && "input_json_delta".equals(type.asText())) {
                        // Tool call arguments delta (streaming)
                        JsonNode partialJson = delta.get("partial_json");
                        if (partialJson != null) {
                            // Accumulate partial JSON for tool calls
                            // This is complex and would need state management
                            // For now, we'll skip streaming tool calls
                        }
                    }
                }

                callback.accept(chunk);
            } catch (IOException e) {
                // Ignore parsing errors for incomplete chunks
            }
        }
    }
}
