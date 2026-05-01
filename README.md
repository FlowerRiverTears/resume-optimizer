# 简历优化器 (Resume Optimizer)

**[English](./README.en.md)** | **中文**

---

基于 AI 技术的智能简历分析与优化系统，集成 LLM 大模型、RAG 检索增强、ATS 评分等能力，帮助求职者提升简历质量、匹配岗位要求、发现技能差距。

系统采用前后端分离架构，后端基于 Spring Boot 4.0.5 + LangChain4j 1.0.1，前端基于 Vue.js 3.5.30 + ECharts 5.5.0，支持多种 LLM 提供商（OpenAI、通义千问、DeepSeek、MiniMax）。

## 核心功能

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

### 后端

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Java | 17 | 主要开发语言 |
| Spring Boot | 4.0.5 | 后端框架 |
| LangChain4j | 1.0.1 | LLM 集成框架（OpenAI 兼容协议） |
| Apache PDFBox | 3.0.0 | PDF 文件解析 |
| Apache POI | 5.2.5 | Word 文件解析 |
| DJL | 0.31.1 | 本地向量模型推理（all-MiniLM-L6-v2） |

### 前端

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Vue.js | 3.5.30 | 前端框架（Composition API + `<script setup>`） |
| Vite | 7.3.1 | 构建工具 |
| ECharts | 5.5.0 | 数据可视化（雷达图/柱状图） |

### AI 模型支持

| 提供商 | 模型 | 用途 |
|-------|------|-----|
| OpenAI | GPT-4o-mini | 主力对话模型 |
| 阿里云 DashScope | Qwen-Plus | 国产模型备选 |
| DeepSeek | DeepSeek-Chat | 国产模型备选（支持思考过程） |
| Minimax | MiniMax-M2.7 | 国产模型备选 |
| 本地模型 | all-MiniLM-L6-v2 | 向量嵌入模型（DJL 推理） |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20.19+ 或 22.12+
- Maven 3.6+

### 后端启动

```bash
cd resume-optimizer
./mvnw spring-boot:run
```

后端服务运行在 `http://localhost:9000`

### 前端启动

```bash
cd resume-optimizer-frontend
npm install
npm run dev
```

前端服务运行在 `http://localhost:5173`

### 使用流程

1. 打开前端页面 `http://localhost:5173`
2. 在左侧「AI 模型配置」面板输入 API Key 并验证
3. 上传简历文件（PDF/DOCX/TXT）或粘贴文本
4. 点击「开始分析」查看 ATS 评分报告
5. 查看技能雷达图、关键词详情、优化建议
6. 切换到「AI 助手」进行智能对话

## API 接口

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/api/parse` | POST | 简历文件解析 |
| `/api/analyze` | POST | 简历文本分析 |
| `/api/ats-score` | POST | ATS 综合评分（LLM+RAG） |
| `/api/optimize` | POST | 生成优化版简历 |
| `/api/ai/chat` | POST | AI 智能对话 |
| `/api/ai/generate-resume` | POST | AI 简历生成 |
| `/api/ai/analyze` | POST | AI 深度分析 |
| `/api/ai/rag/ingest` | POST | RAG 文档导入 |
| `/api/ai/rag/search` | POST | RAG 知识库搜索 |
| `/api/ai/keys/validate` | POST | API Key 验证 |
| `/api/health` | GET | 健康检查 |

详细 API 文档请参见 [docs/README.md](./docs/README.md)

## 项目结构

```
resume-optimizer/                    # 后端项目
├── src/main/java/.../resumeoptimizer/
│   ├── config/                      # 配置类（CORS/LLM/向量存储）
│   ├── controller/                  # 控制器（AI/Key/简历）
│   ├── model/                       # 数据模型
│   ├── service/                     # 业务服务（AI/RAG/ATS/权重/分析/模板）
│   └── util/                        # 工具类（文件解析）
├── docs/                            # 项目文档
└── pom.xml                          # Maven 配置

resume-optimizer-frontend/           # 前端项目
├── src/
│   ├── components/                  # Vue 组件（7个）
│   ├── api.js                       # API 接口定义
│   ├── markdown.js                  # Markdown 解析器
│   └── App.vue                      # 根组件
└── vite.config.js                   # Vite 配置
```

## 文档索引

| 文档 | 描述 |
|-----|------|
| [docs/README.md](./docs/README.md) | 项目背景与完整 API 文档 |
| [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) | 系统架构设计 |
| [docs/FRONTEND.md](./docs/FRONTEND.md) | 前端开发规范 |
| [docs/SECURITY.md](./docs/SECURITY.md) | 安全规范 |
| [docs/STYLE.md](./docs/STYLE.md) | 代码风格指南 |
| [docs/GUIDE.md](./docs/GUIDE.md) | 文档设计原理 |

## 隐私声明

- 所有简历数据仅在本地处理，不上传到第三方
- API Key 仅存储在内存中，应用重启后清空
- 简历内容不持久化存储

## 许可证

本项目仅供学习和研究使用。
