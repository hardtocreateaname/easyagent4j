package com.easyagent4j.core.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM Provider注册表 — 管理多个Provider实例。
 * 支持注册、获取、设置默认Provider。
 */
public class LlmProviderRegistry {
    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<>();
    private String defaultProviderName;

    /**
     * 注册Provider。
     * @param provider Provider实例
     */
    public void register(LlmProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        String name = provider.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }
        providers.put(name, provider);
    }

    /**
     * 获取指定名称的Provider。
     * @param name Provider名称
     * @return Provider实例，不存在返回null
     */
    public LlmProvider get(String name) {
        if (name == null) {
            return null;
        }
        return providers.get(name);
    }

    /**
     * 获取默认Provider。
     * @return 默认Provider实例，无默认返回null
     */
    public LlmProvider getDefault() {
        if (defaultProviderName == null) {
            return null;
        }
        return providers.get(defaultProviderName);
    }

    /**
     * 设置默认Provider。
     * @param name Provider名称
     * @throws IllegalArgumentException 如果Provider不存在
     */
    public void setDefault(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Provider name cannot be null");
        }
        if (!providers.containsKey(name)) {
            throw new IllegalArgumentException("Provider '" + name + "' not found. Available providers: " + providers.keySet());
        }
        this.defaultProviderName = name;
    }

    /**
     * 获取所有已注册的Provider。
     * @return Provider集合（不可修改）
     */
    public Collection<LlmProvider> getAll() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * 获取所有已注册的Provider名称。
     * @return Provider名称集合（不可修改）
     */
    public Collection<String> getAllNames() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    /**
     * 检查是否存在指定Provider。
     * @param name Provider名称
     * @return true存在，false不存在
     */
    public boolean contains(String name) {
        if (name == null) {
            return false;
        }
        return providers.containsKey(name);
    }

    /**
     * 获取已注册Provider数量。
     * @return Provider数量
     */
    public int size() {
        return providers.size();
    }

    /**
     * 清空所有Provider。
     */
    public void clear() {
        providers.clear();
        defaultProviderName = null;
    }
}