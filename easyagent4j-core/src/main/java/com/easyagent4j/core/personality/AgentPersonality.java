package com.easyagent4j.core.personality;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent性格配置类。
 *
 * 定义Agent的角色、语气、特质、回复风格等性格特征，用于生成system prompt。
 */
public class AgentPersonality {
    private final String name;
    private final String role;
    private final String tone;
    private final List<String> traits;
    private final String responseStyle;
    private final List<String> boundaries;
    private final List<String> principles;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    // Jackson反序列化构造函数
    @JsonCreator
    private AgentPersonality(
            @JsonProperty("name") String name,
            @JsonProperty("role") String role,
            @JsonProperty("tone") String tone,
            @JsonProperty("traits") List<String> traits,
            @JsonProperty("responseStyle") String responseStyle,
            @JsonProperty("boundaries") List<String> boundaries,
            @JsonProperty("principles") List<String> principles) {
        this.name = name;
        this.role = role;
        this.tone = tone;
        this.traits = traits != null ? new ArrayList<>(traits) : new ArrayList<>();
        this.responseStyle = responseStyle;
        this.boundaries = boundaries != null ? new ArrayList<>(boundaries) : new ArrayList<>();
        this.principles = principles != null ? new ArrayList<>(principles) : new ArrayList<>();
    }

    // Builder模式构造函数
    private AgentPersonality(Builder builder) {
        this.name = builder.name;
        this.role = builder.role;
        this.tone = builder.tone;
        this.traits = new ArrayList<>(builder.traits);
        this.responseStyle = builder.responseStyle;
        this.boundaries = new ArrayList<>(builder.boundaries);
        this.principles = new ArrayList<>(builder.principles);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 根据性格字段生成system prompt段落。
     */
    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        // 角色定义
        if (role != null && !role.isEmpty()) {
            prompt.append("You are ").append(role);
            if (name != null && !name.isEmpty()) {
                prompt.append(", named ").append(name);
            }
            prompt.append(".\n\n");
        }

        // 语气
        if (tone != null && !tone.isEmpty()) {
            prompt.append("Tone: ").append(tone).append("\n\n");
        }

        // 特质
        if (!traits.isEmpty()) {
            prompt.append("Traits:\n");
            for (String trait : traits) {
                prompt.append("- ").append(trait).append("\n");
            }
            prompt.append("\n");
        }

        // 回复风格
        if (responseStyle != null && !responseStyle.isEmpty()) {
            prompt.append("Response Style: ").append(responseStyle).append("\n\n");
        }

        // 原则
        if (!principles.isEmpty()) {
            prompt.append("Principles:\n");
            for (String principle : principles) {
                prompt.append("- ").append(principle).append("\n");
            }
            prompt.append("\n");
        }

        // 边界
        if (!boundaries.isEmpty()) {
            prompt.append("Boundaries:\n");
            for (String boundary : boundaries) {
                prompt.append("- ").append(boundary).append("\n");
            }
        }

        return prompt.toString().trim();
    }

    /**
     * 从JSON字符串加载性格配置。
     */
    public static AgentPersonality fromJson(String json) throws JsonProcessingException {
        return JSON_MAPPER.readValue(json, AgentPersonality.class);
    }

    /**
     * 从Markdown格式加载性格配置（类似OpenClaw的SOUL.md）。
     *
     * 支持的格式：
     * - ## Name
     * - ## Role
     * - ## Tone
     * - ## Traits (下面用-列出)
     * - ## Response Style
     * - ## Principles (下面用-列出)
     * - ## Boundaries (下面用-列出)
     */
    public static AgentPersonality fromMarkdown(String markdown) {
        Builder builder = builder();
        String[] lines = markdown.split("\n");
        String currentSection = null;
        List<String> currentList = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // 检测章节标题 ## Title
            if (trimmed.startsWith("## ")) {
                // 保存上一个列表
                if (currentList != null) {
                    setSectionList(builder, currentSection, currentList);
                    currentList = null;
                }
                currentSection = trimmed.substring(3).trim().toLowerCase();
                continue;
            }

            // 检测列表项 - item
            if (trimmed.startsWith("- ") && currentSection != null) {
                String item = trimmed.substring(2).trim();
                if (currentList == null) {
                    currentList = new ArrayList<>();
                }
                currentList.add(item);
                continue;
            }

            // 单值字段（Name, Role, Tone, Response Style）
            if (currentSection != null && !trimmed.isEmpty() && !trimmed.startsWith("-")) {
                setSectionValue(builder, currentSection, trimmed);
                currentSection = null; // 单值字段只读取第一行
            }
        }

        // 保存最后一个列表
        if (currentList != null) {
            setSectionList(builder, currentSection, currentList);
        }

        return builder.build();
    }

    private static void setSectionValue(Builder builder, String section, String value) {
        switch (section.toLowerCase()) {
            case "name":
                builder.name(value);
                break;
            case "role":
                builder.role(value);
                break;
            case "tone":
                builder.tone(value);
                break;
            case "response style":
            case "responsestyle":
                builder.responseStyle(value);
                break;
        }
    }

    private static void setSectionList(Builder builder, String section, List<String> list) {
        switch (section.toLowerCase()) {
            case "traits":
                builder.traits(list);
                break;
            case "principles":
                builder.principles(list);
                break;
            case "boundaries":
                builder.boundaries(list);
                break;
        }
    }

    // Getters
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getTone() { return tone; }
    public List<String> getTraits() { return new ArrayList<>(traits); }
    public String getResponseStyle() { return responseStyle; }
    public List<String> getBoundaries() { return new ArrayList<>(boundaries); }
    public List<String> getPrinciples() { return new ArrayList<>(principles); }

    /**
     * Builder模式构造。
     */
    public static class Builder {
        private String name;
        private String role;
        private String tone;
        private List<String> traits = new ArrayList<>();
        private String responseStyle;
        private List<String> boundaries = new ArrayList<>();
        private List<String> principles = new ArrayList<>();

        public Builder name(String name) { this.name = name; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder tone(String tone) { this.tone = tone; return this; }
        public Builder traits(List<String> traits) {
            this.traits = traits != null ? new ArrayList<>(traits) : new ArrayList<>();
            return this;
        }
        public Builder addTrait(String trait) {
            this.traits.add(trait);
            return this;
        }
        public Builder responseStyle(String responseStyle) { this.responseStyle = responseStyle; return this; }
        public Builder boundaries(List<String> boundaries) {
            this.boundaries = boundaries != null ? new ArrayList<>(boundaries) : new ArrayList<>();
            return this;
        }
        public Builder addBoundary(String boundary) {
            this.boundaries.add(boundary);
            return this;
        }
        public Builder principles(List<String> principles) {
            this.principles = principles != null ? new ArrayList<>(principles) : new ArrayList<>();
            return this;
        }
        public Builder addPrinciple(String principle) {
            this.principles.add(principle);
            return this;
        }

        public AgentPersonality build() {
            return new AgentPersonality(this);
        }
    }
}
