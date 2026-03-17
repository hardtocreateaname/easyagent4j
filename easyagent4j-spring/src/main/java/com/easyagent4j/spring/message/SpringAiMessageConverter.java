package com.easyagent4j.spring.message;

import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.ToolCall;
import com.easyagent4j.core.message.ToolResultMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * AgentMessage to Spring AI Message converter.
 */
public class SpringAiMessageConverter {

    public List<Message> toSpringAiMessages(List<AgentMessage> messages) {
        List<Message> result = new ArrayList<>();
        for (AgentMessage msg : messages) {
            Message springMsg = toSpringAiMessage(msg);
            if (springMsg != null) {
                result.add(springMsg);
            }
        }
        return result;
    }

    public Message toSpringAiMessage(AgentMessage msg) {
        return switch (msg.getRole()) {
            case USER -> {
                String text = msg.getContent() != null ? msg.getContent().toString() : "";
                yield UserMessage.builder().text(text).build();
            }
            case ASSISTANT -> {
                String text;
                List<AssistantMessage.ToolCall> toolCalls = List.of();
                if (msg instanceof com.easyagent4j.core.message.AssistantMessage am) {
                    text = am.getTextContent();
                    List<AssistantMessage.ToolCall> tcList = new ArrayList<>();
                    for (com.easyagent4j.core.message.ToolCall tc : am.getToolCalls()) {
                        tcList.add(new AssistantMessage.ToolCall(tc.getId(), "function", tc.getName(), tc.getArguments()));
                    }
                    toolCalls = tcList;
                } else {
                    text = msg.getContent() != null ? msg.getContent().toString() : "";
                }
                yield AssistantMessage.builder()
                    .content(text)
                    .toolCalls(toolCalls)
                    .build();
            }
            case TOOL_RESULT -> {
                ToolResultMessage trm = (ToolResultMessage) msg;
                ToolResponseMessage.ToolResponse tr = new ToolResponseMessage.ToolResponse(
                    trm.getToolCall().getId(),
                    trm.getToolCall().getName(),
                    trm.getResultContent());
                yield ToolResponseMessage.builder().responses(List.of(tr)).build();
            }
            default -> null;
        };
    }

    public AgentMessage fromSpringAiMessage(Message msg) {
        if (msg instanceof UserMessage um) {
            return new com.easyagent4j.core.message.UserMessage(um.getText());
        } else if (msg instanceof AssistantMessage am) {
            return new com.easyagent4j.core.message.AssistantMessage(am.getText());
        } else if (msg instanceof ToolResponseMessage trm) {
            String toolCallId = "";
            String toolName = "";
            if (trm.getResponses() != null && !trm.getResponses().isEmpty()) {
                ToolResponseMessage.ToolResponse tr = trm.getResponses().get(0);
                toolCallId = tr.id();
                toolName = tr.name();
            }
            return new ToolResultMessage(
                new ToolCall(toolCallId, toolName, null),
                trm.getText(), false);
        }
        return null;
    }
}
