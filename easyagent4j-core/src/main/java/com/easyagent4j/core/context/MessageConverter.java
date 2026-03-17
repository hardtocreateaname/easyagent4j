package com.easyagent4j.core.context;

import com.easyagent4j.core.message.AgentMessage;
import java.util.List;

/**
 * 消息格式转换接口 — 将AgentMessage转换为LLM消息格式。
 */
public interface MessageConverter {
    List<?> convert(List<AgentMessage> messages);
}
