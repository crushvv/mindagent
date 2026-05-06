# MindAgent

一个用于学习多模态 Agent 开发的心理咨询助手项目，技术栈基于 Java + Spring AI，模型使用 Qwen2.5-7B，通过 Ollama 提供本地推理能力，并使用 Docker 进行部署与环境管理。

## 1. 项目目标

- 学习并实践 Spring AI 的 Agent 设计方式（Prompt、Tool、Memory、RAG、Workflow）。
- 构建一个可演进的心理咨询场景 Agent（以支持性对话为主，不替代专业医疗诊断）。
- 建立可持续迭代的工程结构：可运行、可测试、可部署、可记录开发进度。

## 2. 功能边界（MVP）

- 文本对话：支持多轮上下文记忆与角色设定。
- 基础多模态预留：先预留图像/语音输入接口，不在第一阶段实现复杂处理链路。
- 心理咨询风格回复：强调共情、澄清、支持，不给出医疗诊断结论。
- 安全策略：对自伤/伤人等高风险语义给出危机干预提示（紧急求助建议）。
- 会话记录：保存匿名化会话，便于后续评估与调优。

## 3. 技术方案

- 语言与框架：Java 17+、Spring Boot、Spring AI
- 模型服务：Ollama + `qwen2.5:7b`
- 工具协议：MCP（Model Context Protocol）统一封装 Agent 外部工具能力
- 后端形态：Spring Boot 单体优先，后续按流量拆分服务
- 容器化：Docker / Docker Compose
- 数据存储（建议）：PostgreSQL（会话） + Redis（短期记忆缓存，可选）
- 可观测性（后续）：Actuator + 日志聚合

## 4. 系统架构（初稿）

1. `mindagent-api`：对外 REST API（chat、session、health）。
2. `mindagent-core`：Agent 编排（prompt 模板、工具调用、记忆策略、安全策略）。
3. `mindagent-infra`：模型调用适配、数据库访问、缓存和外部组件封装。
4. `deploy`：Docker Compose、环境变量模板、部署脚本。

### 4.1 Spring Boot 后端上线能力（必须项）

- 提供标准 REST API 与统一异常处理，满足前端/移动端接入。
- 支持配置中心化（环境变量 + profile），区分 dev/staging/prod。
- 支持健康探针（liveness/readiness）、日志追踪、优雅停机。
- 支持容器化部署与滚动发布，满足后续上线运行需求。

## 5. 情绪标签驱动的回复路由（RAG/直答）

你提出的策略可以实现，并建议作为默认路由规则：

- `chat`：直接生成回复（不走 RAG），降低时延与成本。
- `失落` / `焦虑` / `risk`：先进行 RAG 检索，再结合检索内容生成回复。
- 安全兜底：`risk` 标签必须走安全策略模板，RAG 仅作为辅助，不覆盖危机干预主回复。

路由收益：

- 普通聊天更快，体验更自然。
- 情绪波动和风险场景回复更稳，更可控，更可追溯。

## 6. 风险通知与咨询留档（新增）

- `risk` 场景（如自杀风险）触发 Agent 通过 MCP 调用邮件工具，通知导员。
- 非 `chat` 场景（`失落`、`焦虑`、`risk`）触发 Agent 通过 MCP 调用 Excel 工具写入留档。
- 留档字段建议包含：时间、会话 ID、匿名用户 ID、情绪标签、风险等级、路由策略、回复摘要、是否触发通知。
- 默认遵循最小化与脱敏原则，避免在 Excel 中落敏感原文。
- 未来新增工具（短信、企业微信、工单、日历）只需新增 MCP Tool，无需改动 Agent 主流程。

## 7. 开发阶段规划

### Phase 0 - 项目初始化

- 初始化 Spring Boot + Spring AI 工程
- 接入本地 Ollama 与 Qwen2.5-7B
- 完成基础对话 API 与健康检查

### Phase 1 - 心理咨询 Agent MVP

- 设计咨询助手系统提示词（共情、倾听、边界）
- 接入会话记忆（最近 N 轮 + 摘要）
- 增加风险语义识别与安全回复模板
- 完成情绪标签路由（`chat` 直答；其余标签走 RAG）
- 完成 `risk` 导员通知链路（异步）

### Phase 2 - 多模态能力与质量提升

- 增加图像输入能力（先做接口与占位处理）
- 完成情绪评分与权重融合
- 完成非 `chat` 咨询留档与 Excel 导出
- 引入评估脚本（回复质量、安全性、稳定性）
- 增加日志追踪与可观测性

### Phase 3 - 工程化与部署

- 完成 Docker Compose 一键启动
- 增加 CI（构建 + 测试）
- 输出部署文档与演示流程

## 8. 项目结构（建议）

```text
mindagent/
  ├─ README.md
  ├─ DEVELOPMENT_LOG.md
  ├─ docs/
  │   ├─ architecture.md
  │   ├─ prompts.md
  │   └─ safety-guidelines.md
  ├─ mindagent-api/
  ├─ mindagent-core/
  ├─ mindagent-infra/
  └─ deploy/
```

## 9. 安全与伦理声明

- 本项目用于学习 Agent 开发，不构成医疗建议或心理治疗。
- 对于危机内容（如自伤、伤人、极端绝望表达），系统必须优先引导用户联系当地紧急救助和专业机构。
- 所有用户数据应默认最小化采集、匿名化存储，并明确告知用途。

## 10. 近期任务（Next Sprint）

1. 创建 Spring Boot 主工程与多模块骨架。
2. 完成 Ollama 联通验证（`qwen2.5:7b`）。
3. 实现 `/api/chat` MVP（单轮到多轮）。
4. 落地情绪标签路由（`chat` 直答，其他标签 RAG）。
5. 增加 `risk` 导员通知能力（异步、可重试）。
6. 增加非 `chat` 咨询 Excel 留档导出。
7. 编写第一版安全策略与系统提示词模板。

---

如果你希望，我下一步可以直接在这个仓库里把 `Spring Boot + Spring AI + Docker Compose` 骨架也一并搭起来。