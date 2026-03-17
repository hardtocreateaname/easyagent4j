package com.easyagent4j.core.context.transform;

import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.message.AgentMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合变换器 — 按顺序执行多个变换器。
 */
public class CompositeTransformer implements MessageTransformer {
    private final List<MessageTransformer> transformers;

    public CompositeTransformer(List<MessageTransformer> transformers) {
        this.transformers = new ArrayList<>(transformers);
    }

    @Override
    public List<AgentMessage> transform(List<AgentMessage> messages) {
        List<AgentMessage> result = messages;
        for (MessageTransformer t : transformers) {
            result = t.transform(result);
        }
        return result;
    }
}
