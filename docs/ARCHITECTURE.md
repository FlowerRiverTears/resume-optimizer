# 系统架构设计

## 架构概述

简历优化器采用**前后端分离**架构，后端基于 Spring Boot 提供 RESTful API，前端基于 Vue.js 构建单页应用。

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层 (Vue.js)                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │AiChat    │ │AiAnalysis│ │ResumeEdit│ │FileUpload│           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
│       │            │            │            │                   │
│       └────────────┴────────────┴────────────┘                   │
│                         │                                        │
│                    api.js (API 封装)                             │
└─────────────────────────┬───────────────────────────────────────┘
                          │ HTTP/REST
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后端层 (Spring Boot)                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Controller 层                          │  │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐           │  │
│  │  │ResumeCtrl  │ │AiAgentCtrl │ │ApiKeyCtrl  │           │  │
│  │  └─────┬──────┘ └─────┬──────┘ └─────┬──────┘           │  │
│  └────────┼──────────────┼──────────────┼───────────────────┘  │
│           │              │              │                       │
│  ┌────────▼──────────────▼──────────────▼───────────────────┐  │
│  │                    Service 层                             │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │  │
│  │  │ResumeSvc │ │AiAgentSvc│ │RagService│ │ApiKeySvc │    │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │  │
│  │  ┌────────────────┐ ┌──────────────────────────────┐   │  │
│  │  │AtsScoreService│ │WeightedRetrievalService      │   │  │
│  │  └────────────────┘ └──────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────┘  │
│           │              │                                       │
│  ┌────────▼──────────────▼──────────────────────────────────┐  │
│  │                    配置层 (Config)                         │  │
│  │  ┌──────────┐ ┌──────────────┐ ┌────────────────┐       │  │
│  │  │LlmConfig │ │VectorStoreCfg│ │CorsConfig      │       │  │
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
│  │         本地向量模型 (all-MiniLM-L6-v2)           │          │
│  └──────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## 核心模块设计

### 1. LLM 集成模块

#### 设计目标

- 支持多个 LLM 提供商
- 统一的 API 接口
- 灵活的配置管理

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
│ + chatModel(): ChatModel                           │
│ + openaiChatModel(): ChatModel                     │
│ + dashscopeChatModel(): ChatModel                  │
│ + deepseekChatModel(): ChatModel                   │
│ + minimaxChatModel(): ChatModel                    │
└─────────────────────────────────────────────────────┘
                         │
                         │ creates
                         ▼
┌─────────────────────────────────────────────────────┐
│              OpenAiChatModel (LangChain4j)          │
│─────────────────────────────────────────────────────│
│ - apiKey: String                                    │
│ - baseUrl: String                                   │
│ - modelName: String                                 │
│ - temperature: double                               │
│ - maxTokens: int                                    │
└─────────────────────────────────────────────────────┘
```

#### 配置示例

```yaml
ai:
  llm:
    provider: openai  # 当前激活的提供商
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 2048
```

### 2. RAG 检索增强模块

#### 设计目标

- 混合检索：向量检索 + 关键词检索
- 语义分块：智能文档切分
- 权重排序：多维度结果优化

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
│  └────────┬────────┘    └────────┬────────┘         │
│           │                      │                   │
│           └──────────┬───────────┘                   │
│                      ▼                               │
│           ┌─────────────────────┐                   │
│           │     结果合并         │                   │
│           └──────────┬──────────┘                   │
│                      ▼                               │
│           ┌─────────────────────┐                   │
│           │     重排序          │                   │
│           │  (Reranking)        │                   │
│           └──────────┬──────────┘                   │
│                      │                               │
└──────────────────────┼───────────────────────────────┘
                       ▼
              检索结果列表
```

#### 核心方法

```java
public List<SearchResult> hybridSearch(String query, int maxResults, double minScore) {
    // 1. 向量检索
    List<SearchResult> vectorResults = vectorSearch(query, maxResults * 2, minScore);
    
    // 2. 关键词检索
    List<SearchResult> keywordResults = keywordSearch(query, maxResults * 2);
    
    // 3. 结果合并
    Map<String, SearchResult> merged = mergeResults(vectorResults, keywordResults);
    
    // 4. 重排序
    List<SearchResult> reranked = rerank(new ArrayList<>(merged.values()), query);
    
    return reranked.stream().limit(maxResults).collect(Collectors.toList());
}
```

### 3. 权重检索模块

#### 设计目标

- 多维度权重配置
- 动态权重调整
- 可扩展的评分维度

#### 权重维度

| 维度 | 权重 | 描述 |
|-----|------|-----|
| skill-match | 0.35 | 技能匹配度 |
| semantic-similarity | 0.30 | 语义相似度 |
| category-relevance | 0.20 | 类别相关性 |
| experience-level | 0.15 | 经验级别 |

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

- 综合评分计算
- 多维度分析（结构、内容、关键词）
- RAG 深度检索增强
- 智能优化建议

#### 评分维度

| 维度 | 权重 | 描述 |
|-----|------|-----|
| ATS 评分 | 40% | 简历格式与 ATS 兼容性 |
| 匹配度 | 40% | 技能与岗位匹配程度 |
| 结构评分 | 20% | 简历结构完整性 |

#### 评分等级

| 分数范围 | 等级 | 描述 |
|---------|------|-----|
| 90-100 | 优秀 | 简历质量优秀，与岗位高度匹配 |
| 75-89 | 良好 | 简历质量良好，基本匹配岗位要求 |
| 60-74 | 一般 | 简历质量一般，部分技能需要补充 |
| 40-59 | 待改进 | 简历需要较多改进 |
| 0-39 | 需重写 | 简历与岗位要求差距较大 |

#### 数据模型

```
AtsScoreResponse
├── overallScore        # 综合评分 (0-100)
├── grade               # 等级 (优秀/良好/一般/待改进/需重写)
├── gradeDescription    # 等级描述
├── jobMatchScore       # 职位匹配评分
│   ├── score           # 匹配分数
│   ├── level           # 匹配等级
│   └── description     # 匹配描述
├── structureScore      # 结构评分
│   ├── score           # 结构分数
│   ├── hasContact      # 是否有联系方式
│   ├── hasSummary      # 是否有个人简介
│   ├── hasExperience   # 是否有工作经历
│   ├── hasEducation    # 是否有教育背景
│   └── wordCount       # 总字数
├── contentScore        # 内容评分
│   ├── score           # 内容分数
│   ├── skillCount      # 技能数量
│   └── keywordDensity  # 关键词密度
├── keywordScore        # 关键词评分
│   ├── score           # 关键词分数
│   ├── totalKeywords   # 总关键词数
│   └── matchedKeywords # 匹配关键词数
├── categoryDetails     # 分类详情
│   └── [category]      # 分类名称
│       ├── name        # 分类中文名
│       ├── score       # 分类分数
│       ├── matched     # 匹配数量
│       └── total       # 总数量
├── skillGapDetails     # 技能差距详情
│   └── [skill]         # 技能名称
│       ├── category    # 所属分类
│       ├── importance  # 重要程度
│       ├── suggestion  # 学习建议
│       └── learningResources # 学习资源
└── optimizationSuggestions # 优化建议
    └── [suggestion]    # 建议项
        ├── type        # 类型
        ├── title       # 标题
        ├── description # 描述
        ├── priority    # 优先级
        └── impact      # 预期效果
```

#### API 接口

```
POST /api/ats-score
Content-Type: application/json

请求参数:
{
  "resumeText": "简历内容",
  "jobDescription": "职位描述（可选）"
}

响应结果:
{
  "overallScore": 85,
  "grade": "优秀",
  "gradeDescription": "简历质量优秀...",
  "jobMatchScore": { ... },
  "structureScore": { ... },
  "contentScore": { ... },
  "keywordScore": { ... },
  "categoryDetails": { ... },
  "skillGapDetails": [ ... ],
  "optimizationSuggestions": [ ... ],
  "responseTimeMs": 1234
}
```

### 5. AI 智能体模块

#### 设计目标

- 统一的对话接口
- 多场景 Prompt 模板
- 思考过程分离

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
│  3. 清理 Markdown 格式                  │
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

## 数据流设计

### 简历分析流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 上传简历  │───▶│ 解析内容  │───▶│ 提取技能  │───▶│ 匹配岗位  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
                                                    │
                    ┌───────────────────────────────┘
                    ▼
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 生成报告  │◀───│ RAG 检索  │◀───│ 权重排序  │◀───│ 计算差距  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

### AI 对话流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│ 用户提问  │───▶│ RAG 检索  │───▶│ 构建 Prompt│
└──────────┘    └──────────┘    └──────────┘
                                     │
                                     ▼
┌──────────┐    ┌──────────┐    ┌──────────┐
│ 返回响应  │◀───│ 解析响应  │◀───│ 调用 LLM  │
└──────────┘    └──────────┘    └──────────┘
```

## 技术选型理由

### 后端技术选型

| 技术 | 选型理由 |
|-----|---------|
| Spring Boot | 成熟稳定，生态丰富，开发效率高 |
| LangChain4j | Java 生态最好的 LLM 集成框架 |
| 本地向量模型 | 无需外部依赖，保护隐私，响应快速 |
| 内存向量存储 | 简单易用，适合中小规模数据 |

### 前端技术选型

| 技术 | 选型理由 |
|-----|---------|
| Vue 3 | Composition API，更好的 TypeScript 支持 |
| Vite | 快速的开发服务器，优秀的构建性能 |
| ECharts | 功能强大的可视化库 |

## 扩展性设计

### 新增 LLM 提供商

1. 在 `application.yaml` 添加配置
2. 在 `LlmConfig` 添加 Bean
3. 在 `selectPreconfiguredModel` 添加分支

### 新增检索维度

1. 在 `WeightConfig` 添加新字段
2. 在 `WeightedRetrievalService` 实现计算逻辑
3. 更新权重配置

### 新增 API 接口

1. 在 `model` 包创建请求/响应模型
2. 在 `service` 包实现业务逻辑
3. 在 `controller` 包创建控制器

## 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                       负载均衡器                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ 后端实例1 │    │ 后端实例2 │    │ 后端实例3 │
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

---

本架构设计确保了系统的可扩展性、可维护性和高性能。
