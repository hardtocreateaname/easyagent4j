package com.easyagent4j.core;

import com.easyagent4j.core.event.AgentEvent;
import com.easyagent4j.core.event.AgentEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 记录所有事件的监听器 — 用于测试事件流。
 */
public class RecordingEventListener implements AgentEventListener {

    private final List<AgentEvent> events = new ArrayList<>();

    @Override
    public void onEvent(AgentEvent event) {
        events.add(event);
    }

    public List<AgentEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * 获取所有事件的类型序列。
     */
    public List<String> getEventTypeSequence() {
        List<String> types = new ArrayList<>();
        for (AgentEvent event : events) {
            types.add(event.getType());
        }
        return types;
    }

    /**
     * 统计特定类型事件的数量。
     */
    public long countByType(String type) {
        return events.stream().filter(e -> type.equals(e.getType())).count();
    }

    /**
     * 检查事件序列是否包含指定顺序的类型。
     */
    public boolean containsSequence(String... types) {
        List<String> sequence = getEventTypeSequence();
        String seqStr = String.join(",", sequence);
        String needle = String.join(",", types);
        return seqStr.contains(needle);
    }

    /**
     * 检查事件序列是否精确匹配（按给定顺序存在，不要求完全相等）。
     */
    public boolean containsSubSequence(String... types) {
        List<String> sequence = getEventTypeSequence();
        outer:
        for (int i = 0; i <= sequence.size() - types.length; i++) {
            for (int j = 0; j < types.length; j++) {
                if (!sequence.get(i + j).equals(types[j])) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 清空记录。
     */
    public void clear() {
        events.clear();
    }
}
