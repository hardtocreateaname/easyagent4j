package com.easyagent4j.spring.autoconfigure;

import com.easyagent4j.core.agent.Agent;
import com.easyagent4j.core.agent.AgentConfig;
import com.easyagent4j.core.chat.ChatModel;
import com.easyagent4j.core.context.MessageTransformer;
import com.easyagent4j.core.context.transform.SlidingWindowTransformer;
import com.easyagent4j.core.tool.ToolExecutionMode;
import com.easyagent4j.spring.chat.SpringAiChatModelAdapter;
import com.easyagent4j.spring.observability.AgentMetrics;
import com.easyagent4j.spring.observability.DefaultAgentMetrics;
import com.easyagent4j.spring.observability.MetricsEventListener;
import com.easyagent4j.spring.properties.EasyAgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import com.easyagent4j.core.event.AgentEventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EasyAgent4j auto configuration.
 */
@Configuration
@ConditionalOnClass(Agent.class)
@EnableConfigurationProperties(EasyAgentProperties.class)
public class EasyAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentConfig easyAgentConfig(EasyAgentProperties props) {
        ToolExecutionMode mode = props.getToolExecutionMode();
        return AgentConfig.builder()
            .systemPrompt(props.getSystemPrompt())
            .maxToolIterations(props.getMaxToolIterations())
            .toolExecutionMode(mode != null ? mode : ToolExecutionMode.PARALLEL)
            .streamingEnabled(props.isStreaming())
            .build();
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
    @ConditionalOnMissingBean(Agent.class)
    public Agent agent(AgentConfig config, ChatModel chatModel) {
        return new Agent(config, chatModel);
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
