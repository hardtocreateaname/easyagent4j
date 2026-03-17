# EasyAgent4j v0.2.0 迭代规格文档

## 项目现状
- 8个Maven模块，92个Java源文件，6139行代码
- 已有：AgentLoop（核心循环）、Agent（主类）、AgentConfig、AgentContext、Tool系统、Event系统、Steering机制
- 缺失：记忆模块、用户偏好、Agent性格、自主任务循环

## 需求

### 1. 记忆模块（Memory Module）
Agent具备持久化记忆能力，类似OpenClaw的MEMORY.md机制。

**接口设计：**
```java
// core/memory/MemoryStore.java
public interface MemoryStore {
    // 短期记忆（当前会话上下文，已由AgentContext覆盖）
    
    // 长期记忆 — 持久化到文件
    void saveLongTerm(String key, String content);        // 写入长期记忆
    Optional<String> loadLongTerm(String key);            // 读取长期记忆
    List<MemoryEntry> searchLongTerm(String query, int limit);  // 语义搜索
    List<MemoryEntry> listLongTerm();                     // 列出所有记忆条目
    
    // 用户偏好
    void setUserPreference(String key, String value);
    Optional<String> getUserPreference(String key);
    Map<String, String> getAllPreferences();
    
    // 记忆管理
    void deleteLongTerm(String key);
    void consolidate();  // 整合短期记忆到长期记忆（摘要式）
}

// core/memory/MemoryEntry.java
public record MemoryEntry(
    String key,
    String content,
    Instant createdAt,
    Instant updatedAt,
    Map<String, String> tags
) {}

// core/memory/MemoryType.java
public enum MemoryType {
    LONG_TERM,      // 长期记忆（持久化）
    PREFERENCE,     // 用户偏好
    PERSONALITY,    // Agent性格
    EPISODIC        // 情节记忆（对话片段）
}
```

**文件存储实现：**
```java
// core/memory/FileMemoryStore.java
// 存储 path: ${agent.memory.base-path}/${sessionId}/
//   memory.md          — 长期记忆（Markdown格式）
//   preferences.json   — 用户偏好（JSON）
//   personality.json   — Agent性格配置（JSON）
//   episodic/          — 情节记忆目录
```

**文件格式（memory.md）：**
```markdown
# Agent Memory

## 用户信息
- 名字：陈佳铭
- 偏好：简洁回复，中文沟通

## 重要决策
- [2026-03-17] 决定使用Spring AI作为底层模型适配层

## 项目上下文
- EasyAgent4j：基于Spring AI的轻量级Agent框架
```

### 2. Agent性格模块（Personality Module）
Agent具备可配置的性格特征。

```java
// core/personality/AgentPersonality.java
public class AgentPersonality {
    private String name;           // Agent名称
    private String role;           // 角色定位
    private String tone;           // 语气风格（formal/casual/professional/friendly）
    private List<String> traits;   // 性格特征（如：严谨、幽默、主动）
    private String responseStyle;  // 回复风格描述
    private List<String> boundaries; // 行为边界（如：不编造数据、不泄露隐私）
    private List<String> principles; // 核心原则
    
    // 根据性格生成system prompt
    public String buildSystemPrompt();  
    // 从JSON文件加载
    public static AgentPersonality fromJson(String json);
    // 从Markdown文件加载（SOUL.md风格）
    public static AgentPersonality fromMarkdown(String markdown);
}
```

**personality.json示例：**
```json
{
  "name": "小助手",
  "role": "编程助手",
  "tone": "professional",
  "traits": ["严谨", "主动", "简洁"],
  "responseStyle": "直接回答问题，不使用填充词",
  "boundaries": ["不编造数据", "不确定时说明", "保护用户隐私"],
  "principles": ["先做再问", "代码质量优先"]
}
```

### 3. 自主任务循环（Autonomous Task Loop）
Agent能够自主循环执行任务直到完成，具备任务规划和容错能力。

```java
// core/task/TaskPlan.java
public class TaskPlan {
    private String goal;                    // 用户原始目标
    private List<TaskStep> steps;           // 规划的步骤
    private int currentStepIndex;
    private TaskStatus status;              // PLANNING/EXECUTING/REVIEWING/DONE/FAILED
    
    public record TaskStep(
        String description,
        String action,
        boolean completed,
        String result,
        int maxRetries,
        int retryCount
    ) {}
    
    public enum TaskStatus { PLANNING, EXECUTING, REVIEWING, DONE, FAILED }
}

// core/task/TaskPlanner.java — LLM驱动的任务规划器
public interface TaskPlanner {
    TaskPlan plan(String userGoal, List<AgentTool> availableTools);
    TaskPlan replan(TaskPlan currentPlan, String failureReason);
}

// core/task/DefaultTaskPlanner.java — 基于LLM的任务规划实现
public class DefaultTaskPlanner implements TaskPlanner {
    private final ChatModel chatModel;
    
    @Override
    public TaskPlan plan(String userGoal, List<AgentTool> availableTools) {
        // 构建规划prompt，让LLM将目标拆解为步骤
        // 每个步骤包含：描述、需要调用的工具/动作、预期输出
    }
}

// core/task/AutonomousAgentLoop.java — 自主循环（扩展AgentLoop）
public class AutonomousAgentLoop extends AgentLoop {
    private final TaskPlanner taskPlanner;
    private final MemoryStore memoryStore;
    
    public AgentContext executeAutonomous(String userGoal) {
        // 1. 规划：TaskPlan plan = taskPlanner.plan(goal, tools)
        // 2. 循环执行每个step：
        //    a. 调用LLM执行step
        //    b. 如果失败，重试（最多maxRetries次）
        //    c. 如果仍失败，replan
        //    d. 记录结果到plan
        // 3. 审查：让LLM review所有步骤结果
        // 4. 如果审查不通过，回到步骤2或replan
        // 5. 保存关键信息到长期记忆
        // 6. 返回最终结果
    }
}
```

### 4. 容错与重试机制

```java
// core/resilience/RetryPolicy.java
public class RetryPolicy {
    private int maxRetries;           // 默认3
    private Duration initialDelay;    // 默认1秒
    private double backoffMultiplier; // 默认2.0
    private boolean retryOnToolError; // 默认true
    private boolean retryOnLlmError;  // 默认true
    
    // 指数退避
    public Duration getNextDelay(int attempt) { ... }
}

// 集成到AgentLoop：
// 在executeSingleTool中，如果工具执行失败且retryPolicy允许重试：
//   1. 等待backoff时间
//   2. 重新执行
//   3. 如果仍然失败，返回错误结果，由TaskPlanner决定是否replan
```

### 5. AgentConfig扩展

```java
// AgentConfig新增字段：
private PersonalityConfig personality;  // 性格配置
private MemoryConfig memory;            // 记忆配置
private RetryPolicy retryPolicy;        // 重试策略
private boolean autonomousMode;         // 是否启用自主模式
private TaskPlanner taskPlanner;        // 任务规划器

public static class MemoryConfig {
    private String basePath = ".agent/memory";  // 记忆存储路径
    private boolean enabled = true;
    private boolean autoConsolidate = true;      // 自动整合短期记忆
}

public static class PersonalityConfig {
    private String name;
    private String personalityPath;  // 性格文件路径（JSON或Markdown）
}
```

### 6. Memory集成到System Prompt

```java
// core/memory/MemoryPromptBuilder.java
public class MemoryPromptBuilder {
    /**
     * 将记忆信息注入到system prompt中。
     * 格式：
     * [原始system prompt]
     * 
     * ## Memory
     * - 用户偏好: ...
     * - 长期记忆摘要: ...
     * - 最近情节: ...
     */
    public String buildEnhancedSystemPrompt(
        String basePrompt,
        AgentPersonality personality,
        MemoryStore memoryStore
    ) { ... }
}
```

## 新增文件清单

```
easyagent4j-core/src/main/java/com/easyagent4j/core/
├── memory/
│   ├── MemoryStore.java              (接口)
│   ├── FileMemoryStore.java          (文件存储实现)
│   ├── MemoryEntry.java              (记忆条目record)
│   ├── MemoryType.java               (记忆类型枚举)
│   └── MemoryPromptBuilder.java      (记忆→system prompt注入)
├── personality/
│   ├── AgentPersonality.java         (性格配置类)
│   └── PersonalityLoader.java        (加载器：JSON/Markdown)
├── task/
│   ├── TaskPlan.java                 (任务计划)
│   ├── TaskStep.java                 (任务步骤record)
│   ├── TaskStatus.java               (任务状态枚举)
│   ├── TaskPlanner.java              (规划器接口)
│   ├── DefaultTaskPlanner.java       (LLM驱动规划实现)
│   └── AutonomousAgentLoop.java      (自主循环)
├── resilience/
│   ├── RetryPolicy.java              (重试策略)
│   └── CircuitBreaker.java           (熔断器，可选)
└── agent/
    ├── AgentConfig.java              (修改：新增字段)
    ├── Agent.java                    (修改：集成记忆/性格/自主模式)
    └── AgentLoop.java                (修改：集成重试策略)
```

## 编码任务拆分

### 子任务1：Memory模块
- MemoryStore接口 + MemoryEntry + MemoryType
- FileMemoryStore实现（memory.md读写 + preferences.json + personality.json）
- MemoryPromptBuilder
- 单元测试

### 子任务2：Personality模块  
- AgentPersonality类 + PersonalityLoader
- JSON/Markdown双格式加载
- buildSystemPrompt方法
- 单元测试

### 子任务3：Task + Autonomous模块
- TaskPlan + TaskStep + TaskStatus + TaskPlanner接口
- DefaultTaskPlanner（LLM驱动规划）
- AutonomousAgentLoop（自主循环+容错重试+replan）
- RetryPolicy
- 单元测试

### 子任务4：集成 + Agent改造
- AgentConfig扩展（新字段+Builder）
- Agent.java集成（记忆/性格/自主模式切换）
- AgentLoop集成重试
- 示例更新
- 全量编译验证

## 验收标准
1. `mvn compile` 全模块编译通过
2. 所有新模块单元测试通过
3. 新增示例演示记忆+性格+自主模式
4. 代码风格与现有代码一致
