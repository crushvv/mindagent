# MindAgent

一个用于学习多模态 Agent 开发的心理咨询助手项目，技术栈基于 **Java + Spring Boot + Spring AI**，对话模型通过 **Ollama** 使用 `qwen2.5:7b`，语音转写通过 **Spring AI OpenAI 兼容接口**（可接 OpenAI Whisper 或同类服务）。项目目标：可运行、可测试、可迭代，后续再逐步替换为文档中的完整架构（MCP 工具、RAG、LLM 单模态评分等）。

## 当前已实现（2026-05-07）

- **Spring Boot 3.4 + Java 17** 可运行后端，`mvn test` 通过。
- **`POST /api/chat`**：多模态入参（`message` + `modalInputs`）、输入标准化、**规则版情绪融合**（四类标签 + 权重）、调用 Ollama 生成回复；响应中含 `toolExecutionResults`。
- **`POST /api/transcribe`**：`multipart/form-data` 上传音频，走 Spring AI `OpenAiAudioTranscriptionModel` 转写。
- **标签驱动工具链（首版实现）**：`risk` 时 **JavaMail** 发预警邮件；非 `chat` 时用 **Apache POI** 追加写入项目根目录下 **`excel/consultation_records.xlsx`**（目录自动创建）。后续可替换为 MCP 工具实现，接口 `AgentToolService` 保持不变。
- **Actuator**：`GET /actuator/health` 健康检查。
- **Maven 国内镜像**：项目内 `.mvn/settings.xml` + `maven.config`（可选，便于依赖下载）。

> 说明：文档中「单模态由大模型判情绪 + 再融合」为**目标设计**；当前仓库里融合逻辑仍为**可跑通的规则版**，便于先打通链路，下一步再替换为 LLM 评分。

## 快速启动

```bash
cd mindagent
mvn test
mvn spring-boot:run
```

浏览器可访问：`http://localhost:8080/actuator/health`（应看到 `{"status":"UP",...}`）。

## 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | JSON 体：`sessionId`, `userId`, `message`, `modalInputs`（可选） |
| POST | `/api/transcribe` | `form-data` 字段 `file`：上传 wav 等音频 |
| GET | `/actuator/health` | 健康检查 |

## 环境变量（建议在 IDEA Maven 运行配置里一次性配置）

**邮件（QQ SMTP 示例）**

- `MAIL_HOST`（默认 `smtp.qq.com`）
- `MAIL_PORT`（默认 `587`）
- `MAIL_USERNAME`：发件 QQ 邮箱
- `MAIL_PASSWORD`：QQ 邮箱 **SMTP 授权码**
- `RISK_EMAIL_TO`：风险预警收件人（默认 `3373347091@qq.com`，可按需覆盖）

**Excel 留档**

- `EXCEL_FILE_PATH`：默认 `./excel/consultation_records.xlsx`

**Ollama 对话**

- `OLLAMA_BASE_URL`（默认 `http://localhost:11434`）
- `OLLAMA_MODEL`（默认 `qwen2.5:7b`）

**Whisper 转写（可选）**

- `WHISPER_ENABLED`、`WHISPER_BASE_URL`、`WHISPER_MODEL`、`WHISPER_API_KEY`

工具开关：`AGENT_TOOL_EMAIL_ENABLED`、`AGENT_TOOL_EXCEL_ENABLED`（默认 `true`）。

IDEA：**Edit Configurations → Maven → Command line: `spring-boot:run` → Environment variables** 填入上表，可长期保存，无需每次在终端 export。

---

## 1. 项目目标

- 学习并实践 Spring AI 的 Agent 设计方式（Prompt、Tool、Memory、RAG、Workflow）。
- 构建可演进的心理咨询场景 Agent（支持性对话为主，不替代专业医疗诊断）。
- 建立可持续迭代的工程结构：可运行、可测试、可部署、可记录开发进度。

## 2. 功能边界（MVP）

- 文本对话；多模态输入结构已具备（语音转写、图像/视频仍为占位或简化路径）。
- 心理咨询风格回复：共情、澄清、支持，不做医疗诊断。
- 安全策略：危险词命中判 `risk`；危机场景需人工与专业机构介入。
- 会话与留档：Excel 本地追加（首版）；后续可接数据库与审计日志。

## 3. 技术方案

- Java 17+、Spring Boot、Spring AI（Ollama Chat + OpenAI 兼容 Audio）
- 邮件：`spring-boot-starter-mail`（JavaMail）
- Excel：`Apache POI`
- 工具协议（规划）：MCP 统一封装外部工具；当前为直连实现
- 容器化（规划）：Docker / Docker Compose

## 4. 系统架构（目标）

1. API：`/api/chat`、`/api/transcribe`、Actuator  
2. Core：多模态标准化、情绪融合、路由策略、工具编排  
3. Infra：Ollama、Whisper、后续 RAG/向量库、MCP  
4. `deploy`：Compose（待补）

### 4.1 Spring Boot 上线能力（目标）

统一异常处理、配置分环境、健康探针、日志与观测、容器发布。

## 5. 情绪标签与路由（设计）

- `chat`：直答（设计上不走 RAG）。  
- `失落` / `焦虑` / `risk`：设计上走 RAG 增强（**RAG 代码待接**）。  
- 危险关键词：**直接 `risk`**，优先于普通融合。

### 5.1 情绪识别与融合原则（文档目标）

- 单模态由大模型给出情绪分值（待实现）。  
- 融合按模态权重合并，输出最终标签。  
- 危险词硬规则优先。

## 6. 风险通知与咨询留档

- `risk`：发预警邮件（当前 JavaMail）。  
- 非 `chat`：写 Excel（当前 POI，路径 `excel/`）。  
- 敏感信息最小化；生产环境需鉴权、审计与幂等（见 `docs/engineering-spec.md`）。

## 7. 开发阶段规划（简）

- Phase 0：工程可运行 — **进行中/基本具备**  
- Phase 1：MVP 咨询 + 记忆 + 安全模板 + RAG  
- Phase 2：LLM 单模态评分、多模态真实理解、观测与评测  
- Phase 3：Compose、CI、部署文档  

## 8. 仓库结构（当前单模块）

```text
mindagent/
  ├─ pom.xml
  ├─ README.md
  ├─ DEVELOPMENT_LOG.md
  ├─ docs/engineering-spec.md
  ├─ .mvn/
  ├─ src/main/java/com/mindagent/...
  └─ src/main/resources/application.yml
```

## 9. 安全与伦理声明

本项目用于学习，不提供医疗建议。危机内容应引导用户联系当地紧急救助与专业机构。勿将真实 API Key、授权码提交到公开仓库。

## 10. 下一步（建议）

1. 将情绪单模态评分改为 **LLM 输出结构化分值**（保留危险词兜底）。  
2. 接入 **RAG**（向量库 + 检索）。  
3. 将 `AgentToolService` 实现替换为 **MCP** 调用，保留 JavaMail/Excel 为 Tool 实现之一。  
4. 补充 `docker-compose` 与生产配置模板。

---

更细的接口契约与融合规范见 **`docs/engineering-spec.md`**；日常进度见 **`DEVELOPMENT_LOG.md`**。
