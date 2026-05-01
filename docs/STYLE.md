# 代码风格指南

## 概述

本文档定义了简历优化器项目的代码风格规范，确保代码一致性、可读性和可维护性。

## Java 代码规范

### 命名规范

#### 类命名

- 使用 **PascalCase**（大驼峰命名法）
- 类名应为名词，描述其职责

```java
// ✅ 正确
public class ResumeAnalysisService { }
public class AiAgentController { }
public class WeightedRetrievalService { }

// ❌ 错误
public class resumeAnalysis { }
public class AIAGENT_CONTROLLER { }
```

#### 方法命名

- 使用 **camelCase**（小驼峰命名法）
- 方法名应为动词或动词短语

```java
// ✅ 正确
public List<SearchResult> searchDocuments(String query) { }
public void ingestDocument(String content, String source) { }
private String buildContext(List<SearchResult> results) { }

// ❌ 错误
public List<SearchResult> SearchDocuments(String query) { }
public void document_ingest(String content) { }
```

#### 变量命名

- 使用 **camelCase**
- 变量名应具有描述性

```java
// ✅ 正确
String resumeContent = "...";
List<SearchResult> searchResults = new ArrayList<>();
int maxResults = 5;

// ❌ 错误
String ResumeContent = "...";
List<SearchResult> sr = new ArrayList<>();
int MAXRESULTS = 5;
```

#### 常量命名

- 使用 **UPPER_SNAKE_CASE**

```java
// ✅ 正确
public static final String DEFAULT_PROVIDER = "openai";
private static final int MAX_RETRY_COUNT = 3;
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

// ❌ 错误
public static final String defaultProvider = "openai";
private static final int maxRetryCount = 3;
```

### 代码组织

#### 类结构顺序

```java
package com.flowerrivertears.resumeoptimizer.service;

// 1. 导入语句
import com.flowerrivertears.resumeoptimizer.model.*;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;
import java.util.*;

// 2. 类注释
/**
 * AI 智能体服务
 * 提供 LLM 对话、简历生成、深度分析等功能
 */
@Service
public class AiAgentService {

    // 3. 静态常量
    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);
    private static final String SYSTEM_PROMPT = "...";
    
    // 4. 依赖注入
    @Autowired
    private ChatModel chatModel;
    
    @Autowired
    private RagService ragService;
    
    // 5. 实例变量
    
    // 6. 构造方法
    
    // 7. 公共方法
    public AiChatResponse chat(AiChatRequest request) {
        // ...
    }
    
    // 8. 私有方法
    private String buildContext(List<SearchResult> results) {
        // ...
    }
    
    // 9. 内部类
    private static class ParsedResponse {
        // ...
    }
}
```

#### 方法长度

- 单个方法不超过 **50 行**
- 超过时应拆分为多个私有方法

```java
// ✅ 正确 - 拆分为多个方法
public List<SearchResult> hybridSearch(String query, int maxResults) {
    List<SearchResult> vectorResults = vectorSearch(query, maxResults);
    List<SearchResult> keywordResults = keywordSearch(query, maxResults);
    List<SearchResult> merged = mergeResults(vectorResults, keywordResults);
    return rerank(merged, query);
}

private List<SearchResult> vectorSearch(String query, int maxResults) {
    // ...
}

private List<SearchResult> keywordSearch(String query, int maxResults) {
    // ...
}
```

### 注释规范

#### 类注释

```java
/**
 * RAG 检索服务
 * 
 * 提供文档索引、向量检索、关键词检索、混合检索等功能。
 * 支持语义分块和权重排序。
 * 
 * @author FlowerRiverTears
 * @since 1.0.0
 */
@Service
public class RagService {
    // ...
}
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
public List<SearchResult> hybridSearch(String query, int maxResults, double minScore) {
    // ...
}
```

#### 行内注释

```java
// 计算 TF-IDF 分数
double idf = Math.log((double) keywordIndex.size() / (docCount + 1));
double tfidfScore = tf * idf;

// 注意：此处使用 0.6 作为向量权重，可根据实际效果调整
double vectorComponent = r.getVectorScore() * 0.6;
```

### 异常处理

#### 检查参数

```java
public void ingestDocument(String content, String source, String category) {
    // 参数校验
    if (content == null || content.isBlank()) {
        throw new IllegalArgumentException("文档内容不能为空");
    }
    
    // 业务逻辑
    // ...
}
```

#### 日志记录

```java
public AiChatResponse chat(AiChatRequest request) {
    long startTime = System.currentTimeMillis();
    log.info("AI Chat request: message='{}', provider={}", 
             request.getMessage(), request.getProvider());
    
    try {
        // 业务逻辑
        AiChatResponse response = doChat(request);
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("AI Chat completed in {}ms", elapsed);
        
        return response;
    } catch (Exception e) {
        log.error("AI Chat failed: {}", e.getMessage(), e);
        throw new RuntimeException("AI 对话失败", e);
    }
}
```

### Lombok 使用

```java
// 数据类使用 @Data
@Data
public class AiChatRequest {
    private String message;
    private String provider;
    private String keyId;
}

// 不可变类使用 @Builder
@Data
@Builder
public class AiChatResponse {
    private String answer;
    private String thinking;
    private String provider;
    private String model;
    private List<SearchResult> searchResults;
    private long responseTimeMs;
}
```

## JavaScript/Vue 代码规范

### 命名规范

```javascript
// 变量 - camelCase
const resumeContent = ref('')
const isLoading = ref(false)

// 函数 - camelCase
const handleSubmit = async () => { }
const formatTime = (ms) => { }

// 组件 - PascalCase
import AiChat from './components/AiChat.vue'
import ResumeEditor from './components/ResumeEditor.vue'

// 常量 - UPPER_SNAKE_CASE
const API_BASE = '/api'
const MAX_FILE_SIZE = 10 * 1024 * 1024
```

### 组件结构

```vue
<script setup>
// 1. 导入
import { ref, computed, onMounted } from 'vue'
import { API, apiPost } from '../api.js'

// 2. Props
const props = defineProps({
  keyId: String,
  resumeContent: String
})

// 3. Emits
const emit = defineEmits(['close', 'submit'])

// 4. 响应式状态
const isLoading = ref(false)
const result = ref(null)
const error = ref('')

// 5. 计算属性
const formattedResult = computed(() => {
  if (!result.value) return ''
  return JSON.stringify(result.value, null, 2)
})

// 6. 方法
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

// 7. 生命周期
onMounted(() => {
  fetchData()
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
// ✅ 正确 - 使用 async/await 和 try-catch
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

// ❌ 错误 - 缺少错误处理
const handleSubmit = async () => {
  const result = await apiPost(API.ai.chat, { message: inputMessage.value })
  response.value = result
}
```

### CSS 规范

```css
/* 选择器命名 - 使用短横线分隔 */
.ai-chat { }
.message-content { }
.code-block { }

/* 属性顺序 */
.component {
  /* 1. 布局 */
  display: flex;
  flex-direction: column;
  
  /* 2. 尺寸 */
  width: 100%;
  height: 100%;
  
  /* 3. 外边距 */
  margin: 16px 0;
  
  /* 4. 内边距 */
  padding: 20px;
  
  /* 5. 边框 */
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  
  /* 6. 背景 */
  background: #fff;
  
  /* 7. 其他 */
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
```

## 配置文件规范

### YAML 格式

```yaml
# 使用 2 空格缩进
ai:
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
      
# 使用注释说明配置项
ai:
  weight:
    enabled: true  # 是否启用权重排序
    dimensions:
      skill-match: 0.35  # 技能匹配权重
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

### Vue 代码

- [ ] 组件职责单一
- [ ] Props 有类型定义
- [ ] 异步操作有错误处理
- [ ] 响应式数据使用正确
- [ ] 样式使用 scoped

### 通用

- [ ] 代码无冗余
- [ ] 无明显的性能问题
- [ ] 遵循项目目录结构

---

遵循以上规范可以确保代码质量和团队协作效率。
