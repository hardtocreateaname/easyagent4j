package com.easyagent4j.providers.springai;

import com.easyagent4j.core.chat.ChatResponseChunk;
import com.easyagent4j.core.chat.Usage;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.AssistantMessage;
import com.easyagent4j.core.message.ToolResultMessage;
import com.easyagent4j.core.message.ToolCall;
import com.easyagent4j.core.tool.ToolDefinition;
import com.easyagent4j.core.provider.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Spring AI ChatModel适配器 — 将Spring AI的ChatModel适配到LlmProvider接口。
 * 
 * <p>使用此适配器可以让EasyAgent4j使用Spring AI提供的各种模型实现，
 * 包括OpenAI、Anthropic、Zhipu等通过Spring AI starter接入的模型。</p>
 * 
 * <p>使用方式：</p>
 * <pre>
 * // 在Spring配置中
 * &#64;Bean
 * public LlmProvider zhipuProvider(org.springframework.ai.zhipuai.ZhiPuAiChatModel chatModel) {
 *     return new SpringAiProviderAdapter("zhipu", chatModel);
 * }
 * </pre>
 */
public class SpringAiProviderAdapter implements LlmProvider {
    private static final Logger log = LoggerFactory.getLogger(SpringAiProviderAdapter.class);

    private final String providerName;
    private final org.springframework.ai.chat.model.ChatModel springChatModel;

    /**
     * 创建适配器实例。
     * 
     * @param providerName Provider名称（如 "zhipu", "openai", "anthropic"）
     * @param springChatModel Spring AI ChatModel实例
     */
    public SpringAiProviderAdapter(String providerName, org.springframework.ai.chat.model.ChatModel springChatModel) {
        this.providerName = providerName;
        this.springChatModel = springChatModel;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public com.easyagent4j.core.chat.ChatResponse call(com.easyagent4j.core.chat.ChatRequest request) {
        List<Message> springMessages = convertMessages(request);
        Prompt prompt = new Prompt(springMessages, buildChatOptions(request));

        org.springframework.ai.chat.model.ChatResponse springResponse = springChatModel.call(prompt);
        return convertResponse(springResponse);
    }

    @Override
    public void stream(com.easyagent4j.core.chat.ChatRequest request, Consumer<ChatResponseChunk> callback) {
        List<Message> springMessages = convertMessages(request);
        Prompt prompt = new Prompt(springMessages, buildChatOptions(request));

        // 使用AtomicReference跟踪已处理的文本长度，处理累积式流式响应
        // 某些模型（如智谱GLM）返回的流式chunk是累积内容而非增量内容
        AtomicReference<String> lastContent = new AtomicReference<>("");
        final java.util.concurrent.atomic.AtomicInteger rawChunkCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Flux<org.springframework.ai.chat.model.ChatResponse> flux = springChatModel.stream(prompt);
        flux.subscribe(
            chunk -> {
                // 获取原始内容
                String rawContent = null;
                if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
                    rawContent = chunk.getResult().getOutput().getText();
                }
                
                int count = rawChunkCount.incrementAndGet();
                log.debug("Raw Spring AI chunk #{}: [{}]", count, rawContent);
                
                ChatResponseChunk result = convertChunkDelta(chunk, lastContent);
                if ((result.getText() != null && !result.getText().isEmpty())
                    || (result.getToolCalls() != null && !result.getToolCalls().isEmpty())) {
                    log.debug("Sending delta: [{}]", result.getText());
                    callback.accept(result);
                }
            },
            error -> {
                ChatResponseChunk errorChunk = new ChatResponseChunk();
                errorChunk.setText("[Error: " + error.getMessage() + "]");
                errorChunk.setFinished(true);
                callback.accept(errorChunk);
            },
            () -> {
                ChatResponseChunk doneChunk = new ChatResponseChunk();
                doneChunk.setFinished(true);
                callback.accept(doneChunk);
            }
        );
    }

    @Override
    public boolean supportsToolCalling() {
        return true; // Spring AI ChatModel通常支持工具调用
    }

    @Override
    public boolean supportsStreaming() {
        return true; // Spring AI ChatModel通常支持流式
    }

    @Override
    public boolean supportsVision() {
        return true; // 取决于具体模型实现
    }

    /**
     * 将EasyAgent4j的ChatRequest转换为Spring AI的Message列表。
     */
    private List<Message> convertMessages(com.easyagent4j.core.chat.ChatRequest request) {
        List<Message> springMessages = new ArrayList<>();

        // 添加系统消息
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            springMessages.add(new org.springframework.ai.chat.messages.SystemMessage(request.getSystemPrompt()));
        }

        // 转换消息列表
        if (request.getMessages() != null) {
            for (Object msgObj : request.getMessages()) {
                convertMessage(msgObj).ifPresent(springMessages::add);
            }
        }

        return springMessages;
    }

    private org.springframework.ai.chat.prompt.ChatOptions buildChatOptions(com.easyagent4j.core.chat.ChatRequest request) {
        if (request.getTools() == null || request.getTools().isEmpty()) {
            return null;
        }

        List<ToolCallback> callbacks = request.getTools().stream()
            .map(this::toToolCallback)
            .toList();

        return DefaultToolCallingChatOptions.builder()
            .toolCallbacks(callbacks)
            .internalToolExecutionEnabled(false)
            .build();
    }

    private ToolCallback toToolCallback(ToolDefinition tool) {
        return FunctionToolCallback.<Map<String, Object>, String>builder(
                tool.getName(),
                input -> "Tool execution is handled by EasyAgent4j"
            )
            .description(tool.getDescription())
            .inputType(Map.class)
            .inputSchema(tool.getParameterSchema())
            .build();
    }

    /**
     * 转换单个消息。
     */
    private Optional<Message> convertMessage(Object msgObj) {
        if (msgObj instanceof Message) {
            return Optional.of((Message) msgObj);
        } else if (msgObj instanceof Map<?, ?> mapMsg) {
            String role = mapMsg.get("role") != null ? mapMsg.get("role").toString().toLowerCase() : "user";
            String content = mapMsg.get("content") != null ? mapMsg.get("content").toString() : "";
            return switch (role) {
                case "user" -> Optional.of(new org.springframework.ai.chat.messages.UserMessage(content));
                case "assistant" -> Optional.of(new org.springframework.ai.chat.messages.AssistantMessage(content));
                case "system" -> Optional.of(new org.springframework.ai.chat.messages.SystemMessage(content));
                default -> Optional.of(new org.springframework.ai.chat.messages.UserMessage(content));
            };
        } else if (msgObj instanceof com.easyagent4j.core.message.UserMessage) {
            com.easyagent4j.core.message.UserMessage userMsg = (com.easyagent4j.core.message.UserMessage) msgObj;
            return Optional.of(new org.springframework.ai.chat.messages.UserMessage(userMsg.getTextContent()));
        } else if (msgObj instanceof AssistantMessage assistantMsg) {
            List<org.springframework.ai.chat.messages.AssistantMessage.ToolCall> toolCalls = assistantMsg.getToolCalls()
                .stream()
                .map(tc -> new org.springframework.ai.chat.messages.AssistantMessage.ToolCall(
                    tc.getId(), "function", tc.getName(), tc.getArguments()))
                .toList();
            return Optional.of(org.springframework.ai.chat.messages.AssistantMessage.builder()
                .content(assistantMsg.getTextContent())
                .toolCalls(toolCalls)
                .build());
        } else if (msgObj instanceof com.easyagent4j.core.message.SystemMessage) {
            com.easyagent4j.core.message.SystemMessage systemMsg = (com.easyagent4j.core.message.SystemMessage) msgObj;
            return Optional.of(new org.springframework.ai.chat.messages.SystemMessage(systemMsg.getTextContent()));
        } else if (msgObj instanceof ToolResultMessage toolResultMsg) {
            org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse toolResponse =
                new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse(
                    toolResultMsg.getToolCall().getId(),
                    toolResultMsg.getToolCall().getName(),
                    toolResultMsg.getResultContent()
                );
            return Optional.of(org.springframework.ai.chat.messages.ToolResponseMessage.builder()
                .responses(List.of(toolResponse))
                .build());
        } else if (msgObj instanceof AgentMessage) {
            AgentMessage agentMsg = (AgentMessage) msgObj;
            String content = agentMsg.getContent() != null ? agentMsg.getContent().toString() : "";
            return switch (agentMsg.getRole()) {
                case USER -> Optional.of(new org.springframework.ai.chat.messages.UserMessage(content));
                case ASSISTANT -> Optional.of(new org.springframework.ai.chat.messages.AssistantMessage(content));
                case SYSTEM -> Optional.of(new org.springframework.ai.chat.messages.SystemMessage(content));
                case TOOL_RESULT -> Optional.empty();
                default -> Optional.empty();
            };
        }
        return Optional.empty();
    }

    /**
     * 将Spring AI响应转换为EasyAgent4j响应。
     */
    private com.easyagent4j.core.chat.ChatResponse convertResponse(org.springframework.ai.chat.model.ChatResponse springResponse) {
        com.easyagent4j.core.chat.ChatResponse response = new com.easyagent4j.core.chat.ChatResponse();
        
        if (springResponse.getResult() != null && springResponse.getResult().getOutput() != null) {
            response.setText(springResponse.getResult().getOutput().getText());
            
            // 提取工具调用
            List<ToolCall> toolCalls = extractToolCalls(springResponse);
            if (!toolCalls.isEmpty()) {
                response.setToolCalls(toolCalls);
            }
        }

        // 提取usage
        if (springResponse.getMetadata() != null && springResponse.getMetadata().getUsage() != null) {
            var usage = springResponse.getMetadata().getUsage();
            response.setUsage(new Usage(
                usage.getPromptTokens().intValue(),
                usage.getCompletionTokens() != null ? usage.getCompletionTokens().intValue() : 0,
                usage.getTotalTokens().intValue()
            ));
        }

        return response;
    }

    /**
     * 从Spring AI响应中提取工具调用。
     */
    private List<ToolCall> extractToolCalls(org.springframework.ai.chat.model.ChatResponse springResponse) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        if (springResponse.getResult() != null && 
            springResponse.getResult().getOutput() != null &&
            springResponse.getResult().getOutput() instanceof org.springframework.ai.chat.messages.AssistantMessage) {
            
            org.springframework.ai.chat.messages.AssistantMessage assistantMessage = 
                (org.springframework.ai.chat.messages.AssistantMessage) springResponse.getResult().getOutput();
            List<org.springframework.ai.chat.messages.AssistantMessage.ToolCall> springToolCalls = assistantMessage.getToolCalls();
            
            if (springToolCalls != null) {
                for (org.springframework.ai.chat.messages.AssistantMessage.ToolCall tc : springToolCalls) {
                    toolCalls.add(new ToolCall(
                        tc.id(),
                        tc.name(),
                        tc.arguments()
                    ));
                }
            }
        }
        
        return toolCalls;
    }

    /**
     * 将Spring AI流式响应转换为EasyAgent4j响应块（增量模式）。
     * 
     * <p>处理两种流式模式：</p>
     * <ul>
     *   <li>累积模式：每个chunk包含从开始到当前位置的全部内容（如智谱GLM）</li>
     *   <li>增量模式：每个chunk只包含新增的内容（如OpenAI）</li>
     * </ul>
     */
    private ChatResponseChunk convertChunkDelta(org.springframework.ai.chat.model.ChatResponse springChunk, 
                                                 AtomicReference<String> lastContentRef) {
        ChatResponseChunk chunk = new ChatResponseChunk();
        List<ToolCall> toolCalls = extractToolCalls(springChunk);
        if (!toolCalls.isEmpty()) {
            chunk.setToolCalls(toolCalls);
        }
        
        if (springChunk.getResult() != null && springChunk.getResult().getOutput() != null) {
            String currentContent = springChunk.getResult().getOutput().getText();
            
            if (currentContent != null && !currentContent.isEmpty()) {
                String lastContent = lastContentRef.get();
                
                // 检测是否为累积模式
                if (currentContent.startsWith(lastContent) && currentContent.length() > lastContent.length()) {
                    // 累积模式：提取新增部分
                    String delta = currentContent.substring(lastContent.length());
                    chunk.setText(delta);
                    lastContentRef.set(currentContent);
                } else if (!currentContent.equals(lastContent)) {
                    // 增量模式或内容变化：直接使用
                    chunk.setText(currentContent);
                    lastContentRef.set(lastContent + currentContent);
                }
                // 如果currentContent等于lastContent，说明是重复内容，忽略
            }
        }

        return chunk;
    }
}
