---
title: 'Vue 3 Composition API 最佳实践'
description: '深入理解 Vue 3 Composition API，掌握组合式函数的设计模式与代码组织技巧，构建可维护的大型应用。'
pubDate: 2026-02-22
author: '辰汐'
tags: ['Vue', 'JavaScript', '前端开发', 'Composition API']
category: '前端架构'
cover: 'https://picsum.photos/seed/vue/800/500'
views: 312
comments: 15
likes: 78
slug: 'vue3-composition-api'
---

# Vue 3 Composition API 最佳实践

Vue 3 的 Composition API 为构建大型应用提供了更灵活的代码组织方式。本文将分享在实际项目中总结的最佳实践，帮助你写出更优雅、可维护的 Vue 代码。

## 为什么需要 Composition API？

### Options API 的局限性

在大型组件中，Options API 存在以下问题：

```javascript
// 同一个功能的代码分散在不同选项中
export default {
  data() {
    return {
      searchQuery: '',
      searchResults: [],
      loading: false,
      error: null
    }
  },
  methods: {
    // 搜索相关的逻辑
    async search() { /* ... */ },
    clearSearch() { /* ... */ }
  },
  computed: {
    // 搜索相关的计算属性
    hasResults() { /* ... */ }
  },
  watch: {
    // 搜索相关的监听
    searchQuery: 'onQueryChange'
  },
  mounted() {
    // 搜索相关的初始化
    this.loadRecentSearches()
  }
}
```

### Composition API 的优势

```javascript
// 相关逻辑组合在一起
import { useSearch } from './composables/useSearch'

export default {
  setup() {
    const { 
      searchQuery, 
      searchResults, 
      loading, 
      search,
      hasResults 
    } = useSearch()
    
    return {
      searchQuery,
      searchResults,
      loading,
      search,
      hasResults
    }
  }
}
```

## 组合式函数设计模式

### 1. 单一职责原则

每个组合式函数只负责一个功能：

```typescript
// ✅ 好的设计：专注用户认证
export function useAuth() {
  const user = ref<User | null>(null)
  const isAuthenticated = computed(() => !!user.value)
  
  const login = async (credentials: Credentials) => {
    // 登录逻辑
  }
  
  const logout = async () => {
    // 登出逻辑
  }
  
  return {
    user,
    isAuthenticated,
    login,
    logout
  }
}

// ✅ 好的设计：专注表单处理
export function useForm<T>(options: FormOptions<T>) {
  const values = ref<T>(options.initialValues)
  const errors = ref<FormErrors>({})
  const isSubmitting = ref(false)
  
  const validate = () => {
    // 验证逻辑
  }
  
  const handleSubmit = async (onSubmit: (values: T) => Promise<void>) => {
    if (!validate()) return
    isSubmitting.value = true
    try {
      await onSubmit(values.value)
    } finally {
      isSubmitting.value = false
    }
  }
  
  return {
    values,
    errors,
    isSubmitting,
    handleSubmit
  }
}
```

### 2. 可配置化设计

```typescript
interface UsePaginationOptions {
  pageSize?: number
  initialPage?: number
  fetchFn: (page: number, pageSize: number) => Promise<PaginatedResult<any>>
}

export function usePagination<T>(options: UsePaginationOptions) {
  const { 
    pageSize = 10, 
    initialPage = 1,
    fetchFn 
  } = options
  
  const currentPage = ref(initialPage)
  const items = ref<T[]>([])
  const total = ref(0)
  const loading = ref(false)
  
  const fetchData = async () => {
    loading.value = true
    try {
      const result = await fetchFn(currentPage.value, pageSize)
      items.value = result.items
      total.value = result.total
    } finally {
      loading.value = false
    }
  }
  
  const totalPages = computed(() => Math.ceil(total.value / pageSize))
  
  const goToPage = (page: number) => {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
    fetchData()
  }
  
  // 初始加载
  fetchData()
  
  return {
    currentPage,
    items,
    total,
    loading,
    totalPages,
    goToPage,
    refresh: fetchData
  }
}

// 使用示例
const { 
  items: articles, 
  currentPage, 
  totalPages, 
  goToPage 
} = usePagination({
  pageSize: 20,
  fetchFn: articleApi.getList
})
```

### 3. 副作用管理

```typescript
export function useEventListener(
  target: EventTarget,
  event: string,
  callback: EventListener
) {
  onMounted(() => {
    target.addEventListener(event, callback)
  })
  
  onUnmounted(() => {
    target.removeEventListener(event, callback)
  })
}

export function useInterval(callback: () => void, delay: number) {
  let timer: number | null = null
  
  const start = () => {
    timer = window.setInterval(callback, delay)
  }
  
  const stop = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
  
  onUnmounted(stop)
  
  return { start, stop }
}

export function useAsyncTask<T>(asyncFn: () => Promise<T>) {
  const data = ref<T | null>(null)
  const error = ref<Error | null>(null)
  const loading = ref(false)
  
  const execute = async () => {
    loading.value = true
    error.value = null
    try {
      data.value = await asyncFn()
    } catch (e) {
      error.value = e as Error
    } finally {
      loading.value = false
    }
  }
  
  return {
    data,
    error,
    loading,
    execute
  }
}
```

## 实战案例：文章管理

### 组合式函数

```typescript
// composables/useArticleManager.ts
import { ref, computed } from 'vue'
import type { Article, ArticleFilters } from '@/types'

export function useArticleManager() {
  // 状态
  const articles = ref<Article[]>([])
  const filters = ref<ArticleFilters>({
    category: null,
    status: 'published',
    search: ''
  })
  const loading = ref(false)
  const selectedIds = ref<Set<string>>(new Set())
  
  // 计算属性
  const filteredArticles = computed(() => {
    return articles.value.filter(article => {
      if (filters.value.category && article.category !== filters.value.category) {
        return false
      }
      if (filters.value.status && article.status !== filters.value.status) {
        return false
      }
      if (filters.value.search) {
        const search = filters.value.search.toLowerCase()
        return article.title.toLowerCase().includes(search) ||
               article.summary.toLowerCase().includes(search)
      }
      return true
    })
  })
  
  const selectedCount = computed(() => selectedIds.value.size)
  const isAllSelected = computed(() => 
    filteredArticles.value.length > 0 && 
    filteredArticles.value.every(a => selectedIds.value.has(a.id))
  )
  
  // 方法
  const fetchArticles = async () => {
    loading.value = true
    try {
      articles.value = await articleApi.getAll()
    } finally {
      loading.value = false
    }
  }
  
  const updateFilters = (newFilters: Partial<ArticleFilters>) => {
    filters.value = { ...filters.value, ...newFilters }
    // 重置选择
    selectedIds.value.clear()
  }
  
  const toggleSelection = (id: string) => {
    if (selectedIds.value.has(id)) {
      selectedIds.value.delete(id)
    } else {
      selectedIds.value.add(id)
    }
  }
  
  const toggleAllSelection = () => {
    if (isAllSelected.value) {
      selectedIds.value.clear()
    } else {
      filteredArticles.value.forEach(a => selectedIds.value.add(a.id))
    }
  }
  
  const deleteSelected = async () => {
    if (selectedCount.value === 0) return
    
    const confirmed = await confirm(`确定要删除选中的 ${selectedCount.value} 篇文章吗？`)
    if (!confirmed) return
    
    loading.value = true
    try {
      await articleApi.deleteMany(Array.from(selectedIds.value))
      await fetchArticles()
      selectedIds.value.clear()
    } finally {
      loading.value = false
    }
  }
  
  return {
    // 状态
    articles,
    filters,
    loading,
    selectedIds,
    // 计算属性
    filteredArticles,
    selectedCount,
    isAllSelected,
    // 方法
    fetchArticles,
    updateFilters,
    toggleSelection,
    toggleAllSelection,
    deleteSelected
  }
}
```

### 组件中使用

```vue
<template>
  <div class="article-manager">
    <!-- 筛选栏 -->
    <FilterBar 
      :filters="filters"
      @update="updateFilters"
    />
    
    <!-- 批量操作 -->
    <BulkActions
      :selected-count="selectedCount"
      @delete="deleteSelected"
    />
    
    <!-- 文章列表 -->
    <ArticleTable
      :articles="filteredArticles"
      :loading="loading"
      :selected-ids="selectedIds"
      :is-all-selected="isAllSelected"
      @toggle-selection="toggleSelection"
      @toggle-all="toggleAllSelection"
    />
  </div>
</template>

<script setup lang="ts">
import { useArticleManager } from './composables/useArticleManager'
import FilterBar from './components/FilterBar.vue'
import BulkActions from './components/BulkActions.vue'
import ArticleTable from './components/ArticleTable.vue'

const {
  filters,
  loading,
  selectedIds,
  filteredArticles,
  selectedCount,
  isAllSelected,
  fetchArticles,
  updateFilters,
  toggleSelection,
  toggleAllSelection,
  deleteSelected
} = useArticleManager()

// 初始加载
fetchArticles()
</script>
```

## 代码组织建议

### 目录结构

```
src/
├── composables/          # 全局组合式函数
│   ├── useAuth.ts
│   ├── useHttp.ts
│   ├── useStorage.ts
│   └── index.ts
├── features/             # 功能模块
│   ├── articles/
│   │   ├── components/   # 组件
│   │   ├── composables/  # 模块级组合式函数
│   │   ├── stores/       # Pinia store
│   │   └── api.ts        # API 调用
│   └── users/
│       └── ...
└── shared/               # 共享资源
    ├── utils/
    ├── types/
    └── constants/
```

### 命名规范

```typescript
// 组合式函数：use + 功能名
useAuth()
usePagination()
useFormValidation()

// 状态：名词
const user = ref<User>()
const articles = ref<Article[]>()
const loading = ref(false)

// 计算属性：is/has + 形容词
const isAuthenticated = computed(() => !!user.value)
const hasErrors = computed(() => errors.value.length > 0)
const isAllSelected = computed(() => ...)

// 方法：动词 + 名词
const fetchArticles = async () => { }
const validateForm = () => { }
const toggleSelection = (id: string) => { }
```

## 常见陷阱与解决方案

### 1. 响应式丢失

```typescript
// ❌ 错误：解构会失去响应式
const { count, increment } = useCounter()

// ✅ 正确：保持响应式
const counter = useCounter()
// 在模板中使用 counter.count
```

### 2. 异步 setup

```typescript
// ❌ 错误：setup 不能是 async
async setup() {
  const data = await fetchData() // 会导致问题
}

// ✅ 正确：在 onMounted 中处理
setup() {
  const data = ref(null)
  
  onMounted(async () => {
    data.value = await fetchData()
  })
  
  return { data }
}

// ✅ 或者使用 Suspense
async setup() {
  const data = await fetchData()
  return { data }
}
// 配合 <Suspense> 组件使用
```

### 3. 循环依赖

```typescript
// ❌ 错误：循环依赖
const useA = () => {
  const { b } = useB()
  return { a: computed(() => b.value + 1) }
}

const useB = () => {
  const { a } = useA()
  return { b: computed(() => a.value - 1) }
}

// ✅ 正确：提取共享状态
const useShared = () => {
  const base = ref(0)
  const a = computed(() => base.value + 1)
  const b = computed(() => base.value - 1)
  return { base, a, b }
}
```

## 总结

Composition API 为 Vue 开发带来了新的可能性：

1. **逻辑复用**：通过组合式函数实现代码复用
2. **类型支持**：更好的 TypeScript 支持
3. **代码组织**：按功能组织代码，而非选项类型
4. **灵活性**：更灵活的代码结构

掌握这些最佳实践，将帮助你构建更优雅、可维护的 Vue 3 应用。

---

**参考资源**：
- [Vue 3 官方文档](https://vuejs.org/guide/extras/composition-api-faq.html)
- [VueUse](https://vueuse.org/) - 实用的组合式函数集合
- [Vue 3 迁移指南](https://v3-migration.vuejs.org/)
