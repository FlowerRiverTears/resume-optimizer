# 前端开发规范

## 技术栈

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Vue.js | 3.5.30 | 前端框架 (Composition API) |
| Vite | 7.3.1 | 构建工具 |
| ECharts | 5.5.0 | 数据可视化图表 |
| vue-echarts | 7.0.0 | Vue ECharts 集成 |

## 目录结构

```
src/
├── components/          # Vue 组件
│   ├── AiChat.vue       # AI 聊天界面
│   ├── AiAnalysis.vue   # AI 深度分析
│   ├── AnalysisResult.vue    # 分析结果展示（ATS评分）
│   ├── ApiKeyInput.vue  # API Key 输入
│   ├── ResumeEditor.vue # 简历编辑器
│   ├── ResumeUploader.vue   # 文件上传
│   └── TemplateComparison.vue # 模板对比
├── api.js               # API 接口定义
├── markdown.js          # Markdown 解析器
├── main.js              # 应用入口
└── App.vue              # 根组件
```

## 组件规范

### 组件命名

- 使用 PascalCase 命名：`AiChat.vue`、`ResumeEditor.vue`
- 组件名应具有描述性，反映其功能
- 多词命名，避免与 HTML 元素冲突

### 组件结构

```vue
<script setup>
// 1. 导入
import { ref, computed, onMounted } from 'vue'
import { API, apiPost } from '../api.js'

// 2. Props 定义
const props = defineProps({
  keyId: String,
  resumeContent: String
})

// 3. Emits 定义
const emit = defineEmits(['close', 'analysis-done'])

// 4. 响应式状态
const isLoading = ref(false)
const result = ref(null)

// 5. 计算属性
const formattedResult = computed(() => {
  // ...
})

// 6. 方法
const handleSubmit = async () => {
  // ...
}

// 7. 生命周期
onMounted(() => {
  // ...
})
</script>

<template>
  <!-- 模板内容 -->
</template>

<style scoped>
/* 样式内容 */
</style>
```

### Props 规范

```javascript
const props = defineProps({
  // 基础类型检查
  title: String,
  
  // 多种类型
  value: [String, Number],
  
  // 必填字段
  required: {
    type: String,
    required: true
  },
  
  // 默认值
  size: {
    type: String,
    default: 'medium'
  },
  
  // 对象默认值
  options: {
    type: Object,
    default: () => ({})
  }
})
```

### Emits 规范

```javascript
const emit = defineEmits(['update', 'submit', 'close'])

// 使用 emit
emit('update', newValue)
emit('submit', { data: result })
emit('close')
```

## API 调用规范

### API 定义 (api.js)

```javascript
const API_BASE = '/api'

export const API = {
  parse: `${API_BASE}/parse`,
  analyze: `${API_BASE}/analyze`,
  
  ai: {
    chat: `${API_BASE}/ai/chat`,
    generateResume: `${API_BASE}/ai/generate-resume`,
    analyze: `${API_BASE}/ai/analyze`
  }
}

export async function apiPost(url, data) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  
  if (!response.ok) {
    throw new Error(await response.text())
  }
  
  return response.json()
}
```

### API 调用示例

```javascript
import { API, apiPost } from '../api.js'

const fetchData = async () => {
  isLoading.value = true
  error.value = ''
  
  try {
    const result = await apiPost(API.ai.chat, {
      message: inputMessage.value,
      provider: selectedProvider.value
    })
    
    response.value = result
  } catch (err) {
    error.value = '请求失败: ' + err.message
  } finally {
    isLoading.value = false
  }
}
```

## 样式规范

### CSS 组织

```css
/* 1. 组件容器 */
.component-name {
  /* 布局属性 */
  display: flex;
  flex-direction: column;
  
  /* 尺寸属性 */
  width: 100%;
  min-height: 200px;
  
  /* 外边距 */
  margin: 16px 0;
  
  /* 内边距 */
  padding: 20px;
  
  /* 边框 */
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  
  /* 背景 */
  background: #fff;
  
  /* 其他 */
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 2. 子元素 */
.component-name .header {
  /* ... */
}

/* 3. 状态类 */
.component-name.active {
  /* ... */
}

/* 4. 响应式 */
@media (max-width: 768px) {
  .component-name {
    /* ... */
  }
}
```

### 颜色变量

```css
/* 主色调 */
--primary: #2563eb;
--primary-dark: #1d4ed8;

/* 辅助色 */
--secondary: #7c3aed;

/* 状态色 */
--success: #10b981;
--warning: #f59e0b;
--error: #ef4444;

/* 评分颜色 */
--score-excellent: #10b981;  /* 90-100 优秀 */
--score-good: #22c55e;       /* 75-89 良好 */
--score-average: #f59e0b;    /* 60-74 一般 */
--score-poor: #ef4444;       /* 0-59 待改进 */

/* 灰度 */
--gray-50: #f9fafb;
--gray-100: #f3f4f6;
--gray-200: #e5e7eb;
--gray-300: #d1d5db;
--gray-400: #9ca3af;
--gray-500: #6b7280;
--gray-600: #4b5563;
--gray-700: #374151;
--gray-800: #1f2937;
--gray-900: #111827;
```

### Markdown 样式规范

```css
/* Markdown 内容容器 */
.message-content :deep(h2) {
  font-size: 16px;
  font-weight: 700;
  margin: 12px 0 6px;
  color: #1e293b;
  border-bottom: 1.5px solid #e2e8f0;
  padding-bottom: 4px;
}

/* 表格样式 */
.message-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.message-content :deep(th) {
  background: #f1f5f9;
  font-weight: 600;
  padding: 6px 10px;
  border-bottom: 2px solid #cbd5e1;
}

.message-content :deep(td) {
  padding: 5px 10px;
  border-bottom: 1px solid #f1f5f9;
}

/* 代码块样式 */
.message-content :deep(.code-block) {
  background: #1e293b;
  border-radius: 6px;
  overflow: hidden;
}

.message-content :deep(.code-block code) {
  color: #e2e8f0;
  font-family: 'SF Mono', 'Fira Code', monospace;
}
```

## Markdown 解析器

### 使用方式

```javascript
import { renderMarkdown } from '../api.js'

// 在模板中使用
<div v-html="renderMarkdown(content)"></div>
```

### 支持的语法

| 语法 | 示例 |
|-----|------|
| 标题 | `## 标题` `### 子标题` |
| 加粗 | `**加粗文本**` |
| 斜体 | `*斜体文本*` |
| 删除线 | `~~删除文本~~` |
| 行内代码 | `` `code` `` |
| 代码块 | `` ```javascript `` |
| 链接 | `[文本](url)` |
| 图片 | `![alt](src)` |
| 列表 | `- 项目` `1. 项目` |
| 任务列表 | `- [x] 完成` `- [ ] 未完成` |
| 引用 | `> 引用内容` |
| 表格 | `\| 列1 \| 列2 \|` |
| 分隔线 | `---` |

## ATS 评分组件

### 组件概述

`AnalysisResult.vue` 是 ATS 评分结果展示组件，用于展示简历分析报告。

### 功能模块

| 模块 | 描述 |
|-----|------|
| 评分卡片 | ATS 综合评分、职位匹配度、匹配/缺失技能数量 |
| 技能雷达图 | ECharts 雷达图展示各分类匹配度 |
| 关键词详情 | 已匹配技能、缺失技能、学习建议 |
| 优化建议 | 按优先级排序的优化建议卡片 |
| 结构分析 | 简历结构完整性检查、字数统计 |

### 数据获取

```javascript
const fetchAtsScore = async () => {
  const data = await apiPost(API.atsScore, {
    resumeText: props.resumeContent,
    jobDescription: ''
  })
  atsScoreData.value = data
}
```

### 评分颜色映射

```javascript
const getScoreColor = (score) => {
  if (score >= 80) return '#10b981'  // 绿色 - 优秀
  if (score >= 60) return '#f59e0b'  // 橙色 - 一般
  return '#ef4444'                    // 红色 - 待改进
}
```

### ECharts 图表

```javascript
// 雷达图配置
const option = {
  title: { text: '技能匹配雷达图' },
  radar: {
    indicator: categories.map(cat => ({ name: cat, max: 100 }))
  },
  series: [{
    type: 'radar',
    data: [{ value: values, name: '匹配度' }]
  }]
}
```

## 状态管理

### 本地状态

```javascript
// 使用 ref 管理简单状态
const count = ref(0)
const message = ref('')

// 使用 reactive 管理对象状态
const state = reactive({
  user: null,
  loading: false,
  error: null
})
```

### 组件间通信

```javascript
// 父组件
const handleUpdate = (data) => {
  console.log('Received:', data)
}

<ChildComponent @update="handleUpdate" />

// 子组件
const emit = defineEmits(['update'])
emit('update', { value: newValue })
```

## 错误处理

### 异步操作错误处理

```javascript
const fetchData = async () => {
  isLoading.value = true
  error.value = ''
  
  try {
    const result = await apiPost(API.endpoint, params)
    data.value = result
  } catch (err) {
    error.value = err.message || '操作失败'
    console.error('API Error:', err)
  } finally {
    isLoading.value = false
  }
}
```

### 用户友好错误提示

```javascript
// 错误消息映射
const ERROR_MESSAGES = {
  'NETWORK_ERROR': '网络连接失败，请检查网络',
  'INVALID_API_KEY': 'API Key 无效，请检查配置',
  'FILE_TOO_LARGE': '文件大小超过限制 (10MB)',
  'UNSUPPORTED_FORMAT': '不支持的文件格式'
}

const showError = (code) => {
  error.value = ERROR_MESSAGES[code] || '未知错误'
}
```

## 性能优化

### 懒加载组件

```javascript
import { defineAsyncComponent } from 'vue'

const AiAnalysis = defineAsyncComponent(() => 
  import('./components/AiAnalysis.vue')
)
```

### 列表渲染优化

```vue
<template>
  <div v-for="item in list" :key="item.id">
    {{ item.name }}
  </div>
</template>
```

### 防抖与节流

```javascript
import { debounce } from 'lodash-es'

const handleSearch = debounce((query) => {
  // 搜索逻辑
}, 300)
```

## 构建与部署

### 开发模式

```bash
npm run dev
```

### 生产构建

```bash
npm run build
```

### 预览构建结果

```bash
npm run preview
```

## 浏览器兼容性

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

---

遵循以上规范可以确保代码质量、可维护性和团队协作效率。
