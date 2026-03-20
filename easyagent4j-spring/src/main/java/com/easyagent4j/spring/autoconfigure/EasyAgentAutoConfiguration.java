package com.easyagent4j.spring.autoconfigure;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.context.transform.SlidingWindowTransformer;
import com.easyagent4j.core.memory.FileMemoryStore;
import com.easyagent4j.core.memory.MemoryStore;
import com.easyagent4j.core.personality.AgentPersonality;
import com.easyagent4j.core.personality.PersonalityLoader;
import com.easyagent4j.core.provider.LlmProvider;
import com.easyagent4j.core.provider.LlmProviderRegistry;
import com.easyagent4j.core.provider.ProviderChatModelAdapter;
import com.easyagent4j.core.resilience.RetryPolicy;
import com.easyagent4j.core.tool.AgentTool;
import com.easyagent4j.core.tool.ToolExecutionMode;
import com.easyagent4j.core.tool.builtin.SubAgentTool;
import com.easyagent4j.spring.chat.SpringAiChatModelAdapter;
import com.easyagent4j.spring.agent.AgentSessionManager;
import com.easyagent4j.spring.observability.AgentMetrics;
import com.easyagent4j.spring.observability.DefaultAgentMetrics;
import com.easyagent4j.spring.observability.MetricsEventListener;
import com.easyagent4j.spring.properties.EasyAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import com.easyagent4j.core.event.AgentEventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * EasyAgent4j auto configuration.
 */
@Configuration
@ConditionalOnClass(Agent.class)
@EnableConfigurationProperties(EasyAgentProperties.class)
public class EasyAgentAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(EasyAgentAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public AgentConfig easyAgentConfig(EasyAgentProperties props) {
        ToolExecutionMode mode = props.getToolExecutionMode();
        AgentConfig.Builder builder = AgentConfig.builder()
            .systemPrompt(props.getSystemPrompt())
            .maxToolIterations(props.getMaxToolIterations())
            .toolExecutionMode(mode != null ? mode : ToolExecutionMode.PARALLEL)
            .streamingEnabled(props.isStreaming())
            .autonomousMode(props.isAutonomousMode());

        // 添加memory配置
        if (props.getMemory().isEnabled()) {
            builder.memory(new AgentConfig.MemoryConfig(
                props.getMemory().getBasePath(),
                props.getMemory().isEnabled(),
                props.getMemory().isAutoConsolidate()
            ));
        }

        // 添加personality配置
        if (props.getPersonality().getPersonalityPath() != null) {
            builder.personality(new AgentConfig.PersonalityConfig(
                props.getPersonality().getName(),
                props.getPersonality().getPersonalityPath()
            ));
        }

        // 添加retry policy配置
        builder.retryPolicy(new RetryPolicy());

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(LlmProviderRegistry.class)
    public LlmProviderRegistry llmProviderRegistry(java.util.List<LlmProvider> providers, EasyAgentProperties props) {
        LlmProviderRegistry registry = new LlmProviderRegistry();
        
        // 注册所有可用的Provider（过滤掉null值）
        if (providers != null) {
            for (LlmProvider provider : providers) {
                if (provider != null) {
                    registry.register(provider);
                    log.info("Registered LLM provider: {}", provider.getName());
                }
            }
        }
        
        // 设置默认Provider（仅当provider存在时才设置）
        String defaultProvider = props.getChatProvider();
        if (defaultProvider != null && !defaultProvider.isEmpty()) {
            if (registry.get(defaultProvider) != null) {
                registry.setDefault(defaultProvider);
                log.info("Set default LLM provider: {}", defaultProvider);
            } else {
                log.warn("Requested provider '{}' not found, will use first available provider", defaultProvider);
            }
        }
        
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel easyAgentChatModel(LlmProviderRegistry registry,
                                         java.util.Map<String, org.springframework.ai.chat.model.ChatModel> chatModels,
                                         EasyAgentProperties props) {
        // 优先从Provider Registry中查找
        String providerName = props.getChatProvider();
        if (providerName != null && !providerName.isEmpty()) {
            LlmProvider provider = registry.get(providerName);
            if (provider != null) {
                log.info("Using LLM provider: {}", providerName);
                return new ProviderChatModelAdapter(provider);
            }
        }
        
        // Fallback到第一个可用的Provider
        LlmProvider defaultProvider = registry.getDefault();
        if (defaultProvider != null) {
            log.info("Using default LLM provider: {}", defaultProvider.getName());
            return new ProviderChatModelAdapter(defaultProvider);
        }
        
        // Fallback到Spring AI ChatModel（兼容旧版）
        String beanName = switch (providerName.toLowerCase()) {
            case "zhipuai", "zhipu" -> "zhiPuAiChatModel";
            case "openai" -> "openAiChatModel";
            default -> "openAiChatModel";
        };

        org.springframework.ai.chat.model.ChatModel springChatModel = chatModels.get(beanName);
        if (springChatModel == null) {
            // Fallback to first available
            springChatModel = chatModels.values().iterator().next();
            log.warn("Requested chat model '{}' not found, using first available", beanName);
        }

        log.info("Using Spring AI chat model: {}", beanName);
        return new SpringAiChatModelAdapter(springChatModel);
    }

    @Bean
    @ConditionalOnMissingBean(MessageTransformer.class)
    public MessageTransformer messageTransformer(EasyAgentProperties props) {
        int max = props.getContext().getMaxMessages();
        if (max > 0) {
            return new SlidingWindowTransformer(max);
        }
        return messages -> messages;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "easyagent.memory", name = "enabled", havingValue = "true")
    public MemoryStore memoryStore(EasyAgentProperties props) {
        String basePath = props.getMemory().getBasePath();
        String sessionId = props.getSystemPrompt() != null ? 
            String.valueOf(props.getSystemPrompt().hashCode()) : "default";
        return new FileMemoryStore(basePath, sessionId);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "easyagent.personality", name = "personalityPath")
    public AgentPersonality agentPersonality(EasyAgentProperties props) {
        try {
            String path = props.getPersonality().getPersonalityPath();
            AgentPersonality personality = PersonalityLoader.load(Paths.get(path));
            log.info("Loaded personality from: {}", path);
            return personality;
        } catch (Exception e) {
            log.error("Failed to load personality from: {}", props.getPersonality().getPersonalityPath(), e);
            throw new RuntimeException("Failed to load personality", e);
        }
    }

    @Bean
    @ConditionalOnMissingBean(Agent.class)
    public Agent agent(AgentConfig config, ChatModel chatModel,
                        Optional<MemoryStore> memoryStore,
                        Optional<AgentPersonality> agentPersonality,
                        Optional<List<AgentTool>> tools,
                        Optional<List<AgentEventListener>> listeners) {
        Agent agent = new Agent(config, chatModel);

        // 注入memory store
        if (config.getMemory() != null && config.getMemory().isEnabled() && memoryStore.isPresent()) {
            MemoryStore scopedStore = memoryStore.get();
            if (scopedStore instanceof com.easyagent4j.core.memory.ForkableMemoryStore forkableMemoryStore) {
                scopedStore = forkableMemoryStore.fork(agent.getContext().getSessionId());
            }
            agent.setMemoryStore(scopedStore);
            log.info("MemoryStore configured for agent");
        }

        // 注入personality
        if (config.getPersonality() != null && agentPersonality.isPresent()) {
            agent.setPersonality(agentPersonality.get());
            log.info("AgentPersonality configured for agent: {}", agentPersonality.get().getName());
        }

        for (AgentTool tool : tools.orElse(List.of())) {
            agent.addTool(tool);
        }
        agent.addTool(new SubAgentTool(agent));

        for (AgentEventListener listener : listeners.orElse(List.of())) {
            agent.subscribe(listener);
        }

        return agent;
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentSessionManager agentSessionManager(AgentConfig config,
                                                   ChatModel chatModel,
                                                   Optional<MemoryStore> memoryStore,
                                                   Optional<AgentPersonality> agentPersonality,
                                                   Optional<List<com.easyagent4j.core.tool.AgentTool>> tools,
                                                   Optional<List<AgentEventListener>> listeners) {
        return new AgentSessionManager(
            config,
            chatModel,
            memoryStore,
            agentPersonality,
            tools.orElse(List.of()),
            listeners.orElse(List.of())
        );
    }

    /**
     * 注册AgentMetrics Bean。
     * <p>
     * 默认使用{@link DefaultAgentMetrics}（内存计数器，零外部依赖）。
     * 当classpath中存在Micrometer MeterRegistry时，用户可自行注册
     * Micrometer-backed实现以替换此默认Bean。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(AgentMetrics.class)
    public AgentMetrics agentMetrics() {
        return new DefaultAgentMetrics();
    }

    /**
     * 注册MetricsEventListener，通过事件系统自动采集指标。
     * <p>
     * 注意：此Listener由Spring容器管理，但需要在Agent运行时被
     * 订阅到{@link com.easyagent4j.core.event.AgentEventPublisher}中。
     * 典型做法是在创建Agent后手动调用 {@code publisher.subscribe(metricsEventListener)}。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(MetricsEventListener.class)
    public MetricsEventListener metricsEventListener(AgentMetrics agentMetrics) {
        return new MetricsEventListener(agentMetrics);
    }
}
