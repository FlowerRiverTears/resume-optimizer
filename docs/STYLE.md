# 代码风格指南

## 概述

本文档定义了简历优化器项目的代码风格规范，确保代码一致性、可读性和可维护性。

## Java 代码规范

### 命名规范

#### 类命名

- 使用 **PascalCase**（大驼峰命名法）
- 类名应为名词，描述其职责

```java
public class ResumeAnalysisService { }
public class AiAgentController { }
public class WeightedRetrievalService { }
public class AtsScoreResponse { }
```

#### 方法命名

- 使用 **camelCase**（小驼峰命名法）
- 方法名应为动词或动词短语

```java
public List<SearchResult> searchDocuments(String query) { }
public void ingestDocument(String content, String source) { }
private String buildContext(List<SearchResult> results) { }
private List<String> inferMatchedSkillsFromResume(AnalysisResponse basic, String resumeText) { }
```

#### 变量命名

- 使用 **camelCase**
- 变量名应具有描述性

```java
String resumeContent = "...";
List<SearchResult> searchResults = new ArrayList<>();
int maxResults = 5;
double weightedScore = 0.85;
```

#### 常量命名

- 使用 **UPPER_SNAKE_CASE**

```java
public static final String DEFAULT_PROVIDER = "openai";
private static final int MAX_RETRY_COUNT = 3;
private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
```

### 代码组织

#### 类结构顺序

```java
package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.model.*;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);
    private static final String SYSTEM_PROMPT = "...";
    
    @Autowired
    private ChatModel chatModel;
    
    @Autowired
    private RagService ragService;
    
    public AiChatResponse chat(AiChatRequest request) {
        // ...
    }
    
    public AiChatResponse generateResume(String userInput, String provider, String keyId) {
        // ...
    }
    
    private String buildContext(List<SearchResult> results) {
        // ...
    }
    
    private ParsedResponse parseThinkingContent(String response) {
        // ...
    }
    
    private static class ParsedResponse {
        String answer;
        String thinking;
    }
}
```

#### 方法长度

- 单个方法不超过 **50 行**
- 超过时应拆分为多个私有方法

```java
public List<SearchResult> hybridSearch(String query, int maxResults) {
    List<SearchResult> vectorResults = vectorSearch(query, maxResults);
    List<SearchResult> keywordResults = keywordSearch(query, maxResults);
    List<SearchResult> merged = mergeResults(vectorResults, keywordResults);
    return rerank(merged, query);
}

private List<SearchResult> vectorSearch(String query, int maxResults) { }
private List<SearchResult> keywordSearch(String query, int maxResults) { }
private List<SearchResult> mergeResults(List<SearchResult> v, List<SearchResult> k) { }
private List<SearchResult> rerank(List<SearchResult> results, String query) { }
```

### 注释规范

#### 类注释

```java
/**
 * RAG 检索服务
 * 
 * 提供文档索引、向量检索、关键词检索、混合检索等功能。
 * 支持语义分块和权重排序。
 */
@Service
public class RagService { }
```

#### 方法注释

```java
/**
 * 执行混合检索
 * 
 * 结合向量检索和关键词检索，对结果进行合并和重排序。
 * 
 * @param query 查询文本
 * @param maxResults 最大返回结果数
 * @param minScore 最小相似度阈值
 * @return 排序后的检索结果列表
 */
public List<SearchResult> hybridSearch(String query, int maxResults, double minScore) { }
```

#### 行内注释

```java
double idf = Math.log((double) keywordIndex.size() / (docCount + 1));
double tfidfScore = tf * idf;

double vectorComponent = r.getVectorScore() * 0.6;
double keywordComponent = r.getKeywordScore() * 0.25;
double relevanceComponent = calculateRelevance(r, query) * 0.15;
```

### 异常处理

#### 检查参数

```java
public void ingestDocument(String content, String source, String category) {
    if (content == null || content.isBlank()) {
        throw new IllegalArgumentException("文档内容不能为空");
    }
    // ...
}
```

#### 日志记录

```java
public AiChatResponse chat(AiChatRequest request) {
    long startTime = System.currentTimeMillis();
    log.info("AI Chat request: provider={}", request.getProvider());
    
    try {
        AiChatResponse response = doChat(request);
        log.info("AI Chat completed in {}ms", System.currentTimeMillis() - startTime);
        return response;
    } catch (Exception e) {
        log.error("AI Chat failed: {}", e.getMessage(), e);
        throw new RuntimeException("AI 对话失败", e);
    }
}
```

### Lombok 使用

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    private String message;
    private String provider;
    private String keyId;
    private String context;
}

@Data
@Builder
public class AtsScoreResponse {
    private int overallScore;
    private String grade;
    private String gradeDescription;
    private JobMatchScore jobMatchScore;
    private StructureScore structureScore;
    private ContentScore contentScore;
    private List<SkillGap> skillGapDetails;
    private List<OptimizationSuggestion> optimizationSuggestions;
}
```

### LangChain4j API 使用

```java
// 使用 EmbeddingSearchRequest 替代已废弃的 findRelevant
EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
    .queryEmbedding(queryEmbedding)
    .maxResults(maxResults)
    .minScore(minScore)
    .build();

EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
```

## JavaScript/Vue 代码规范

### 命名规范

```javascript
const resumeContent = ref('')
const isLoading = ref(false)

const handleSubmit = async () => { }
const formatTime = (ms) => { }

import AiChat from './components/AiChat.vue'
import ResumeEditor from './components/ResumeEditor.vue'

const API_BASE = '/api'
const MAX_FILE_SIZE = 10 * 1024 * 1024
```

### 组件结构

```vue
<script setup>
import { ref, computed, onMounted, nextTick, watch, onBeforeUnmount } from 'vue'
import { API, apiPost, apiGet, renderMarkdown } from '../api.js'

const props = defineProps({
  keyId: String,
  resumeContent: String
})

const emit = defineEmits(['close', 'submit'])

const isLoading = ref(false)
const result = ref(null)
const error = ref('')

const formattedResult = computed(() => {
  if (!result.value) return ''
  return JSON.stringify(result.value, null, 2)
})

const fetchData = async () => {
  isLoading.value = true
  try {
    result.value = await apiPost(API.endpoint, params)
  } catch (err) {
    error.value = err.message
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchData()
})

onBeforeUnmount(() => {
  // 清理 ECharts 实例等
})
</script>

<template>
  <!-- 模板内容 -->
</template>

<style scoped>
/* 样式内容 */
</style>
```

### 异步处理

```javascript
const handleSubmit = async () => {
  isLoading.value = true
  error.value = ''
  
  try {
    const result = await apiPost(API.ai.chat, {
      message: inputMessage.value
    })
    response.value = result
  } catch (err) {
    error.value = '请求失败: ' + err.message
  } finally {
    isLoading.value = false
  }
}
```

### ECharts 使用规范

```javascript
let radarChart = null

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  radarChart.setOption(option)
}

onBeforeUnmount(() => {
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
})
```

### CSS 规范

```css
.ai-chat { }
.message-content { }
.code-block { }
.result-item { }
.btn-send { }

.component {
  display: flex;
  flex-direction: column;
  width: 100%;
  margin: 16px 0;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
```

### Markdown 解析器规范

```javascript
// 使用 renderMarkdown 渲染 AI 回复
<div v-html="renderMarkdown(msg.content)"></div>

// 不直接使用 v-html 渲染用户输入
<div>{{ userInput }}</div>

// 自定义 Markdown 解析器不依赖第三方库
// 所有文本经过 escapeHtml 转义，防止 XSS
```

## 配置文件规范

### YAML 格式

```yaml
ai:
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
      
ai:
  weight:
    enabled: true
    dimensions:
      skill-match: 0.35
```

### Vite 配置

```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true
      }
    }
  }
})
```

## Git 提交规范

### 提交信息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| Type | 描述 |
|------|-----|
| feat | 新功能 |
| fix | Bug 修复 |
| docs | 文档更新 |
| style | 代码格式调整 |
| refactor | 重构 |
| test | 测试相关 |
| chore | 构建/工具相关 |

### 示例

```
feat(ai): 添加 DeepSeek 模型支持

- 新增 DeepSeek 配置项
- 实现 DeepSeek ChatModel Bean
- 更新模型选择逻辑

Closes #123
```

## 代码审查清单

### Java 代码

- [ ] 类和方法有适当的注释
- [ ] 变量名具有描述性
- [ ] 参数进行了有效性检查
- [ ] 异常被正确处理和记录
- [ ] 没有硬编码的敏感信息
- [ ] 日志级别使用正确
- [ ] Lombok 注解使用正确（@Data + @Builder + @NoArgsConstructor）
- [ ] LangChain4j API 使用最新版本（EmbeddingSearchRequest 替代 findRelevant）

### Vue 代码

- [ ] 组件职责单一
- [ ] Props 有类型定义
- [ ] 异步操作有错误处理（try-catch-finally）
- [ ] 响应式数据使用正确（ref/reactive/computed）
- [ ] 样式使用 scoped
- [ ] ECharts 实例正确销毁（dispose）
- [ ] Markdown 使用 renderMarkdown 渲染，不直接 v-html

### 通用

- [ ] 代码无冗余
- [ ] 无明显的性能问题
- [ ] 遵循项目目录结构
- [ ] API Key 不出现在日志中

---

遵循以上规范可以确保代码质量和团队协作效率。
