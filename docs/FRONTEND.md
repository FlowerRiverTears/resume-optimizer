# 前端开发规范

## 技术栈

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Vue.js | 3.5.30 | 前端框架（Composition API + `<script setup>`） |
| Vite | 7.3.1 | 构建工具 |
| ECharts | 5.5.0 | 数据可视化图表（雷达图/柱状图） |
| vue-echarts | 7.0.0 | Vue ECharts 集成 |
| 自定义 Markdown 解析器 | - | AI 回复渲染（无第三方依赖，XSS 防护） |

## 目录结构

```
src/
├── components/          # Vue 组件
│   ├── AiChat.vue       # AI 聊天界面（Markdown 渲染/思考过程/RAG 结果/快捷提问）
│   ├── AiAnalysis.vue   # AI 深度分析（模型选择/分析报告/RAG 引用）
│   ├── AnalysisResult.vue    # 分析结果展示（ATS 评分/雷达图/柱状图/关键词/建议/结构/PDF 导出）
│   ├── ApiKeyInput.vue  # API Key 输入（4 提供商/高级设置/折叠面板）
│   ├── ResumeEditor.vue # 简历编辑器（文本编辑/职位描述/触发分析）
│   ├── ResumeUploader.vue   # 文件上传（拖拽/粘贴/示例简历）
│   └── TemplateComparison.vue # 模板对比（左右对比/技能高亮/使用模板）
├── api.js               # API 接口定义（apiPost/apiGet/apiDelete/apiUpload + renderMarkdown re-export）
├── markdown.js          # Markdown 解析器（自定义实现，两阶段解析，XSS 防护）
├── main.js              # 应用入口
└── App.vue              # 根组件（状态管理/Tab 导航/组件编排）
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
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { API, apiPost, apiGet, renderMarkdown } from '../api.js'

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
  title: String,
  value: [String, Number],
  required: {
    type: String,
    required: true
  },
  size: {
    type: String,
    default: 'medium'
  },
  options: {
    type: Object,
    default: () => ({})
  }
})
```

### Emits 规范

```javascript
const emit = defineEmits(['update', 'submit', 'close'])

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
  optimize: `${API_BASE}/optimize`,
  atsScore: `${API_BASE}/ats-score`,
  
  ai: {
    chat: `${API_BASE}/ai/chat`,
    generateResume: `${API_BASE}/ai/generate-resume`,
    analyze: `${API_BASE}/ai/analyze`,
    search: `${API_BASE}/ai/search`,
    health: `${API_BASE}/ai/health`,
    rag: {
      ingest: `${API_BASE}/ai/rag/ingest`,
      ingestBatch: `${API_BASE}/ai/rag/ingest-batch`,
      search: `${API_BASE}/ai/rag/search`,
      clear: `${API_BASE}/ai/rag/clear`
    },
    weight: {
      config: `${API_BASE}/ai/weight/config`,
      configs: `${API_BASE}/ai/weight/configs`
    },
    keys: {
      validate: `${API_BASE}/ai/keys/validate`,
      list: `${API_BASE}/ai/keys/list`,
      providers: `${API_BASE}/ai/keys/providers`
    }
  }
}

export async function apiPost(url, data) { /* ... */ }
export async function apiGet(url) { /* ... */ }
export async function apiDelete(url) { /* ... */ }
export async function apiUpload(url, formData) { /* ... */ }
export { renderMarkdown } from './markdown.js'
```

### API 调用示例

```javascript
import { API, apiPost, apiGet } from '../api.js'

const fetchData = async () => {
  isLoading.value = true
  error.value = ''
  
  try {
    const result = await apiPost(API.ai.chat, {
      message: inputMessage.value,
      provider: selectedProvider.value,
      keyId: selectedKeyId.value || null
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
.component-name {
  /* 1. 布局属性 */
  display: flex;
  flex-direction: column;
  
  /* 2. 尺寸属性 */
  width: 100%;
  min-height: 200px;
  
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

AiChat.vue 和 AiAnalysis.vue 都使用 `:deep()` 选择器定义 Markdown 样式。两者样式类似但主色调不同：

- **AiChat.vue**: 主色调 `#2563eb`（蓝色）
- **AiAnalysis.vue**: 主色调 `#7c3aed`（紫色）

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
.message-content :deep(.table-wrapper) {
  overflow-x: auto;
  margin: 8px 0;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

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
  position: relative;
  margin: 8px 0;
  border-radius: 6px;
  background: #1e293b;
  overflow: hidden;
}

.message-content :deep(.code-block .code-lang) {
  position: absolute;
  top: 0;
  right: 0;
  padding: 2px 8px;
  background: #334155;
  color: #94a3b8;
  font-size: 10px;
  border-bottom-left-radius: 4px;
}

.message-content :deep(.code-block code) {
  color: #e2e8f0;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}

/* 行内代码 */
.message-content :deep(code) {
  background: #f1f5f9;
  color: #e11d48;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
}

/* 引用 */
.message-content :deep(blockquote) {
  border-left: 3px solid #3b82f6;
  padding: 4px 12px;
  margin: 8px 0;
  background: #eff6ff;
  border-radius: 0 4px 4px 0;
  color: #475569;
  font-size: 13px;
}
```

## Markdown 解析器

### 架构设计

自定义 Markdown 解析器采用**两阶段解析**架构：

1. **块级解析** (`parseBlocks`): 将文本分割为块级元素
2. **行内解析** (`inline`): 处理块内的行内格式

### 使用方式

```javascript
import { renderMarkdown } from '../api.js'

// 在模板中使用
<div v-html="renderMarkdown(content)"></div>
```

### 支持的语法

| 语法 | 示例 | 输出 |
|-----|------|-----|
| 标题 | `## 标题` `### 子标题` | `<h2>` ~ `<h6>` |
| 加粗 | `**加粗文本**` | `<strong>` |
| 斜体 | `*斜体文本*` | `<em>` |
| 删除线 | `~~删除文本~~` | `<del>` |
| 行内代码 | `` `code` `` | `<code>` |
| 代码块 | `` ```javascript `` | `.code-block > pre > code` |
| 链接 | `[文本](url)` | `<a target="_blank">` |
| 图片 | `![alt](src)` | `<img>` |
| 无序列表 | `- 项目` | `<ul><li>` |
| 有序列表 | `1. 项目` | `<ol><li>` |
| 任务列表 | `- [x] 完成` `- [ ] 未完成` | `<li class="task-item">` + checkbox |
| 引用 | `> 引用内容` | `<blockquote>`（递归渲染） |
| 表格 | `\| 列1 \| 列2 \|` | `.table-wrapper > table`（支持对齐） |
| 分隔线 | `---` | `<hr>` |

### 安全特性

```javascript
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
```

- 所有代码块内容经过 HTML 转义
- 链接和图片的 URL 经过 HTML 转义
- 行内代码先提取保护，避免被其他规则影响
- 链接添加 `target="_blank" rel="noopener"` 防止钓鱼

## ATS 评分组件

### 组件概述

`AnalysisResult.vue` 是 ATS 评分结果展示组件，是前端最复杂的组件，包含多个子模块。

### 功能模块

| 模块 | 描述 |
|-----|------|
| 评分卡片 | ATS 综合评分、职位匹配度、匹配/缺失技能数量 |
| 技能雷达图 | ECharts 雷达图展示各分类匹配度（前端/后端/数据库/DevOps） |
| 技能柱状图 | ECharts 柱状图展示各分类匹配/缺失技能数量对比 |
| 关键词详情 | 已匹配技能、缺失技能、学习建议、学习资源 |
| 优化建议 | 按优先级排序的优化建议卡片（高🔴/中🟡/低🟢） |
| 结构分析 | 简历结构完整性检查、字数统计 |
| PDF 导出 | 分析报告导出为 PDF 格式 |

### 数据获取

```javascript
const fetchAtsScore = async () => {
  const data = await apiPost(API.atsScore, {
    resumeText: props.resumeContent,
    jobDescription: '',
    keyId: props.keyId || null
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
  title: { text: '技能匹配雷达图', left: 'center' },
  tooltip: { trigger: 'item' },
  radar: {
    indicator: categories.map(cat => ({ name: cat, max: 100 })),
    radius: '65%',
    splitNumber: 4
  },
  series: [{
    type: 'radar',
    data: [{
      value: values,
      name: '匹配度',
      areaStyle: { color: 'rgba(37, 99, 235, 0.3)' },
      lineStyle: { color: '#2563eb' },
      itemStyle: { color: '#2563eb' }
    }]
  }]
}
```

### 图表初始化注意事项

1. **DOM 必须先渲染**: 图表容器在 `v-if` 守卫内，必须等 `displayData` 有值后才渲染
2. **双重 nextTick**: 使用 `await nextTick(); await nextTick()` 确保 DOM 完全更新
3. **dispose 重用**: 重新初始化前先 `dispose()` 旧实例，避免内存泄漏
4. **watch 监听**: 监听 `displayData` 变化，数据就绪后自动初始化图表

```javascript
const initCharts = async () => {
  await nextTick()
  await nextTick()
  initRadarChart()
  initBarChart()
}

watch(displayData, (newVal) => {
  if (newVal) {
    nextTick(() => {
      initRadarChart()
      initBarChart()
    })
  }
})

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  // ... setOption
}
```

## 状态管理

### 本地状态

```javascript
const count = ref(0)
const message = ref('')

const state = reactive({
  user: null,
  loading: false,
  error: null
})
```

### 组件间通信

```javascript
// 父组件 (App.vue)
const handleKeyValidated = (result) => {
  activeKeyId.value = result.keyId
}

<ApiKeyInput @key-validated="handleKeyValidated" />

// 子组件
const emit = defineEmits(['key-validated'])
emit('key-validated', { keyId, provider })
```

### App.vue 核心状态

```javascript
const resumeContent = ref('')        // 简历文本内容
const jobDescription = ref('')       // 职位描述
const analysisResult = ref(null)     // 基础分析结果
const activeTab = ref('upload')      // 当前激活的 Tab
const activeKeyId = ref('')          // 当前使用的 API Key ID
const showComparison = ref(false)    // 是否显示模板对比
const optimizedResume = ref('')      // 优化后的简历
const showAiAnalysis = ref(false)    // 是否显示 AI 深度分析
const aiAnalysisResult = ref(null)   // AI 分析结果
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

### ECharts 实例管理

```javascript
// 组件卸载时销毁图表实例
onBeforeUnmount(() => {
  if (radarChart) radarChart.dispose()
  if (barChart) barChart.dispose()
})

// 窗口大小变化时自适应
window.addEventListener('resize', () => {
  radarChart?.resize()
  barChart?.resize()
})
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

### Vite 代理配置

```javascript
// vite.config.js
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

## 浏览器兼容性

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

---

遵循以上规范可以确保代码质量、可维护性和团队协作效率。
