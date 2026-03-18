package com.easyagent4j.provider.minimax;

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
 * MiniMax Provider实现 — 使用JDK HttpClient调用MiniMax API。
 * API: POST https://api.minimax.chat/v1/text/chatcompletion_v2
 * 支持abab6.5s-chat等系列模型。
 */
public class MiniMaxProvider implements LlmProvider {
    private static final String CHAT_COMPLETION_ENDPOINT = "/v1/text/chatcompletion_v2";
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final Pattern SSE_LINE_PATTERN = Pattern.compile("^data:\\s*(.+)$");

    private final LlmProviderConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MiniMaxProvider(LlmProviderConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "minimax";
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        try {
            String requestBody = buildRequestBody(request, false);
            HttpRequest httpRequest = buildHttpRequest(requestBody);

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("MiniMax API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to call MiniMax API", e);
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
                throw new RuntimeException("MiniMax API error: " + response.statusCode() + " - " + errorBody);
            }

            parseStreamResponse(response.body(), callback);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to stream MiniMax API", e);
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
        rootNode.put("tokens_to_generate", config.getMaxTokens());

        // Build messages array (MiniMax uses "messages" field)
        ArrayNode messagesArray = rootNode.putArray("messages");

        // Add system prompt if present (as first message with sender_type "system")
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            ObjectNode systemMsg = messagesArray.addObject();
            systemMsg.put("sender_type", "system");
            systemMsg.put("sender_name", "system");
            systemMsg.put("text", request.getSystemPrompt());
        }

        // Add messages from request
        if (request.getMessages() != null) {
            for (Object msgObj : request.getMessages()) {
                if (msgObj instanceof AgentMessage) {
                    AgentMessage agentMsg = (AgentMessage) msgObj;
                    ObjectNode message = messagesArray.addObject();
                    String senderType = convertSenderType(agentMsg.getRole());
                    message.put("sender_type", senderType);
                    message.put("sender_name", senderType);

                    if (agentMsg instanceof UserMessage) {
                        UserMessage userMsg = (UserMessage) agentMsg;
                        if (userMsg.getParts() != null && !userMsg.getParts().isEmpty()) {
                            // Multi-modal content
                            ArrayNode contentArray = message.putArray("text");
                            if (userMsg.getTextContent() != null) {
                                contentArray.add(userMsg.getTextContent());
                            }
                            for (ContentPart part : userMsg.getParts()) {
                                if (part.getType() == ContentPart.Type.IMAGE && part.getImageData() != null) {
                                    // MiniMax supports images through base64 encoding
                                    ObjectNode imageObj = contentArray.addObject();
                                    String base64Image = Base64.getEncoder().encodeToString(part.getImageData());
                                    imageObj.put("image_base64", base64Image);
                                }
                            }
                        } else {
                            message.put("text", userMsg.getTextContent());
                        }
                    } else if (agentMsg instanceof AssistantMessage) {
                        AssistantMessage assistantMsg = (AssistantMessage) agentMsg;
                        if (assistantMsg.hasToolCalls()) {
                            // MiniMax handles tool calls differently - use function_call field
                            for (ToolCall toolCall : assistantMsg.getToolCalls()) {
                                ObjectNode functionCall = message.putObject("function_call");
                                functionCall.put("name", toolCall.getName());
                                functionCall.put("arguments", toolCall.getArguments());
                            }
                        } else {
                            message.put("text", assistantMsg.getTextContent());
                        }
                    } else if (agentMsg instanceof ToolResultMessage) {
                        ToolResultMessage toolResultMsg = (ToolResultMessage) agentMsg;
                        ObjectNode functionCall = message.putObject("function_call");
                        functionCall.put("name", toolResultMsg.getToolCall().getName());
                        functionCall.put("arguments", toolResultMsg.getToolCall().getArguments());
                        message.put("text", toolResultMsg.getResultContent());
                    }
                }
            }
        }

        // Add tools if present (MiniMax uses "functions" field)
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            ArrayNode functionsArray = rootNode.putArray("functions");
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode functionObj = functionsArray.addObject();
                functionObj.put("name", tool.getName());
                functionObj.put("description", tool.getDescription());
                functionObj.set("parameters", objectMapper.readTree(tool.getParameterSchema()));
            }
        }

        return objectMapper.writeValueAsString(rootNode);
    }

    private String convertSenderType(MessageRole role) {
        // MiniMax uses "USER" and "BOT" roles
        switch (role) {
            case USER:
                return "USER";
            case ASSISTANT:
                return "BOT";
            case SYSTEM:
                return "system";
            default:
                return "USER";
        }
    }

    private HttpRequest buildHttpRequest(String requestBody) {
        String url = config.getBaseUrl();
        if (!url.endsWith(CHAT_COMPLETION_ENDPOINT)) {
            url = url + CHAT_COMPLETION_ENDPOINT;
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

        // Extract text content (MiniMax uses "reply" field)
        JsonNode reply = rootNode.get("reply");
        if (reply != null) {
            response.setText(reply.asText());
        }

        // Extract choices (for consistency with other providers)
        JsonNode choices = rootNode.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode choice = choices.get(0);
            JsonNode message = choice.get("message");
            if (message != null && message.has("content")) {
                response.setText(message.get("content").asText());
            }
        }

        // Extract function calls (MiniMax uses "function_call" field)
        JsonNode functionCall = rootNode.get("function_call");
        if (functionCall != null && !functionCall.isNull()) {
            List<ToolCall> toolCallList = new ArrayList<>();
            String name = functionCall.has("name") ? functionCall.get("name").asText() : null;
            String arguments = functionCall.has("arguments") ? functionCall.get("arguments").asText() : null;
            if (name != null) {
                toolCallList.add(new ToolCall(generateToolCallId(), name, arguments != null ? arguments : ""));
            }
            if (!toolCallList.isEmpty()) {
                response.setToolCalls(toolCallList);
            }
        }

        // Extract usage (MiniMax uses "usage" field)
        JsonNode usage = rootNode.get("usage");
        if (usage != null) {
            int promptTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;
            int completionTokens = usage.has("tokens_to_generate") ? usage.get("tokens_to_generate").asInt() : 0;
            int totalTokens = promptTokens + completionTokens;
            response.setUsage(new Usage(promptTokens, completionTokens, totalTokens));
        }

        return response;
    }

    private String generateToolCallId() {
        return "call_" + System.currentTimeMillis();
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

            // Extract reply text (MiniMax uses "reply" field in streaming)
            JsonNode reply = rootNode.get("reply");
            if (reply != null && !reply.isNull()) {
                chunk.setText(reply.asText());
            }

            // Extract choices (for consistency)
            JsonNode choices = rootNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                JsonNode delta = choice.get("delta");

                if (delta != null) {
                    JsonNode content = delta.get("content");
                    if (content != null && !content.isNull()) {
                        chunk.setText(content.asText());
                    }
                }

                // Check if finished
                JsonNode finishReason = choice.get("finish_reason");
                if (finishReason != null && !finishReason.isNull()) {
                    chunk.setFinished(true);
                }
            }

            // Check for function call
            JsonNode functionCall = rootNode.get("function_call");
            if (functionCall != null && !functionCall.isNull()) {
                // Handle streaming function calls if needed
            }

            callback.accept(chunk);
        } catch (IOException e) {
            // Ignore parsing errors for incomplete chunks
        }
    }
}
