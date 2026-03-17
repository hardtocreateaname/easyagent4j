package com.easyagent4j.core.context.converter;

import com.easyagent4j.core.context.MessageConverter;
import com.easyagent4j.core.message.AgentMessage;
import com.easyagent4j.core.message.MessageRole;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认消息转换器 — 过滤掉非核心角色，直接透传。
 */
public class DefaultMessageConverter implements MessageConverter {
    @Override
    public List<AgentMessage> convert(List<AgentMessage> messages) {
        List<AgentMessage> result = new ArrayList<>();
        for (AgentMessage msg : messages) {
            if (msg.getRole() == MessageRole.USER
                || msg.getRole() == MessageRole.ASSISTANT
                || msg.getRole() == MessageRole.TOOL_RESULT) {
                result.add(msg);
            }
        }
        return result;
    }
}
