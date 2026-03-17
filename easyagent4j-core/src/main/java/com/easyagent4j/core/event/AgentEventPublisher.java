package com.easyagent4j.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件发布器 — 同步发布。
 */
public class AgentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(AgentEventPublisher.class);
    private final List<AgentEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(AgentEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(AgentEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(AgentEvent event) {
        for (AgentEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("Error in event listener for event: {}", event.getType(), e);
            }
        }
    }

    public void clear() {
        listeners.clear();
    }
}
