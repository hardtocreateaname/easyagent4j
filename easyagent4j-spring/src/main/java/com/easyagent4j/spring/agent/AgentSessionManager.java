package com.easyagent4j.spring.agent;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.event.AgentEventListener;
import com.easyagent4j.core.memory.ForkableMemoryStore;
import com.easyagent4j.core.memory.MemoryStore;
import com.easyagent4j.core.personality.AgentPersonality;
import com.easyagent4j.core.tool.AgentTool;
import com.easyagent4j.core.tool.builtin.SubAgentTool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring环境下的Agent会话管理器。
 */
public class AgentSessionManager {
    private final AgentConfig baseConfig;
    private final ChatModel chatModel;
    private final Optional<MemoryStore> memoryStore;
    private final Optional<AgentPersonality> personality;
    private final List<AgentTool> externalTools;
    private final List<AgentEventListener> listeners;
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    public AgentSessionManager(AgentConfig baseConfig,
                               ChatModel chatModel,
                               Optional<MemoryStore> memoryStore,
                               Optional<AgentPersonality> personality,
                               List<AgentTool> externalTools,
                               List<AgentEventListener> listeners) {
        this.baseConfig = baseConfig;
        this.chatModel = chatModel;
        this.memoryStore = memoryStore;
        this.personality = personality;
        this.externalTools = externalTools;
        this.listeners = listeners;
    }

    public Agent getOrCreateRootAgent(String sessionId) {
        return agents.computeIfAbsent(sessionId, this::createRootAgent);
    }

    public Optional<Agent> getAgent(String sessionId) {
        return Optional.ofNullable(agents.get(sessionId));
    }

    public Agent spawnSubAgent(String parentSessionId, String childSessionId, boolean inheritMessages) {
        Agent parent = agents.get(parentSessionId);
        if (parent == null) {
            throw new IllegalArgumentException("Parent agent not found: " + parentSessionId);
        }
        Agent child = parent.spawnSubAgent(childSessionId, inheritMessages);
        subscribeListeners(child);
        agents.put(child.getContext().getSessionId(), child);
        return child;
    }

    private Agent createRootAgent(String sessionId) {
        AgentConfig config = baseConfig.toBuilder().sessionId(sessionId).build();
        Agent agent = new Agent(config, chatModel);

        if (memoryStore.isPresent()) {
            MemoryStore scopedStore = memoryStore.get();
            if (scopedStore instanceof ForkableMemoryStore forkableMemoryStore) {
                scopedStore = forkableMemoryStore.fork(agent.getContext().getSessionId());
            }
            agent.setMemoryStore(scopedStore);
        }
        personality.ifPresent(agent::setPersonality);
        for (AgentTool tool : externalTools) {
            agent.addTool(tool);
        }
        agent.addTool(new SubAgentTool(agent));
        subscribeListeners(agent);
        return agent;
    }

    private void subscribeListeners(Agent agent) {
        for (AgentEventListener listener : listeners) {
            agent.subscribe(listener);
        }
    }
}
