# MindAgent Engineering Specification

## 1. 文档目的

本文件用于给 `MindAgent` 提供可执行的工程规范，明确系统边界、接口契约、质量指标和交付流程，避免项目停留在概念层。

## 2. 系统上下文

- 用户通过客户端调用后端聊天接口。
- 后端进行请求校验、多模态解析、会话装配、安全检查与模型调用。
- 模型由 Ollama 托管（`qwen2.5:7b`），在本地或容器中运行。
- 对话与审计日志按匿名化策略落库，支持后续评估。

## 3. 关键用例

1. 普通支持对话：用户表达情绪，系统提供共情和引导。
2. 多轮跟进：系统基于会话历史保持上下文一致性。
3. 多模态理解：整合文本、语音、图像、视频输入并统一表达。
4. 风险识别：检测自伤/伤人意图并输出危机干预提示。
5. 运维可观测：排查请求失败、超时、模型异常等问题。

## 4. 逻辑架构与职责

## 4.1 API 层（mindagent-api）

- 接收请求、参数校验、鉴权（后续）。
- 返回统一响应结构和错误码。
- 提供健康检查、版本信息、运行状态。

## 4.2 Core 层（mindagent-core）

- Prompt 管理：系统提示词、场景模板、语气约束。
- Memory 管理：短期记忆（窗口）+ 长期摘要（可选）。
- Safety Guard：风险分级、拒答或危机模板输出。
- Emotion Scoring：单模态情绪评估与四级标签打分。
- Multimodal Fusion：多模态权重融合，产出最终情绪标签。
- Agent Orchestration：决定调用链（是否查历史、是否触发安全策略、是否调用工具）。
- Tool Decision Policy：基于 `emotionLabel` 的工具调用策略（邮件、Excel）。

## 4.3 Infra 层（mindagent-infra）

- Ollama Client 封装（重试、超时、错误映射）。
- Whisper Client 封装（语音/视频音轨转写）。
- Vision/Video Adapter（图像/视频内容摘要转文本）。
- Repository 封装（会话、消息、审计日志）。
- MCP Tool Gateway：统一管理 MCP Server 连接、Tool 注册、调用、超时、重试与鉴权。
- Tool Adapters：邮件发送器、Excel 写入器（以 MCP Tool 形式暴露给 Agent）。
- 缓存与配置加载（Redis / env / yaml）。

## 5. API 设计（MVP）

## 5.1 `POST /api/chat`

请求体示例：

```json
{
  "sessionId": "optional-session-id",
  "userId": "anonymous-user-id",
  "message": "最近压力特别大，睡不着",
  "modalInputs": [
    {
      "type": "text",
      "content": "最近压力特别大，睡不着"
    },
    {
      "type": "audio",
      "contentBase64": "..."
    },
    {
      "type": "image",
      "contentBase64": "..."
    }
  ],
  "metadata": {
    "clientType": "web"
  }
}
```

响应体示例：

```json
{
  "requestId": "trace-id",
  "sessionId": "session-id",
  "reply": "听起来你最近承受了很多压力，我们可以先从今晚最影响你的一个念头开始聊。",
  "emotionLabel": "焦虑",
  "emotionScores": {
    "chat": 0.15,
    "失落": 0.20,
    "焦虑": 0.58,
    "risk": 0.07
  },
  "riskLevel": "MEDIUM",
  "safetyAction": "NONE",
  "latencyMs": 2531
}
```

## 5.2 错误码建议

- `MA-4001` 参数非法
- `MA-4290` 请求过载
- `MA-5001` 模型调用失败
- `MA-5002` 存储失败

## 6. 数据模型（建议）

## 6.1 Session

- `id`
- `user_id`（匿名 ID）
- `created_at`
- `updated_at`

## 6.2 Message

- `id`
- `session_id`
- `role`（USER/ASSISTANT/SYSTEM）
- `content`
- `modality`（TEXT/AUDIO/IMAGE/VIDEO/FUSED）
- `risk_level`
- `emotion_label`
- `created_at`

## 6.3 AuditLog

- `id`
- `request_id`
- `event_type`
- `payload_masked`
- `created_at`

## 7. 风险控制策略

## 7.1 风险分级

- `LOW`：一般情绪支持
- `MEDIUM`：明显负面倾向，需加强关怀与建议寻求支持
- `HIGH`：涉及自伤/伤人等危机表达，触发紧急干预模板

## 7.2 响应原则

- 不提供诊断或治疗结论。
- 不生成可能加剧伤害的内容。
- 高风险时优先输出求助资源建议，避免继续深挖细节。

## 8. 多模态处理与情绪融合规范

## 8.1 输入模态

- 文本：直接作为语义输入。
- 语音：经 Whisper 转写为文本。
- 图像：提取表情、神态、环境线索并文本化。
- 视频：优先提取音轨并转写，辅助结合关键帧描述文本化。

## 8.2 统一表示（Textualized Evidence）

每个模态必须转换为标准结构：

```json
{
  "modality": "AUDIO",
  "textEvidence": "用户语速快，反复提到担心工作失误，出现失眠描述",
  "confidence": 0.82
}
```

## 8.3 情绪等级定义（四级）

- `chat`：一般聊天，无明显负面或风险信号。
- `失落`：低落、悲伤、无助等表达占主导。
- `焦虑`：紧张、恐惧、反复担忧、失眠等焦虑特征明显。
- `risk`：自伤/伤人、极端绝望等危机信号。

## 8.4 单模态评分输出

每个模态产出分值向量，范围 `[0,1]`，各项总和为 1：

```json
{
  "modality": "IMAGE",
  "scores": {
    "chat": 0.10,
    "失落": 0.35,
    "焦虑": 0.45,
    "risk": 0.10
  },
  "confidence": 0.74
}
```

## 8.5 融合算法（第一版）

- 设模态集合 `m ∈ {text, audio, image, video}`，每个模态有权重 `w_m`。
- 对每个情绪类别 `c` 计算：`finalScore(c) = Σ (w_m * score_m(c) * confidence_m)`。
- 归一化后选取最大值对应类别作为 `emotionLabel`。
- 安全兜底：任一模态触发高危规则时，将 `emotionLabel` 至少提升为 `risk`。

默认权重（可配置）：

- `text`: 0.40
- `audio`: 0.30
- `image`: 0.20
- `video`: 0.10

## 8.6 回复策略映射

- `chat`：常规倾听 + 轻量引导。
- `失落`：增强共情、鼓励表达、建议社会支持。
- `焦虑`：结构化安抚、呼吸放松建议、聚焦可控行动。
- `risk`：危机干预模板优先，建议联系紧急援助与专业机构。

## 8.7 Agent 工具调用策略（基于情绪标签）

目标：让 Agent 在对话流程中自主决策工具调用，而不是后端写死分支逻辑。

工具定义：

- `sendRiskEmailTool`：发送导员风险通知邮件（MCP Tool）。
- `appendConsultationExcelTool`：追加咨询记录到 Excel（MCP Tool）。

MCP 封装建议：

- 以 `mindagent-tools-mcp` 作为独立 MCP Server，统一暴露工具能力。
- Agent 只感知工具名称与参数 schema，不直接耦合 SMTP/Excel 具体实现。
- 新工具通过新增 MCP Tool 扩展，避免修改核心 Agent 决策逻辑。

触发规则（第一版）：

- 当 `emotionLabel == risk` 时，Agent 必须调用 `sendRiskEmailTool`。
- 当 `emotionLabel != chat` 时，Agent 必须调用 `appendConsultationExcelTool`。

执行顺序建议：

1. 多模态解析 -> 情绪融合 -> 产出 `emotionLabel`。
2. Agent 先生成主回复（含安全策略）。
3. Agent 根据标签调用工具（邮件 / Excel）。
4. 将工具结果写入审计日志并返回调用状态。

幂等与防重复：

- 邮件通知使用 `sessionId + turnId + riskLevel` 作为幂等键。
- Excel 写入使用 `sessionId + turnId` 去重，防止重复记录。

失败处理：

- 工具调用失败不阻断主回复，但必须记录告警并触发重试任务。
- 对 `risk` 邮件工具需至少重试 N 次（可配置），并在最终失败时写高优先级告警。

## 8.8 MCP 工具目录（第一版）

- `sendRiskEmailTool(sessionId, turnId, anonymousUserId, riskLevel, summary, idempotencyKey)`
- `appendConsultationExcelTool(sessionId, turnId, anonymousUserId, emotionLabel, riskLevel, routePolicy, replySummary, idempotencyKey)`

工具治理要求：

- 每个工具必须定义清晰 JSON Schema（必填字段、类型、枚举）。
- 每个工具必须支持幂等键，避免重复执行。
- 每个工具必须输出标准结果：`success`、`errorCode`、`retryable`、`message`。

## 9. 配置规范

- `OLLAMA_BASE_URL`：Ollama 服务地址
- `OLLAMA_MODEL`：默认 `qwen2.5:7b`
- `WHISPER_MODEL`：语音转写模型配置
- `CHAT_TIMEOUT_MS`：模型调用超时
- `MEMORY_WINDOW_SIZE`：短期记忆窗口大小
- `EMOTION_WEIGHT_TEXT`：文本模态权重
- `EMOTION_WEIGHT_AUDIO`：语音模态权重
- `EMOTION_WEIGHT_IMAGE`：图像模态权重
- `EMOTION_WEIGHT_VIDEO`：视频模态权重
- `AGENT_TOOL_EMAIL_ENABLED`：是否启用风险邮件工具
- `AGENT_TOOL_EXCEL_ENABLED`：是否启用 Excel 留档工具
- `AGENT_TOOL_EMAIL_RETRY_MAX`：邮件工具最大重试次数
- `AGENT_TOOL_EXCEL_FILE_PATH`：Excel 存储路径
- `MCP_TOOL_SERVER_ENABLED`：是否启用 MCP 工具服务
- `MCP_TOOL_SERVER_URL`：MCP 服务地址
- `MCP_TOOL_TIMEOUT_MS`：MCP 工具调用超时
- `LOG_MASK_ENABLED`：是否开启日志脱敏

## 10. 可观测性规范

- 每次请求生成 `requestId` 并贯穿日志。
- 记录关键指标：QPS、错误率、P95 延迟、模型超时次数。
- 记录多模态指标：转写时延、图像分析时延、融合耗时、风险触发率。
- 记录工具指标：邮件触发次数、邮件成功率、Excel 写入成功率、工具重试次数。
- 记录 MCP 指标：工具调用耗时、成功率、超时率、schema 校验失败率。
- 日志级别：`INFO` 记录业务路径，`ERROR` 记录异常栈与上下文（脱敏后）。

## 11. 测试策略

- 单元测试：Prompt 组装、风险分类、异常映射。
- 单元测试：情绪评分与融合函数、权重配置加载、风险兜底规则。
- 集成测试：chat API 与 Ollama mock 交互。
- 集成测试：语音转写、图像文本化、视频音轨转写流程。
- 集成测试：`risk` 标签触发邮件工具、非 `chat` 标签触发 Excel 工具。
- 故障测试：邮件/Excel 工具失败重试与幂等去重。
- 集成测试：MCP 工具 schema 校验、连接异常、超时与降级行为。
- 回归用例：正常对话、边界输入、风险语义触发。
- 性能冒烟：并发 5~10 线程验证基本稳定性。

## 12. 部署方案（Docker Compose）

建议包含以下服务：

- `app`：Spring Boot 服务
- `ollama`：模型服务
- `postgres`：会话与日志存储
- `redis`（可选）：短期缓存

## 13. 迭代节奏建议

- 每周一个小里程碑（可演示 + 可验证）。
- 每个里程碑结束更新 `DEVELOPMENT_LOG.md`。
- 每次新增能力必须同时补齐测试和文档。

## 14. 已知风险与缓解

- 模型幻觉风险：加强系统提示词与输出后处理。
- 延迟波动：设置超时和降级文案，必要时减少上下文窗口。
- 安全误判：引入规则 + LLM 双通道校验（后续阶段）。

## 15. DoD（Definition of Done）

一个功能在以下条件满足后才算完成：

1. 代码可编译、可运行。
2. 关键路径测试通过。
3. 文档与配置已更新。
4. 日志和异常处理符合规范。
5. `DEVELOPMENT_LOG.md` 已记录本次变更。

