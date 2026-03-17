# EasyAgent4j — 开发计划

> 创建日期: 2026-03-16 | 最后更新: 2026-03-16

---

## Phase 1: 核心骨架（目标：能跑通基本对话+工具调用）

### 任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1.1 | 初始化Maven多模块项目结构 | ⬜ 待开始 | 父POM + 4个子模块的pom.xml |
| 1.2 | easyagent4j-core: 消息模型 | ⬜ 待开始 | AgentMessage接口 + 4个实现类 + ContentPart + ToolCall |
| 1.3 | easyagent4j-core: 枚举和基础类型 | ⬜ 待开始 | MessageRole, AgentState, ToolExecutionMode, ToolResult |
| 1.4 | easyagent4j-core: 异常体系 | ⬜ 待开始 | AgentException及其子类 |
| 1.5 | easyagent4j-core: ChatModel抽象接口 | ⬜ 待开始 | ChatModel, ChatRequest, ChatResponse, ChatResponseChunk |
| 1.6 | easyagent4j-core: AgentTool接口 + AbstractAgentTool | ⬜ 待开始 | 工具定义、ToolContext、ToolHook |
| 1.7 | easyagent4j-core: 事件系统 | ⬜ 待开始 | AgentEvent基类 + 11个事件类型 + AgentEventPublisher |
| 1.8 | easyagent4j-core: MessageTransformer + SlidingWindowTransformer | ⬜ 待开始 | 上下文变换 |
| 1.9 | easyagent4j-core: MessageConverter + DefaultMessageConverter | ⬜ 待开始 | 消息格式转换 |
| 1.10 | easyagent4j-core: AgentLoop（核心循环） | ⬜ 待开始 | 串行工具执行 + 流式支持 |
| 1.11 | easyagent4j-core: Agent（主类） | ⬜ 待开始 | 组装所有组件，对外暴露API |
| 1.12 | easyagent4j-core: AgentConfig + Builder | ⬜ 待开始 | 配置类 |
| 1.13 | easyagent4j-spring: SpringAiChatModelAdapter | ⬜ 待开始 | 适配Spring AI ChatModel |
| 1.14 | easyagent4j-spring: SpringAiMessageConverter | ⬜ 待开始 | AgentMessage ↔ Spring AI Message |
| 1.15 | easyagent4j-spring: SpringAiToolAdapter | ⬜ 待开始 | AgentTool → Spring AI FunctionCallback |
| 1.16 | easyagent4j-spring: AutoConfiguration + Properties | ⬜ 待开始 | 自动配置 |
| 1.17 | easyagent4j-spring-boot-starter | ⬜ 待开始 | Starter模块 |
| 1.18 | easyagent4j-example-basic: 基础对话示例 | ⬜ 待开始 | 纯文本对话 |
| 1.19 | easyagent4j-example-tools: 工具调用示例 | ⬜ 待开始 | WeatherTool + CalculatorTool |
| 1.20 | Phase 1集成测试 | ⬜ 待开始 | 确保对话+工具调用跑通 |
| 1.21 | mvn compile + mvn test 全模块通过 | ⬜ 待开始 | 确保编译和测试通过 |

### Phase 1 验收标准
- ✅ 能通过Spring AI连接OpenAI进行对话
- ✅ 能定义工具并让Agent调用
- ✅ 事件系统正常工作（能收到流式token）
- ✅ mvn compile && mvn test 全部通过

---

## Phase 2: 完善能力

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 2.1 | AgentLoop: 并行工具执行 | ⬜ 待开始 | CompletableFuture.allOf |
| 2.2 | TokenBudgetTransformer | ⬜ 待开始 | 基于token计数的上下文裁剪 |
| 2.3 | ToolHook完善: beforeToolCall/afterToolCall | ⬜ 待开始 | 工具调用前后钩子 |
| 2.4 | Steering机制 | ⬜ 待开始 | 运行中干预（打断Agent） |
| 2.5 | FollowUp机制 | ⬜ 待开始 | 后续任务队列 |
| 2.6 | AgentContext持久化 | ⬜ 待开始 | 消息历史保存/恢复 |
| 2.7 | SpringAiContentMapper（多模态） | ⬜ 待开始 | 图片等内容类型适配 |
| 2.8 | core层单元测试补充 | ⬜ 待开始 | AgentLoop测试、工具测试 |

---

## Phase 3: 高级特性

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 3.1 | Multi-Agent协作（AgentGroup） | ⬜ 待开始 | 多Agent编排 |
| 3.2 | easyagent4j-example-web: Web API示例 | ⬜ 待开始 | SSE流式 + REST |
| 3.3 | 可观测性（Micrometer指标） | ⬜ 待开始 | token消耗、调用次数、延迟 |
| 3.4 | Tool注解方式定义 | ⬜ 待开始 | @Tool, @ToolParam 注解 |
| 3.5 | 内置常用工具（FileTool, ShellTool, SearchTool） | ⬜ 待开始 | 开箱即用的工具集 |
| 3.6 | README.md + 文档完善 | ⬜ 待开始 | 使用文档 |
| 3.7 | GitHub Actions CI/CD | ⬜ 待开始 | 自动化构建和测试 |

---

## 执行记录

| 日期 | 完成内容 | 备注 |
|------|---------|------|
| 2026-03-16 | 架构文档 v1.0 完成 | ARCHITECTURE.md |
| 2026-03-16 | 开发计划创建 | PLAN.md |

---

## 文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| 架构文档 | `ARCHITECTURE.md` | 详细架构设计 |
| 开发计划 | `PLAN.md` | 本文件 |
| 项目源码 | CentOS: `/root/easyagent4j/` | Maven项目 |
