package com.easyagent4j.core.context;

import com.easyagent4j.core.message.AgentMessage;
import java.util.List;

/**
 * 消息变换器接口。
 */
public interface MessageTransformer {
    List<AgentMessage> transform(List<AgentMessage> messages);
}
