# 系统架构设计

## 架构概述

简历优化器采用**前后端分离**架构，后端基于 Spring Boot 提供 RESTful API，前端基于 Vue.js 构建单页应用。系统核心围绕 LLM + RAG 构建，支持多模型切换和混合检索。

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层 (Vue.js 3)                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │AiChat    │ │AiAnalysis│ │Analysis  │ │ResumeEdit│           │
│  │(Markdown)│ │(Markdown)│ │Result    │ │+Uploader │           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
│       │            │            │            │                   │
│  ┌────┴────────────┴────────────┴────────────┴────┐            │
│  │  ApiKeyInput │ TemplateComparison               │            │
│  └──────────────────┬──────────────────────────────┘            │
│                     │                                           │
│               api.js (API 封装 + renderMarkdown)               │
└─────────────────────┬───────────────────────────────────────────┘
                      │ HTTP/REST (CORS: localhost:5173)
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后端层 (Spring Boot 4.0.5)                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Controller 层                          │  │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐           │  │
│  │  │ResumeCtrl  │ │AiAgentCtrl │ │ApiKeyCtrl  │           │  │
│  │  │/api/*      │ │/api/ai/*   │ │/api/ai/keys│           │  │
│  │  └─────┬──────┘ └─────┬──────┘ └─────┬──────┘           │  │
│  └────────┼──────────────┼──────────────┼───────────────────┘  │
│           │              │              │                       │
│  ┌────────▼──────────────▼──────────────▼───────────────────┐  │
│  │                    Service 层                             │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │  │
│  │  │ResumeSvc │ │AiAgentSvc│ │RagService│ │ApiKeySvc │    │  │
│  │  │(技能提取)│ │(多模型)  │ │(混合检索)│ │(内存存储)│    │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │  │
│  │  ┌────────────────┐ ┌──────────────────────────────┐    │  │
│  │  │AtsScoreService│ │WeightedRetrievalService      │    │  │
│  │  │(LLM+RAG评分) │ │(4维度加权排序)              │    │  │
│  │  └────────────────┘ └──────────────────────────────┘    │  │
│  │  ┌────────────────────┐                                 │  │
│  │  │ResumeTemplateSvc   │                                 │  │
│  │  │(优化生成/对比视图) │                                 │  │
│  │  └────────────────────┘                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│           │              │                                       │
│  ┌────────▼──────────────▼──────────────────────────────────┐  │
│  │                    配置层 (Config)                         │  │
│  │  ┌──────────┐ ┌──────────────┐ ┌────────────────┐       │  │
│  │  │LlmConfig │ │VectorStoreCfg│ │CorsConfig      │       │  │
│  │  │(4提供商) │ │(InMemory)    │ │(localhost:5173)│       │  │
│  │  └──────────┘ └──────────────┘ └────────────────┘       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     外部服务层                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ OpenAI   │ │DashScope │ │DeepSeek  │ │Minimax   │           │
│  │ GPT-4o   │ │Qwen-Plus │ │Chat      │ │M2.7      │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│                                                                 │
│  ┌──────────────────────────────────────────────────┐          │
│  │     本地向量模型 (all-MiniLM-L6-v2 / DJL)        │          │
│  └──────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## 核心模块设计

### 1. LLM 集成模块

#### 设计目标

- 支持多个 LLM 提供商（OpenAI 兼容协议）
- 统一的 API 接口（LangChain4j ChatModel）
- 灵活的配置管理（YAML + 用户自定义 Key）
- 动态模型选择（默认配置 + 用户 Key 覆盖）

#### 类图

```
┌─────────────────────────────────────────────────────┐
│                   LlmConfig                         │
│─────────────────────────────────────────────────────│
│ - provider: String                                  │
│ - openai: OpenAiProvider                           │
│ - dashscope: OpenAiProvider                        │
│ - deepseek: OpenAiProvider                         │
│ - minimax: OpenAiProvider                          │
│─────────────────────────────────────────────────────│
│ + chatModel(): ChatModel           (默认模型)       │
│ + openaiChatModel(): ChatModel     (OpenAI)        │
│ + dashscopeChatModel(): ChatModel  (通义千问)      │
│ + deepseekChatModel(): ChatModel   (DeepSeek)      │
│ + minimaxChatModel(): ChatModel    (MiniMax)       │
│ + selectPreconfiguredModel(provider): ChatModel    │
└─────────────────────────────────────────────────────┘
                         │
                         │ creates
                         ▼
┌─────────────────────────────────────────────────────┐
│              OpenAiChatModel (LangChain4j 1.0.1)    │
│─────────────────────────────────────────────────────│
│ - apiKey: String                                    │
│ - baseUrl: String                                   │
│ - modelName: String                                 │
│ - temperature: double                               │
│ - maxTokens: int (4096)                            │
└─────────────────────────────────────────────────────┘
```

#### 模型选择流程

```
用户请求（provider/keyId）
    │
    ├── 有 keyId → ApiKeyService.createChatModel(keyId)
    │   └── 从内存存储获取 Key 信息 → 创建临时 ChatModel
    │
    ├── 有 provider → LlmConfig.selectPreconfiguredModel(provider)
    │   └── 使用 application.yaml 中预配置的 Key
    │
    └── 无参数 → LlmConfig.chatModel()
        └── 使用默认 provider 配置
```

#### 配置示例

```yaml
ai:
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 4096
```

### 2. RAG 检索增强模块

#### 设计目标

- 混合检索：向量检索 + 关键词检索
- 语义分块：智能文档切分（支持重叠）
- 权重排序：多维度结果优化
- 中文支持：bigram 分词 + 停用词过滤

#### 流程图

```
用户查询
    │
    ▼
┌───────────────────────────────────────────────────────┐
│                   RagService                          │
│                                                       │
│  ┌─────────────────┐    ┌─────────────────┐         │
│  │  向量检索        │    │  关键词检索      │         │
│  │  (Embedding)    │    │  (TF-IDF)       │         │
│  │  all-MiniLM-L6  │    │  bigram分词     │         │
│  └────────┬────────┘    └────────┬────────┘         │
│           │                      │                   │
│           └──────────┬───────────┘                   │
│                      ▼                               │
│           ┌─────────────────────┐                   │
│           │     结果合并         │                   │
│           │  (去重+分数归一化)   │                   │
│           └──────────┬──────────┘                   │
│                      ▼                               │
│           ┌─────────────────────┐                   │
│           │     重排序          │                   │
│           │  向量60%+关键词25%  │                   │
│           │  +内容相关性15%     │                   │
│           └──────────┬──────────┘                   │
│                      │                               │
└──────────────────────┼───────────────────────────────┘
                       ▼
              检索结果列表 (SearchResult)
              ├── score (向量分)
              ├── keywordScore (关键词分)
              ├── weightedScore (加权分)
              ├── source (来源)
              └── category (类别)
```

#### 核心方法

```java
public List<SearchResult> hybridSearch(String query, int maxResults, double minScore) {
    List<SearchResult> vectorResults = vectorSearch(query, maxResults * 2, minScore);
    List<SearchResult> keywordResults = keywordSearch(query, maxResults * 2);
    Map<String, SearchResult> merged = mergeResults(vectorResults, keywordResults);
    List<SearchResult> reranked = rerank(new ArrayList<>(merged.values()), query);
    return reranked.stream().limit(maxResults).collect(Collectors.toList());
}
```

#### 语义分块

```java
private List<String> semanticChunk(String content, int maxChunkSize, int overlap) {
    List<String> paragraphs = Arrays.asList(content.split("\n\n+"));
    List<String> chunks = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    
    for (String para : paragraphs) {
        if (current.length() + para.length() > maxChunkSize && current.length() > 0) {
            chunks.add(current.toString().trim());
            // 保留重叠部分
            String[] words = current.toString().split("\\s+");
            int overlapLen = Math.min(overlap, words.length);
            current = new StringBuilder();
            for (int i = words.length - overlapLen; i < words.length; i++) {
                current.append(words[i]).append(" ");
            }
        }
        current.append(para).append("\n\n");
    }
    if (current.length() > 0) chunks.add(current.toString().trim());
    return chunks;
}
```

### 3. 权重检索模块

#### 设计目标

- 多维度权重配置
- 动态权重调整（API 接口）
- 可扩展的评分维度
- 自定义权重配置保存/加载

#### 权重维度

| 维度 | 权重 | 描述 | 计算方式 |
|-----|------|-----|---------|
| skill-match | 0.35 | 技能匹配度 | 结果中匹配技能数 / 总技能数 |
| semantic-similarity | 0.30 | 语义相似度 | 向量检索原始分数 |
| category-relevance | 0.20 | 类别相关性 | 查询类别与结果类别匹配度 |
| experience-level | 0.15 | 经验级别 | 内容中经验指标（年限/级别） |

#### 计算公式

```
weightedScore = Σ(dimension_score × dimension_weight)
```

```java
public List<SearchResult> applyWeights(List<SearchResult> results, 
                                        String query,
                                        Set<String> matchedSkills,
                                        Set<String> jobSkills) {
    WeightConfig config = getCurrentConfig();
    
    for (SearchResult result : results) {
        double skillScore = calculateSkillMatch(result, matchedSkills, jobSkills);
        double semanticScore = result.getScore();
        double categoryScore = calculateCategoryRelevance(result, query);
        double experienceScore = calculateExperienceLevel(result);
        
        double weightedScore = 
            skillScore * config.getSkillMatch() +
            semanticScore * config.getSemanticSimilarity() +
            categoryScore * config.getCategoryRelevance() +
            experienceScore * config.getExperienceLevel();
        
        result.setWeightedScore(weightedScore);
    }
    
    return results.stream()
        .sorted(Comparator.comparingDouble(SearchResult::getWeightedScore).reversed())
        .collect(Collectors.toList());
}
```

### 4. ATS 评分模块

#### 设计目标

- 综合评分计算（多维度加权）
- LLM + RAG 深度检索增强
- 无 Key 时自动推断技能
- 智能优化建议生成

#### 评分维度

| 维度 | 权重 | 描述 |
|-----|------|-----|
| 职位匹配度 | 40% | 技能与岗位匹配程度 |
| 结构评分 | 30% | 简历结构完整性 |
| 内容评分 | 30% | 内容丰富度和关键词密度 |

#### 评分等级

| 分数范围 | 等级 | 描述 |
|---------|------|-----|
| 90-100 | 优秀 | 简历质量优秀，与岗位高度匹配 |
| 75-89 | 良好 | 简历质量良好，基本匹配岗位要求 |
| 60-74 | 一般 | 简历质量一般，部分技能需要补充 |
| 40-59 | 待改进 | 简历需要较多改进 |
| 0-39 | 需重写 | 简历与岗位要求差距较大 |

#### 评分流程

```
用户请求 ATS 评分
    │
    ├── 有 API Key → LLM + RAG 深度分析
    │   │
    │   ├── 1. 基础分析 (ResumeAnalysisService.analyze)
    │   │   ├── 技能提取（100+ 技能库 + 同义词归一化）
    │   │   ├── 匹配度计算（支持 OR 类别）
    │   │   ├── 分类评分（前端/后端/数据库/DevOps）
    │   │   └── 结构分析 + 关键词词频
    │   │
    │   ├── 2. RAG 检索 (RagService.hybridSearch)
    │   │   ├── 向量检索 + 关键词检索
    │   │   └── 重排序 → 获取相关岗位数据
    │   │
    │   ├── 3. 构建 Prompt
    │   │   ├── 系统提示词（评分规则 + JSON 格式要求）
    │   │   ├── 简历内容
    │   │   ├── 职位描述（可选）
    │   │   ├── RAG 检索结果
    │   │   └── 基础分析数据
    │   │
    │   ├── 4. 调用 LLM → 返回 JSON 评分
    │   │
    │   └── 5. 解析 JSON → AtsScoreResponse
    │       ├── 整体评分 + 等级
    │       ├── 各维度评分
    │       ├── 分类详情
    │       ├── 技能差距
    │       └── 优化建议
    │
    └── 无 API Key → 基础分析（自动推断技能）
        ├── inferMatchedSkillsFromResume（从简历文本识别技能）
        │   └── 20+ 技能模式匹配（Vue.js/Java/Spring Boot/MySQL...）
        ├── inferMissingSkills（推断常见缺失技能）
        │   └── 基于技能分类推断（前端缺TypeScript/后端缺微服务...）
        └── 计算匹配率和评分
```

#### 数据模型

```
AtsScoreResponse
├── overallScore        # 综合评分 (0-100)
├── grade               # 等级 (优秀/良好/一般/待改进/需重写)
├── gradeDescription    # 等级描述
├── jobMatchScore       # 职位匹配评分
│   ├── score           # 匹配分数
│   ├── level           # 匹配等级
│   ├── description     # 匹配描述
│   ├── totalRequired   # 总要求数
│   └── matchedRequired # 已匹配数
├── structureScore      # 结构评分
│   ├── score           # 结构分数
│   ├── hasContact      # 是否有联系方式
│   ├── hasSummary      # 是否有个人简介
│   ├── hasExperience   # 是否有工作经历
│   ├── hasEducation    # 是否有教育背景
│   ├── hasSkills       # 是否有技能清单
│   └── wordCount       # 总字数
├── contentScore        # 内容评分
│   ├── score           # 内容分数
│   ├── skillCount      # 技能数量
│   ├── keywordDensity  # 关键词密度
│   └── densityLevel    # 密度等级
├── keywordScore        # 关键词评分
│   ├── score           # 关键词分数
│   ├── totalKeywords   # 总关键词数
│   ├── matchedKeywords # 匹配关键词数
│   └── matchRate       # 匹配率
├── matchedSkills       # 已匹配技能列表
├── missingSkills       # 缺失技能列表
├── matchedCount        # 匹配数量
├── missingCount        # 缺失数量
├── categoryDetails     # 分类详情
│   └── [category]      # 分类名称 (frontend/backend/database/devops)
│       ├── name        # 分类中文名
│       ├── score       # 分类分数
│       ├── level       # 分类等级
│       ├── matched     # 匹配数量
│       ├── total       # 总数量
│       ├── matchedSkills # 已匹配技能
│       └── missingSkills # 缺失技能
├── skillGapDetails     # 技能差距详情
│   └── [skill]         # 技能名称
│       ├── category    # 所属分类
│       ├── importance  # 重要程度 (高/中/低)
│       ├── reason      # 缺失原因
│       ├── suggestion  # 学习建议
│       └── learningResources # 学习资源
├── optimizationSuggestions # 优化建议
│   └── [suggestion]    # 建议项
│       ├── type        # 类型 (structure/skill/content)
│       ├── title       # 标题
│       ├── description # 描述
│       ├── priority    # 优先级 (高/中/低)
│       └── impact      # 预期效果
├── responseTimeMs      # 响应时间
├── provider            # 评分提供商 (llm-enhanced/basic)
└── keyId               # 使用的 API Key ID
```

### 5. AI 智能体模块

#### 设计目标

- 统一的对话接口
- 多场景 Prompt 模板
- 思考过程分离（DeepSeek `<think/>` 标签）
- 智能请求路由（对话/生成/分析/搜索）

#### Prompt 模板设计

```
┌─────────────────────────────────────────────────────┐
│              AiAgentService                         │
│─────────────────────────────────────────────────────│
│ - SYSTEM_PROMPT: String        (系统提示词)         │
│ - RESUME_GENERATE_PROMPT       (简历生成模板)       │
│ - DEEP_ANALYSIS_PROMPT         (深度分析模板)       │
│ - SKILL_SEARCH_PROMPT          (技能搜索模板)       │
│─────────────────────────────────────────────────────│
│ + chat(request): AiChatResponse                    │
│ + generateResume(userInput, provider, keyId)       │
│ + analyzeWithAi(resumeText, jobDescription)        │
│ + searchWithAi(query, matchedSkills, jobSkills)    │
│ + parseThinkingContent(response): ParsedResponse   │
│ + cleanupMarkdown(text): String                    │
│ + resolveModel(provider, keyId): ChatModel         │
│ + buildConversationalChain(messages): String       │
└─────────────────────────────────────────────────────┘
```

#### 响应处理流程

```
AI 原始响应
    │
    ▼
┌─────────────────────────────────────────┐
│         parseThinkingContent()          │
│                                         │
│  1. 提取 <think...</think > 标签内容    │
│  2. 分离思考过程与最终回答              │
│  3. 清理 Markdown 格式问题             │
│     - 修复未闭合的代码块               │
│     - 修复表格格式                     │
│     - 清理多余空行                     │
│                                         │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│           ParsedResponse                │
│  - answer: String    (最终回答)         │
│  - thinking: String  (思考过程)         │
└─────────────────────────────────────────┘
```

#### 智能请求路由

```
用户消息
    │
    ├── 匹配"生成简历|写简历|帮我写" → generateResume API
    │
    └── 其他 → chat API
        ├── 自动注入简历上下文（前500字）
        └── RAG 检索增强
```

### 6. 简历分析模块

#### 设计目标

- 规则驱动的技能提取（不依赖 LLM）
- 多词技能识别和同义词归一化
- OR 类别匹配（如 Vue/React/Angular 满足任一即可）
- 四维度分类评分

#### 技能库

```java
private static final Set<String> SKILLS = Set.of(
    // 编程语言
    "java", "python", "javascript", "typescript", "c#", "c++", "go", "rust",
    // 前端框架
    "vue", "vue.js", "react", "angular", "jquery", "html", "css",
    // 后端框架
    "spring boot", "spring", "mybatis", "mybatis-plus", "asp.net core",
    // 数据库
    "mysql", "postgresql", "mongodb", "redis", "sql server",
    // DevOps
    "docker", "kubernetes", "jenkins", "git", "linux", "nginx"
    // ... 100+ 技能
);

private static final Map<String, String> NORM = Map.of(
    "csharp", "c#",
    "vue.js", "vue",
    "springboot", "spring boot",
    "mybatisplus", "mybatis-plus",
    "aspnetcore", "asp.net core"
    // ... 同义词归一化
);
```

## 数据流设计

### 简历分析流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 上传简历  │───▶│ 解析内容  │───▶│ 提取技能  │───▶│ 匹配岗位  │
│ (PDF/DOCX)│    │(FileParser)│   │(ResumeSvc)│   │(分类评分) │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
                                                    │
                    ┌───────────────────────────────┘
                    ▼
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 生成报告  │◀───│ RAG 检索  │◀───│ 权重排序  │◀───│ 计算差距  │
│(AtsScore)│    │(RagService)│   │(Weighted) │    │(SkillGap)│
└──────────┘    └──────────┘    └──────────┘    └──────────┘
     │
     ▼
┌──────────┐    ┌──────────┐
│ 前端展示  │───▶│ 模板对比  │
│(雷达图等)│    │(Template) │
└──────────┘    └──────────┘
```

### AI 对话流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│ 用户提问  │───▶│ 智能路由  │───▶│ RAG 检索  │
│          │    │(生成/对话)│    │(混合检索) │
└──────────┘    └──────────┘    └──────────┘
                                      │
                                      ▼
┌──────────┐    ┌──────────┐    ┌──────────┐
│ 返回响应  │◀───│ 解析响应  │◀───│ 构建 Prompt│
│(Markdown)│    │(思考分离)│    │(简历+检索)│
└──────────┘    └──────────┘    └──────────┘
```

### API Key 验证流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│ 输入 Key  │───▶│ 创建模型  │───▶│ 发送测试  │
│ + 提供商  │    │(ChatModel)│   │("Hi")    │
└──────────┘    └──────────┘    └──────────┘
                                      │
                          ┌───────────┴───────────┐
                          ▼                       ▼
                    ┌──────────┐            ┌──────────┐
                    │ 验证成功  │            │ 验证失败  │
                    │ 存储到内存│            │ 返回错误  │
                    │ 返回keyId│            │(友好提示) │
                    └──────────┘            └──────────┘
```

## 前端架构

### 组件关系

```
App.vue (根组件 - 状态管理)
├── ApiKeyInput (API Key 管理)
│   ├── 提供商选择 (4种)
│   ├── Key 验证
│   ├── 高级设置 (Base URL / 模型名)
│   └── 已保存 Key 列表
│
├── Tab: 上传简历
│   └── ResumeUploader
│       ├── 文件上传 (拖拽)
│       ├── 文本粘贴
│       └── 示例简历
│
├── Tab: 编辑内容
│   └── ResumeEditor
│       ├── 简历文本编辑
│       ├── 职位描述输入
│       └── 触发分析
│
├── Tab: 分析结果
│   ├── AnalysisResult
│   │   ├── 评分卡片 (综合/匹配度/技能数)
│   │   ├── 子Tab: 技能雷达图 (ECharts)
│   │   ├── 子Tab: 关键词详情
│   │   ├── 子Tab: 优化建议 (优先级卡片)
│   │   ├── 子Tab: 结构分析
│   │   └── PDF 导出
│   │
│   └── AiAnalysis (可展开)
│       ├── 模型选择
│       ├── AI 分析报告 (Markdown)
│       ├── 思考过程
│       └── RAG 引用
│
├── Tab: AI 助手
│   └── AiChat
│       ├── 对话消息列表
│       ├── Markdown 渲染
│       ├── 思考过程 (可展开)
│       ├── RAG 检索结果
│       ├── 快捷提问
│       └── 模型选择
│
└── TemplateComparison (模态弹窗)
    ├── 左侧: 原简历 (技能高亮)
    ├── 右侧: 优化简历
    └── 使用模板 / 复制
```

### Markdown 渲染架构

```
AI 响应文本
    │
    ▼
renderMarkdown(text)          # 入口函数
    │
    ├── parseBlocks(text)     # 第一阶段: 块级解析
    │   ├── 代码块 (```lang)
    │   ├── 标题 (# ~ ######)
    │   ├── 分隔线 (--- / *** / ___)
    │   ├── 表格 (| col | col |)
    │   ├── 引用 (> text)
    │   ├── 无序列表 (- / * / +)
    │   ├── 有序列表 (1. )
    │   └── 段落
    │
    └── renderBlock(block)    # 第二阶段: 块级渲染
        └── inline(text)      # 行内解析
            ├── 行内代码 (`code`)
            ├── 链接 ([text](url))
            ├── 图片 (![alt](src))
            ├── 加粗 (**text**)
            ├── 斜体 (*text*)
            ├── 删除线 (~~text~~)
            └── 换行 (\n → <br>)

安全特性:
├── escapeHtml() - XSS 防护
├── 代码块内容 HTML 转义
└── 链接/图片 URL 转义
```

## 技术选型理由

### 后端技术选型

| 技术 | 选型理由 |
|-----|---------|
| Spring Boot 4.0.5 | 成熟稳定，生态丰富，开发效率高，自动配置 |
| LangChain4j 1.0.1 | Java 生态最好的 LLM 集成框架，OpenAI 兼容协议统一接口 |
| 本地向量模型 (DJL) | 无需外部依赖，保护隐私，响应快速，离线可用 |
| 内存向量存储 | 简单易用，适合中小规模数据，无需额外部署 |
| Lombok | 减少样板代码，@Data/@Builder 提高开发效率 |

### 前端技术选型

| 技术 | 选型理由 |
|-----|---------|
| Vue 3 | Composition API + `<script setup>`，更好的逻辑复用和类型推导 |
| Vite 7.3.1 | 极速的开发服务器，原生 ESM 支持，优秀的构建性能 |
| ECharts 5.5.0 | 功能强大的可视化库，雷达图/柱状图开箱即用 |
| 自定义 Markdown 解析器 | 无第三方依赖，精确控制渲染，内置 XSS 防护 |

## 扩展性设计

### 新增 LLM 提供商

1. 在 `application.yaml` 添加配置项
2. 在 `LlmConfig` 添加 Bean 定义
3. 在 `selectPreconfiguredModel` 添加分支
4. 在 `ApiKeyService.normalizeProvider` 添加提供商归一化
5. 前端 `ApiKeyInput.vue` 添加提供商选项

### 新增检索维度

1. 在 `WeightConfig` 添加新字段
2. 在 `WeightedRetrievalService` 实现计算逻辑
3. 更新权重配置（归一化确保权重之和为 1）
4. 前端添加权重配置 UI

### 新增 API 接口

1. 在 `model` 包创建请求/响应模型（使用 @Data + @Builder）
2. 在 `service` 包实现业务逻辑
3. 在 `controller` 包创建控制器
4. 前端 `api.js` 添加 API 定义

## 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                       负载均衡器 (Nginx)                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ 后端实例1 │    │ 后端实例2 │    │ 后端实例3 │
    │(Spring   │    │(Spring   │    │(Spring   │
    │ Boot)    │    │ Boot)    │    │ Boot)    │
    └──────────┘    └──────────┘    └──────────┘
          │               │               │
          └───────────────┼───────────────┘
                          │
                          ▼
    ┌─────────────────────────────────────────────────────┐
    │                    外部 LLM API                      │
    │  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
    │  │ OpenAI   │ │DashScope │ │DeepSeek  │            │
    │  └──────────┘ └──────────┘ └──────────┘            │
    └─────────────────────────────────────────────────────┘
```

### 部署注意事项

1. **CORS 配置** - 生产环境需修改 `CorsConfig` 为实际域名
2. **API Key 管理** - 生产环境建议使用数据库持久化 + 加密存储
3. **向量存储** - 大规模数据建议迁移到 Milvus/Pinecone
4. **请求限流** - 生产环境需配置 RateLimiter
5. **HTTPS** - 生产环境必须启用 HTTPS
6. **环境变量** - 敏感配置通过环境变量注入

---

本架构设计确保了系统的可扩展性、可维护性和高性能。
