# 简历优化器 (Resume Optimizer)

## 项目概述

简历优化器是一个基于 AI 技术的智能简历分析与优化系统，集成 LLM 大模型、RAG 检索增强、ATS 评分等能力，帮助求职者提升简历质量、匹配岗位要求、发现技能差距。

系统采用前后端分离架构，后端基于 Spring Boot 4.0.5 + LangChain4j 1.0.1，前端基于 Vue.js 3.5.30 + ECharts 5.5.0，支持多种 LLM 提供商（OpenAI、通义千问、DeepSeek、MiniMax）。

## 项目背景

### 问题痛点

在求职过程中，求职者面临以下挑战：

1. **简历质量参差不齐** - 缺乏专业的简历撰写指导，难以突出个人优势
2. **岗位匹配度低** - 不了解目标岗位的核心技能要求，简历与岗位脱节
3. **技能差距不明确** - 不知道自己与目标岗位的差距在哪里
4. **优化方向模糊** - 缺乏具体的改进建议和学习路径
5. **ATS 系统过滤** - 简历格式和关键词不符合 ATS 系统要求，被自动过滤

### 解决方案

本项目通过 AI 技术提供以下核心功能：

| 功能模块 | 描述 |
|---------|------|
| 简历解析 | 支持 PDF、Word、TXT 格式简历上传与解析 |
| 智能分析 | 基于关键词和语义分析简历内容，提取技能、分析结构 |
| ATS 综合评分 | LLM+RAG 深度评分，多维度分析（职位匹配度/结构/内容/关键词） |
| 岗位匹配 | 分析简历与目标岗位的匹配度，生成技能雷达图和柱状图 |
| AI 对话 | 基于 LLM 的智能问答与简历生成，支持思考过程展示 |
| RAG 检索 | 检索增强生成，混合检索（向量+关键词）+ 权重排序 |
| 技能差距分析 | 识别缺失技能并提供学习建议和资源链接 |
| 优化建议 | 按优先级排序的针对性改进建议（高/中/低） |
| 模板对比 | 原简历与优化简历的左右对比视图，技能高亮标记 |
| PDF 导出 | 分析报告导出为 PDF 格式 |

## 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Java | 17 | 主要开发语言 |
| Spring Boot | 4.0.5 | 后端框架 |
| LangChain4j | 1.0.1 | LLM 集成框架（OpenAI 兼容协议） |
| Apache PDFBox | 3.0.0 | PDF 文件解析 |
| Apache POI | 5.2.5 | Word 文件解析 |
| DJL | 0.31.1 | 本地向量模型推理（all-MiniLM-L6-v2） |
| Lombok | - | 代码简化（@Data、@Builder） |

### 前端技术

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Vue.js | 3.5.30 | 前端框架（Composition API + `<script setup>`） |
| Vite | 7.3.1 | 构建工具 |
| ECharts | 5.5.0 | 数据可视化（雷达图/柱状图） |
| vue-echarts | 7.0.0 | Vue ECharts 集成 |
| 自定义 Markdown 解析器 | - | AI 回复渲染（无第三方依赖） |

### AI 模型支持

| 提供商 | 模型 | Base URL | 用途 |
|-------|------|----------|-----|
| OpenAI | GPT-4o-mini | https://api.openai.com/v1 | 主力对话模型 |
| 阿里云 DashScope | Qwen-Plus | https://dashscope.aliyuncs.com/compatible-mode/v1 | 国产模型备选 |
| DeepSeek | DeepSeek-Chat | https://api.deepseek.com | 国产模型备选（支持思考过程） |
| Minimax | MiniMax-M2.7 | https://api.minimaxi.com/v1 | 国产模型备选 |
| 本地模型 | all-MiniLM-L6-v2 | - | 向量嵌入模型（DJL 推理） |

## 项目结构

```
resume-optimizer/
├── src/main/java/com/flowerrivertears/resumeoptimizer/
│   ├── config/           # 配置类
│   │   ├── CorsConfig.java          # CORS 跨域配置（仅允许 localhost:5173）
│   │   ├── LlmConfig.java           # LLM 模型配置（4个提供商 + 动态选择）
│   │   └── VectorStoreConfig.java   # 向量存储配置（InMemoryEmbeddingStore）
│   ├── controller/       # 控制器层
│   │   ├── AiAgentController.java   # AI 智能体接口（对话/生成/分析/搜索/RAG/权重/健康检查）
│   │   ├── ApiKeyController.java    # API Key 管理（验证/列表/删除/提供商查询）
│   │   └── ResumeController.java    # 简历处理 + ATS 评分 + 模板生成
│   ├── model/            # 数据模型
│   │   ├── AiChatRequest.java       # AI 聊天请求（message/provider/keyId/context）
│   │   ├── AiChatResponse.java      # AI 聊天响应（answer/thinking/searchResults）
│   │   ├── AnalysisRequest.java     # 分析请求（@Builder 支持）
│   │   ├── AnalysisResponse.java    # 分析响应（技能/分类/差距/建议）
│   │   ├── AtsScoreResponse.java    # ATS 评分响应（多维度 + 分类详情 + 技能差距）
│   │   ├── ApiKeyInfo.java          # API Key 信息（内存存储）
│   │   ├── SearchResult.java        # 检索结果（向量分/关键词分/加权分）
│   │   ├── SkillGap.java            # 技能差距（重要性/建议/资源）
│   │   └── WeightConfig.java        # 权重配置（4维度）
│   ├── service/          # 业务服务
│   │   ├── AiAgentService.java      # AI 智能体服务（多模型/思考解析/Prompt模板）
│   │   ├── AtsScoreService.java     # ATS 评分服务（LLM+RAG深度分析 + 技能推断）
│   │   ├── RagService.java          # RAG 检索服务（混合检索 + 语义分块 + 中文分词）
│   │   ├── WeightedRetrievalService.java  # 权重检索服务（4维度加权排序）
│   │   ├── ApiKeyService.java       # API Key 管理服务（验证/存储/脱敏/模型创建）
│   │   ├── ResumeAnalysisService.java    # 简历分析服务（技能提取/匹配/差距分析）
│   │   └── ResumeTemplateService.java    # 简历模板服务（优化生成/对比视图/高亮）
│   └── util/             # 工具类
│       └── FileParser.java          # 文件解析工具（PDF/DOCX/TXT + 大小/类型校验）
├── src/main/resources/
│   └── application.yaml  # 应用配置（LLM/RAG/权重/CORS）
├── docs/                 # 项目文档
│   ├── README.md         # 项目背景（本文档）
│   ├── ARCHITECTURE.md   # 架构设计
│   ├── FRONTEND.md       # 前端规范
│   ├── SECURITY.md       # 安全规范
│   ├── STYLE.md          # 代码风格
│   └── GUIDE.md          # 文档设计原理
└── pom.xml               # Maven 依赖配置

resume-optimizer-frontend/
├── src/
│   ├── components/       # Vue 组件
│   │   ├── AiChat.vue              # AI 聊天组件（Markdown渲染/思考过程/RAG结果/快捷提问）
│   │   ├── AiAnalysis.vue          # AI 深度分析组件（模型选择/分析报告/RAG引用）
│   │   ├── AnalysisResult.vue      # ATS 评分结果（雷达图/柱状图/关键词/建议/结构/PDF导出）
│   │   ├── ApiKeyInput.vue         # API Key 输入组件（4提供商/高级设置/折叠面板）
│   │   ├── ResumeEditor.vue        # 简历编辑器（文本编辑/职位描述/触发分析）
│   │   ├── ResumeUploader.vue      # 文件上传（拖拽/粘贴/示例简历）
│   │   └── TemplateComparison.vue  # 模板对比（左右对比/技能高亮/使用模板）
│   ├── api.js            # API 接口定义（apiPost/apiGet/apiDelete/apiUpload + renderMarkdown）
│   ├── markdown.js       # Markdown 解析器（自定义实现，XSS防护）
│   ├── main.js           # 应用入口
│   └── App.vue           # 根组件（状态管理/Tab导航/组件编排）
├── package.json          # NPM 依赖配置
└── vite.config.js        # Vite 构建配置（代理到 localhost:9000）
```

## 核心功能

### 1. 简历解析与上传

- 支持 PDF、DOCX、TXT 格式
- 自动提取文本内容
- 文件大小限制 10MB
- 文件类型白名单校验
- 三种输入方式：文件上传（拖拽）、文本粘贴、示例简历

### 2. ATS 综合评分（LLM+RAG 深度分析）

ATS 评分是本系统的核心功能，通过 LLM + RAG 深度检索实现动态分析：

| 评分维度 | 权重 | 描述 |
|---------|------|-----|
| 职位匹配度 | 40% | 技能与岗位匹配程度 |
| 结构评分 | 30% | 简历结构完整性（联系方式/简介/经历/教育/技能） |
| 内容评分 | 30% | 内容丰富度和关键词密度 |

**评分等级：**

| 分数 | 等级 | 描述 |
|-----|------|-----|
| 90-100 | 优秀 | 简历质量优秀，与岗位高度匹配 |
| 75-89 | 良好 | 简历质量良好，基本匹配岗位要求 |
| 60-74 | 一般 | 简历质量一般，部分技能需要补充 |
| 40-59 | 待改进 | 简历需要较多改进 |
| 0-39 | 需重写 | 简历与岗位要求差距较大 |

**分析流程：**

```
用户输入 API Key → 上传简历 → 点击分析
    │
    ├── 有 Key → LLM + RAG 深度分析
    │   ├── RAG 检索：ingestResumeData → hybridSearch → rerank
    │   ├── 基础分析：ResumeAnalysisService.analyze（技能提取/匹配/差距）
    │   ├── 构建 Prompt：简历 + 职位 + 检索结果 + 基础分析
    │   ├── 调用 LLM → 返回 JSON 评分
    │   └── 解析 JSON → 展示结果
    │
    └── 无 Key → 基础分析（自动推断技能）
        ├── 从简历文本识别技能关键词（100+ 技能库）
        ├── 推断常见缺失技能（基于技能分类）
        └── 计算匹配率和评分
```

**展示模块：**

| 模块 | 描述 |
|------|------|
| 📊 评分卡片 | ATS 综合评分 + 职位匹配度 + 匹配/缺失技能数量 |
| 🎯 技能雷达图 | ECharts 雷达图展示各分类（前端/后端/数据库/DevOps）匹配度 |
| 📊 技能柱状图 | ECharts 柱状图展示各分类匹配/缺失技能数量对比 |
| 📋 关键词详情 | 已匹配技能 + 缺失技能 + 学习建议 + 学习资源 |
| 💡 优化建议 | 按优先级排序的优化建议卡片（高🔴/中🟡/低🟢） |
| 🏗️ 结构分析 | 简历结构完整性检查 + 字数统计 |
| 📄 PDF 导出 | 分析报告导出为 PDF 格式 |

### 3. AI 智能体对话

- 基于简历内容的智能问答（自动注入简历上下文）
- 简历生成与优化建议（智能识别"生成简历"类请求）
- 技能差距分析与学习路径推荐
- 思考过程分离展示（DeepSeek 等模型的 `<think/>` 标签）
- RAG 检索结果展示（来源/类别/相关度分数）
- 快捷提问按钮（分析优缺点/生成简历/岗位匹配等）
- 多模型选择（通过已保存的 API Key 切换）

### 4. RAG 检索增强

- **混合检索**：向量检索（语义相似度）+ 关键词检索（TF-IDF）
- **重排序**：综合向量分(60%) + 关键词分(25%) + 内容相关性(15%)
- **语义分块**：按段落进行语义分块，支持重叠
- **中文分词**：bigram 分词 + 停用词过滤
- **四维度权重配置**：技能匹配(35%) + 语义相似度(30%) + 类别相关性(20%) + 经验级别(15%)

### 5. API Key 管理

- 内存安全存储（ConcurrentHashMap），不持久化
- 支持 4 种提供商验证（OpenAI/通义千问/DeepSeek/MiniMax）
- 前端实时联动（Key 验证后自动可用于 ATS 评分和 AI 对话）
- 高级设置（自定义 Base URL 和模型名称）
- Key 脱敏显示（仅显示前4后4位）

### 6. 模板对比

- 左右对比视图（原简历 vs 优化简历）
- 技能高亮标记（已匹配绿色 / 缺失红色删除线）
- 一键使用优化模板
- 复制原简历到剪贴板

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20.19+ 或 22.12+
- Maven 3.6+

### 后端启动

```bash
cd resume-optimizer

# 配置环境变量（可选，也可在前端界面输入 API Key）
export OPENAI_API_KEY=your-api-key

# 启动服务
./mvnw spring-boot:run
```

后端服务运行在 `http://localhost:9000`

启动成功后会看到以下日志：

```
Initializing ChatModel: provider=openai, model=gpt-4o-mini
Initializing DashScope ChatModel: model=qwen-plus
Initializing DeepSeek ChatModel: model=deepseek-chat
Initializing Minimax ChatModel: model=MiniMax-M2.7
Initializing local embedding model (all-MiniLM-L6-v2)
Initializing InMemory EmbeddingStore
Tomcat started on port 9000
```

### 前端启动

```bash
cd resume-optimizer-frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

前端服务运行在 `http://localhost:5173`

### 使用流程

1. 打开前端页面 `http://localhost:5173`
2. 在左侧「AI 模型配置」面板输入 API Key 并验证
3. 上传简历文件（PDF/DOCX/TXT）或粘贴文本
4. 编辑简历内容，可选输入目标职位描述
5. 点击「开始分析」查看 ATS 评分报告
6. 查看技能雷达图、关键词详情、优化建议
7. 使用「模板对比」查看优化版简历
8. 切换到「AI 助手」进行智能对话

## 配置说明

### LLM 模型配置

在 `application.yaml` 中配置：

```yaml
ai:
  llm:
    provider: openai  # 当前激活的默认提供商: openai, dashscope, deepseek, minimax
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 4096
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-plus
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
    minimax:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.com/v1
      model-name: MiniMax-M2.7
```

### RAG 配置

```yaml
ai:
  rag:
    embedding:
      provider: local  # 本地向量模型（all-MiniLM-L6-v2）
    vector-store:
      type: in-memory  # 内存向量存储
      max-results: 5
      min-score: 0.5
```

### 权重配置

```yaml
ai:
  weight:
    enabled: true
    dimensions:
      skill-match: 0.35        # 技能匹配权重
      semantic-similarity: 0.30 # 语义相似度权重
      category-relevance: 0.20  # 类别相关性权重
      experience-level: 0.15    # 经验级别权重
```

## API 接口

### 简历解析

```
POST /api/parse
Content-Type: multipart/form-data

参数: file (PDF/DOCX/TXT, 最大 10MB)
返回: { content: "解析后的文本内容" }
```

### 简历分析

```
POST /api/analyze
Content-Type: application/json

参数: { resumeText, jobDescription }
返回: {
  foundKeywords: ["Java", "Vue.js", ...],
  missingKeywords: ["Docker", "Kubernetes", ...],
  skillGaps: [{ skill, category, importance, reason, suggestion, learningResources }],
  categoryScores: { frontend, backend, database, devops },
  structureAnalysis: { hasContact, hasSummary, hasExperience, hasEducation, hasSkills, wordCount },
  suggestions: [{ type, title, description, priority, impact }]
}
```

### ATS 综合评分

```
POST /api/ats-score
Content-Type: application/json

参数: { resumeText, jobDescription, provider, keyId }
返回: {
  overallScore: 85,
  grade: "优秀",
  gradeDescription: "简历质量优秀...",
  jobMatchScore: { score, level, description, totalRequired, matchedRequired },
  structureScore: { score, hasContact, hasSummary, hasExperience, hasEducation, hasSkills, wordCount },
  contentScore: { score, skillCount, keywordDensity, densityLevel },
  keywordScore: { score, totalKeywords, matchedKeywords, matchRate },
  matchedSkills: ["Java", "Vue.js", ...],
  missingSkills: ["Docker", "Kubernetes", ...],
  matchedCount: 9,
  missingCount: 3,
  categoryDetails: {
    frontend: { name, score, level, matched, total, matchedSkills, missingSkills },
    backend: { ... },
    database: { ... },
    devops: { ... }
  },
  skillGapDetails: [ { skill, category, importance, reason, suggestion, learningResources } ],
  optimizationSuggestions: [ { type, title, description, priority, impact } ],
  responseTimeMs: 1234,
  provider: "llm-enhanced",
  keyId: "xxx"
}
```

### AI 对话

```
POST /api/ai/chat
Content-Type: application/json

参数: { message, provider, keyId, context }
返回: { answer, thinking, searchResults, provider, model, responseTimeMs }
```

### AI 简历生成

```
POST /api/ai/generate-resume
Content-Type: application/json

参数: { userInput, provider, keyId }
返回: { answer, searchResults, provider, model, responseTimeMs }
```

### AI 深度分析

```
POST /api/ai/analyze?keyId=xxx
Content-Type: application/json

参数: { resumeText, jobDescription }
返回: { answer, thinking, searchResults, provider, model, responseTimeMs }
```

### AI 增强搜索

```
POST /api/ai/search
Content-Type: application/json

参数: { query, matchedSkills, jobSkills, provider, keyId }
返回: { answer, searchResults, provider, model, responseTimeMs }
```

### RAG 知识库管理

```
POST   /api/ai/rag/ingest       # 单文档导入
POST   /api/ai/rag/ingest-batch # 批量文档导入
POST   /api/ai/rag/search       # 知识库搜索
DELETE /api/ai/rag/clear        # 清空知识库
```

### 权重配置管理

```
GET  /api/ai/weight/config              # 获取当前权重配置
PUT  /api/ai/weight/config              # 更新权重配置
POST /api/ai/weight/config/{name}       # 保存自定义权重配置
GET  /api/ai/weight/config/{name}       # 获取自定义权重配置
DELETE /api/ai/weight/config/{name}     # 删除自定义权重配置
```

### API Key 管理

```
POST   /api/ai/keys/validate    # 验证 API Key（发送测试请求）
GET    /api/ai/keys/list        # 列出已存储的 Key
GET    /api/ai/keys/{keyId}     # 获取指定 Key 信息
DELETE /api/ai/keys/{keyId}     # 删除指定 Key
GET    /api/ai/keys/providers   # 获取支持的提供商列表
```

### 简历优化与模板

```
POST /api/optimize              # 生成优化版简历及对比视图
GET  /api/templates/{type}      # 获取简历模板（default/tech/senior）
```

### 健康检查

```
GET /api/health                 # 简历服务健康检查
GET /api/ai/health              # AI 服务健康检查（返回模型/知识库状态）
```

## 文档索引

| 文档 | 描述 |
|-----|------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 系统架构设计文档 |
| [FRONTEND.md](./FRONTEND.md) | 前端开发规范 |
| [SECURITY.md](./SECURITY.md) | 安全规范文档 |
| [STYLE.md](./STYLE.md) | 代码风格指南 |
| [GUIDE.md](./GUIDE.md) | 文档设计原理讲解 |

## 许可证

本项目仅供学习和研究使用。

## 贡献指南

欢迎提交 Issue 和 Pull Request。

---

**注意**: 本项目使用 AI 技术处理用户数据，所有数据仅在本地处理，保护用户隐私。API Key 仅存储在内存中，应用重启后清空。
