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
import com.easyagent4j.core.resilience.RetryPolicy;
import com.easyagent4j.core.tool.ToolExecutionMode;
import com.easyagent4j.spring.chat.SpringAiChatModelAdapter;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

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
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel easyAgentChatModel(org.springframework.ai.chat.model.ChatModel springChatModel) {
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
                        MemoryStore memoryStore, AgentPersonality agentPersonality) {
        Agent agent = new Agent(config, chatModel);

        // 注入memory store
        if (config.getMemory() != null && config.getMemory().isEnabled() && memoryStore != null) {
            agent.setMemoryStore(memoryStore);
            log.info("MemoryStore configured for agent");
        }

        // 注入personality
        if (config.getPersonality() != null && agentPersonality != null) {
            agent.setPersonality(agentPersonality);
            log.info("AgentPersonality configured for agent: {}", agentPersonality.getName());
        }

        return agent;
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
