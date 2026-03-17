package com.easyagent4j.core.message;

import java.util.Map;

/**
 * Agent消息顶层接口。
 * 所有消息类型都实现此接口。
 */
public interface AgentMessage {
    MessageRole getRole();
    Object getContent();
    long getTimestamp();
    Map<String, Object> getMetadata();
}
