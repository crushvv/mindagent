# MindAgent Development Log

用于持续记录项目开发进度、关键决策和问题追踪。

## 2026-05-06

### 初始化

- 创建项目目录：`C:\Users\10974\excerise-pros\mindagent`
- 初始化 Git 仓库
- 编写项目启动文档：`README.md`
- 创建开发日志文件：`DEVELOPMENT_LOG.md`

### 当前状态

- 项目阶段：Phase 0（初始化）
- 下一步：搭建 Spring Boot + Spring AI 基础工程并打通 Ollama

### 备注

- 模型目标：`qwen2.5:7b`（Ollama）
- 场景目标：心理咨询支持型 Agent（学习项目）

### 新增进展（Phase 0）

- 搭建 Spring Boot 基础骨架（Maven + Java 17）
- 接入 Spring AI Ollama 基础配置
- 新增 `POST /api/chat` 最小可运行接口
- 新增 Actuator 健康检查配置（`/actuator/health`）
- 增加基础启动测试 `contextLoads`

## 2026-05-07

### 完成事项

- 多模态输入标准化（文本 / 语音转写 / 图像与视频占位路径）
- Whisper：Spring AI `OpenAiAudioTranscriptionModel`；`POST /api/transcribe` 文件上传
- 情绪融合 v1（规则 + 模态权重）与路由策略字段
- 标签驱动工具：`risk` 邮件（JavaMail）、非 `chat` Excel 留档（POI，默认 `excel/consultation_records.xlsx`）
- Maven 依赖与测试：`mvn test` 稳定通过；`.mvn` 镜像配置便于国内构建
- 更新 `README.md` 与配置说明（IDEA 环境变量一次性配置）

### 遇到的问题

- OpenAI Whisper 在云侧需额度与网络；本地开发可延后验证转写或自建兼容服务

### 下一步

- LLM 单模态情绪评分替换规则融合（保留危险词强制 `risk`）
- 接入 RAG；工具层演进为 MCP

---

## 记录模板（复制使用）

```markdown
## YYYY-MM-DD

### 完成事项
- ...

### 决策
- ...

### 遇到的问题
- ...

### 下一步
- ...
```

