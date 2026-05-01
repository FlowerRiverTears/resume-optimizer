# 简历优化器 (Resume Optimizer)

## 项目概述

简历优化器是一个基于 AI 技术的智能简历分析与优化系统，帮助求职者提升简历质量、匹配岗位要求、发现技能差距。

## 项目背景

### 问题痛点

在求职过程中，求职者面临以下挑战：

1. **简历质量参差不齐** - 缺乏专业的简历撰写指导，难以突出个人优势
2. **岗位匹配度低** - 不了解目标岗位的核心技能要求，简历与岗位脱节
3. **技能差距不明确** - 不知道自己与目标岗位的差距在哪里
4. **优化方向模糊** - 缺乏具体的改进建议和学习路径

### 解决方案

本项目通过 AI 技术提供以下核心功能：

| 功能模块 | 描述 |
|---------|------|
| 简历解析 | 支持 PDF、Word、TXT 格式简历上传与解析 |
| 智能分析 | 基于关键词和语义分析简历内容 |
| 岗位匹配 | 分析简历与目标岗位的匹配度 |
| AI 对话 | 基于 LLM 的智能问答与简历生成 |
| RAG 检索 | 检索增强生成，提供精准建议 |
| 技能差距分析 | 识别缺失技能并提供学习建议 |

## 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Java | 17 | 主要开发语言 |
| Spring Boot | 4.0.5 | 后端框架 |
| LangChain4j | 1.0.1 | LLM 集成框架 |
| Apache PDFBox | 3.0.0 | PDF 文件解析 |
| Apache POI | 5.2.5 | Word 文件解析 |
| Lombok | - | 代码简化 |

### 前端技术

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Vue.js | 3.5.30 | 前端框架 |
| Vite | 7.3.1 | 构建工具 |
| ECharts | 5.5.0 | 数据可视化 |
| vue-echarts | 7.0.0 | Vue ECharts 集成 |

### AI 模型支持

| 提供商 | 模型 | 用途 |
|-------|------|-----|
| OpenAI | GPT-4o-mini | 主力对话模型 |
| 阿里云 DashScope | Qwen-Plus | 国产模型备选 |
| DeepSeek | DeepSeek-Chat | 国产模型备选 |
| Minimax | MiniMax-M2.7 | 国产模型备选 |
| 本地模型 | all-MiniLM-L6-v2 | 向量嵌入模型 |

## 项目结构

```
resume-optimizer/
├── src/main/java/com/flowerrivertears/resumeoptimizer/
│   ├── config/           # 配置类
│   │   ├── CorsConfig.java          # CORS 跨域配置
│   │   ├── LlmConfig.java           # LLM 模型配置
│   │   └── VectorStoreConfig.java   # 向量存储配置
│   ├── controller/       # 控制器层
│   │   ├── AiAgentController.java   # AI 智能体接口
│   │   ├── ApiKeyController.java    # API Key 管理
│   │   └── ResumeController.java    # 简历处理接口
│   ├── model/            # 数据模型
│   │   ├── AiChatRequest.java       # AI 聊天请求
│   │   ├── AiChatResponse.java      # AI 聊天响应
│   │   └── ...
│   ├── service/          # 业务服务
│   │   ├── AiAgentService.java      # AI 智能体服务
│   │   ├── AtsScoreService.java     # ATS 评分服务
│   │   ├── RagService.java          # RAG 检索服务
│   │   ├── WeightedRetrievalService.java  # 权重检索
│   │   └── ...
│   └── util/             # 工具类
│       └── FileParser.java          # 文件解析工具
├── src/main/resources/
│   └── application.yaml  # 应用配置
└── pom.xml               # Maven 依赖配置

resume-optimizer-frontend/
├── src/
│   ├── components/       # Vue 组件
│   │   ├── AiChat.vue              # AI 聊天组件
│   │   ├── AiAnalysis.vue          # AI 分析组件
│   │   ├── ResumeEditor.vue        # 简历编辑器
│   │   ├── ResumeUploader.vue      # 文件上传
│   │   └── ...
│   ├── api.js            # API 接口定义
│   ├── markdown.js       # Markdown 解析器
│   └── App.vue           # 根组件
├── package.json          # NPM 依赖配置
└── vite.config.js        # Vite 构建配置
```

## 核心功能

### 1. 简历解析与上传

- 支持 PDF、DOCX、TXT 格式
- 自动提取文本内容
- 文件大小限制 10MB

### 2. 智能简历分析

- 关键词提取与匹配
- 技能识别与分类
- 岗位匹配度计算

### 3. ATS 综合评分

- 综合评分计算（0-100分）
- 多维度分析（结构、内容、关键词）
- 评分等级（优秀/良好/一般/待改进/需重写）
- 技能雷达图可视化
- 智能优化建议
- 结构完整性检查

### 4. AI 智能体对话

- 基于简历内容的智能问答
- 简历生成与优化建议
- 技能差距分析与学习路径推荐

### 5. RAG 检索增强

- 向量检索 + 关键词检索混合
- 权重排序优化结果
- 语义分块提高检索精度

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20.19+ 或 22.12+
- Maven 3.6+

### 后端启动

```bash
cd resume-optimizer

# 配置环境变量（可选）
export OPENAI_API_KEY=your-api-key

# 启动服务
./mvnw spring-boot:run
```

后端服务运行在 `http://localhost:9000`

### 前端启动

```bash
cd resume-optimizer-frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

前端服务运行在 `http://localhost:5173`

## 配置说明

### LLM 模型配置

在 `application.yaml` 中配置：

```yaml
ai:
  llm:
    provider: openai  # 可选: openai, dashscope, deepseek, minimax
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      model-name: gpt-4o-mini
```

### RAG 配置

```yaml
ai:
  rag:
    embedding:
      provider: local  # 本地向量模型
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

参数: file (PDF/DOCX/TXT)
返回: { content: "解析后的文本内容" }
```

### 简历分析

```
POST /api/analyze
Content-Type: application/json

参数: { resumeText, jobDescription }
返回: { foundKeywords, missingKeywords, skillGaps, ... }
```

### ATS 综合评分

```
POST /api/ats-score
Content-Type: application/json

参数: { resumeText, jobDescription }
返回: {
  overallScore: 85,
  grade: "优秀",
  gradeDescription: "简历质量优秀...",
  jobMatchScore: { score, level, description },
  structureScore: { score, hasContact, hasSummary, ... },
  contentScore: { score, skillCount, keywordDensity },
  keywordScore: { score, totalKeywords, matchedKeywords },
  categoryDetails: { frontend: { score, matched, total }, ... },
  skillGapDetails: [ { skill, category, importance, suggestion } ],
  optimizationSuggestions: [ { type, title, description, priority } ]
}
```

### AI 对话

```
POST /api/ai/chat
Content-Type: application/json

参数: { message, provider, keyId }
返回: { answer, thinking, searchResults, ... }
```

### AI 简历生成

```
POST /api/ai/generate-resume
Content-Type: application/json

参数: { userInput, provider, keyId }
返回: { answer, searchResults, ... }
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

**注意**: 本项目使用 AI 技术处理用户数据，所有数据仅在本地处理，保护用户隐私。
