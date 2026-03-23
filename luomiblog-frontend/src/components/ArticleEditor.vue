<template>
  <div class="article-edit-page">
    <!-- 页面头部 -->
    <header class="admin-section-header">
      <div class="header-actions">
        <a href="/admin/articles" class="btn-back">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="m15 18-6-6 6-6"/>
          </svg>
          返回列表
        </a>
        <div class="header-title-group">
          <p class="admin-section-subtitle">content</p>
          <h1 class="admin-section-title">{{ isNew ? '新建文章' : '编辑文章' }}</h1>
        </div>
      </div>
      <div class="header-buttons">
        <button class="admin-btn admin-btn-secondary" @click="previewArticle">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          预览
        </button>
        <button class="admin-btn admin-btn-primary" @click="saveArticle">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
            <polyline points="17 21 17 13 7 13 7 21"/>
            <polyline points="7 3 7 8 15 8"/>
          </svg>
          保存修改
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="loadError" class="error-state">
      <div class="error-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" x2="12" y1="8" y2="12"/>
          <line x1="12" x2="12.01" y1="16" y2="16"/>
        </svg>
      </div>
      <h3>加载失败</h3>
      <p>{{ loadError }}</p>
      <div class="error-actions">
        <button class="admin-btn admin-btn-primary" @click="loadArticle">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
            <path d="M3 3v5h5"/>
            <path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/>
            <path d="M16 16h5v5"/>
          </svg>
          重新加载
        </button>
        <a href="/admin/articles" class="admin-btn admin-btn-secondary">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="m15 18-6-6 6-6"/>
          </svg>
          返回列表
        </a>
      </div>
    </div>

    <div v-else class="editor-layout">
      <!-- 左侧编辑区 -->
      <div class="editor-main">
        <div class="glass-panel editor-panel">
          <div class="form-group">
            <label class="form-label">文章标题</label>
            <input type="text" class="admin-input title-input" v-model="article.title" />
          </div>

          <div class="form-group">
            <label class="form-label">Slug (URL标识)</label>
            <input type="text" class="admin-input" v-model="article.slug" :readonly="!isNew" />
            <span class="form-hint">Slug 确定后尽量不要修改，会影响 SEO</span>
          </div>

          <div class="form-group">
            <label class="form-label">文章摘要</label>
            <textarea class="admin-input summary-input" v-model="article.summary" rows="3"></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">文章内容 (Markdown)</label>
            <div class="editor-toolbar">
              <button type="button" class="toolbar-btn" @click="insertMarkdown('**', '**')" title="加粗">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/><path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/></svg>
              </button>
              <button type="button" class="toolbar-btn" @click="insertMarkdown('*', '*')" title="斜体">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" x2="10" y1="4" y2="4"/><line x1="14" x2="5" y1="20" y2="20"/><line x1="15" x2="9" y1="4" y2="20"/></svg>
              </button>
              <button type="button" class="toolbar-btn" @click="insertMarkdown('# ', '')" title="标题">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 12h12"/><path d="M6 20V4"/><path d="M18 20V4"/></svg>
              </button>
              <button type="button" class="toolbar-btn" @click="insertMarkdown('- ', '')" title="列表">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" x2="21" y1="6" y2="6"/><line x1="8" x2="21" y1="12" y2="12"/><line x1="8" x2="21" y1="18" y2="18"/><line x1="3" x2="3.01" y1="6" y2="6"/><line x1="3" x2="3.01" y1="12" y2="12"/><line x1="3" x2="3.01" y1="18" y2="18"/></svg>
              </button>
              <button type="button" class="toolbar-btn" @click="insertMarkdown('```\n', '\n```')" title="代码块">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
              </button>
              <button type="button" class="toolbar-btn" @click="insertMarkdown('> ', '')" title="引用">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"/></svg>
              </button>
              <button type="button" class="toolbar-btn" @click="insertMarkdown('[', '](url)')" title="链接">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
              </button>
              <button type="button" class="toolbar-btn" @click="insertMarkdown('![alt](', ')')" title="图片">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
              </button>
            </div>
            <textarea ref="contentRef" class="admin-input content-input" v-model="article.content" rows="25"></textarea>
          </div>
        </div>
      </div>

      <!-- 右侧设置区 -->
      <div class="editor-sidebar">
        <div class="glass-panel settings-panel">
          <h3 class="panel-title">文章设置</h3>

          <div class="form-group">
            <label class="form-label">发布状态</label>
            <select class="admin-input" v-model="article.status">
              <option value="draft">草稿</option>
              <option value="published">已发布</option>
              <option value="archived">已归档</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">分类</label>
            <select class="admin-input" v-model="article.categoryId">
              <option value="">选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">标签</label>
            <input type="text" class="admin-input" v-model="tagsInput" />
            <span class="form-hint">用逗号分隔</span>
          </div>

          <div class="form-group">
            <label class="form-label">封面图片</label>
            <input type="text" class="admin-input" v-model="article.coverImage" />
          </div>

          <div class="form-group">
            <label class="form-label">作者</label>
            <input type="text" class="admin-input" v-model="article.author" />
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="article.allowComments" />
              <span>允许评论</span>
            </label>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="article.allowSuggestions" />
              <span>允许访客建议</span>
            </label>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="article.top" />
              <span>置顶文章</span>
            </label>
          </div>

          <div v-if="!isNew" class="form-actions">
            <button class="admin-btn admin-btn-danger" @click="deleteArticle">删除文章</button>
          </div>
        </div>

        <div v-if="!isNew && articleInfo.id" class="glass-panel info-panel">
          <h3 class="panel-title">文章信息</h3>
          <div class="info-item">
            <span class="info-label">文章ID</span>
            <span class="info-value">{{ articleInfo.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDate(articleInfo.createdAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">最后修改</span>
            <span class="info-value">{{ formatDate(articleInfo.updatedAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">阅读量</span>
            <span class="info-value">{{ articleInfo.viewCount || 0 }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">点赞数</span>
            <span class="info-value">{{ articleInfo.likeCount || 0 }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { API_BASE_URL } from '../config/api';

interface Article {
  id?: number;
  title: string;
  slug: string;
  summary: string;
  content: string;
  status: 'draft' | 'published' | 'archived';
  categoryId: string | number;
  tags: string[];
  coverImage: string;
  author: string;
  allowComments: boolean;
  allowSuggestions: boolean;
  top: boolean;
  language: string;
}

interface Category {
  id: number;
  name: string;
}

const props = defineProps<{
  slug?: string;
}>();

const loading = ref(true);
const loadError = ref<string | null>(null);
const categories = ref<Category[]>([]);
const contentRef = ref<HTMLTextAreaElement>();

const article = ref<Article>({
  title: '',
  slug: props.slug || '',
  summary: '',
  content: '',
  status: 'draft',
  categoryId: '',
  tags: [],
  coverImage: '',
  author: '',
  allowComments: true,
  allowSuggestions: false,
  top: false,
  language: 'zh'
});

const articleInfo = ref({
  id: null as number | null,
  createdAt: '',
  updatedAt: '',
  viewCount: 0,
  likeCount: 0
});

const isNew = computed(() => !props.slug);

const tagsInput = computed({
  get: () => article.value.tags.join(', '),
  set: (val: string) => {
    article.value.tags = val.split(',').map(t => t.trim()).filter(t => t);
  }
});

function getToken() {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('zh-CN');
}

async function loadCategories() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/categories`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });
    if (response.ok) {
      const result = await response.json();
      if (result.code === 200) {
        categories.value = result.data;
      }
    }
  } catch (error) {
    console.error('加载分类失败:', error);
  }
}

async function loadArticle() {
  if (!props.slug) {
    loading.value = false;
    return;
  }

  try {
    loadError.value = null;
    const response = await fetch(`${API_BASE_URL}/api/articles/${props.slug}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });

    if (!response.ok) {
      if (response.status === 404) {
        loadError.value = '文章不存在，可能已被删除或移动';
        return;
      }
      throw new Error('加载文章失败');
    }

    const result = await response.json();
    if (result.code !== 200) {
      throw new Error(result.message || '加载文章失败');
    }

    const data = result.data;
    article.value = {
      id: data.id,
      title: data.title || '',
      slug: data.slug || '',
      summary: data.summary || '',
      content: data.content || '',
      status: data.status || 'draft',
      categoryId: data.categoryId || '',
      tags: data.tags || [],
      coverImage: data.coverImage || '',
      author: data.author || '',
      allowComments: data.allowComments !== false,
      allowSuggestions: data.allowSuggestions === true,
      top: data.top === true,
      language: data.language || 'zh'
    };

    articleInfo.value = {
      id: data.id,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      viewCount: data.viewCount || 0,
      likeCount: data.likeCount || 0
    };
  } catch (error) {
    console.error('加载文章失败:', error);
    loadError.value = '无法连接到服务器，请检查网络连接或稍后重试';
  } finally {
    loading.value = false;
  }
}

function insertMarkdown(before: string, after: string) {
  const textarea = contentRef.value;
  if (!textarea) return;

  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const text = article.value.content;
  const selected = text.substring(start, end);

  article.value.content = text.substring(0, start) + before + selected + after + text.substring(end);
  
  setTimeout(() => {
    textarea.focus();
    textarea.selectionStart = start + before.length;
    textarea.selectionEnd = start + before.length + selected.length;
  }, 0);
}

function previewArticle() {
  if (article.value.slug) {
    window.open(`/article/${article.value.slug}`, '_blank');
  } else {
    ElMessage.warning('请先设置文章 URL 标识');
  }
}

async function saveArticle() {
  try {
    const payload = { ...article.value };
    
    let response;
    if (isNew.value) {
      // 创建新文章
      response = await fetch(`${API_BASE_URL}/api/articles`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${getToken()}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
    } else {
      // 更新现有文章
      response = await fetch(`${API_BASE_URL}/api/articles/${article.value.id}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${getToken()}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
    }

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || '保存失败');
    }

    const result = await response.json();
    if (result.code !== 200) {
      throw new Error(result.message || '保存失败');
    }

    ElMessage.success('文章保存成功');
    
    if (isNew.value) {
      // 新建文章后跳转到编辑页面
      window.location.href = `/admin/articles/${result.data.slug}`;
    } else {
      // 更新文章信息
      articleInfo.value.updatedAt = result.data.updatedAt;
    }
  } catch (error) {
    console.error('保存文章失败:', error);
    ElMessage.error('保存失败: ' + (error as Error).message);
  }
}

async function deleteArticle() {
  try {
    await ElMessageBox.confirm(
      '确定要删除这篇文章吗？删除后将无法恢复',
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    const response = await fetch(`${API_BASE_URL}/api/admin/articles/${article.value.id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || '删除失败');
    }

    ElMessage.success('文章已删除');
    window.location.href = '/admin/articles';
  } catch (error) {
    if ((error as Error).message === 'cancel') {
      return;
    }
    console.error('删除文章失败:', error);
    ElMessage.error('删除失败: ' + (error as Error).message);
  }
}

onMounted(async () => {
  await loadCategories();
  await loadArticle();
});
</script>

<style scoped>
.article-edit-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  gap: 16px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 107, 157, 0.2);
  border-top-color: #ff6b9d;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.admin-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--admin-card-border);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  color: var(--admin-text-secondary);
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.2s ease;
  font-size: 14px;
}

.btn-back:hover {
  background: var(--admin-hover-bg);
  color: var(--admin-text);
}

.header-title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-section-subtitle {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--admin-primary);
  font-weight: 600;
  margin: 0;
}

.admin-section-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--admin-text);
  margin: 0;
}

.header-buttons {
  display: flex;
  gap: 12px;
}

.admin-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.admin-btn-primary {
  background: linear-gradient(135deg, #ff6b9d 0%, #c44569 100%);
  color: white;
}

.admin-btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.admin-btn-secondary {
  background: var(--admin-input-bg);
  color: var(--admin-text);
}

.admin-btn-secondary:hover {
  background: var(--admin-hover-bg);
}

.admin-btn-danger {
  background: rgba(239, 68, 68, 0.1);
  color: var(--admin-danger);
  width: 100%;
  justify-content: center;
}

.admin-btn-danger:hover {
  background: rgba(239, 68, 68, 0.2);
}

.editor-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}

.glass-panel {
  background: var(--admin-card-bg);
  border: 1px solid var(--admin-card-border);
  border-radius: 12px;
  padding: 24px;
}

.editor-panel {
  min-height: calc(100vh - 200px);
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--admin-text);
  margin-bottom: 8px;
}

.form-hint {
  display: block;
  font-size: 12px;
  color: var(--admin-text-muted);
  margin-top: 4px;
}

.admin-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--admin-card-border);
  border-radius: 8px;
  background: var(--admin-input-bg);
  color: var(--admin-text);
  font-size: 14px;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.admin-input:focus {
  outline: none;
  border-color: var(--admin-primary);
}

.title-input {
  font-size: 18px;
  font-weight: 500;
}

.summary-input {
  resize: vertical;
  min-height: 80px;
}

.content-input {
  resize: vertical;
  min-height: 400px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.editor-toolbar {
  display: flex;
  gap: 4px;
  padding: 8px;
  background: var(--admin-input-bg);
  border: 1px solid var(--admin-card-border);
  border-bottom: none;
  border-radius: 8px 8px 0 0;
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--admin-text-secondary);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toolbar-btn:hover {
  background: var(--admin-hover-bg);
  color: var(--admin-text);
}

.editor-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-panel,
.info-panel {
  padding: 20px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--admin-text);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--admin-card-border);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--admin-text);
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: var(--admin-primary);
}

.form-actions {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--admin-card-border);
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--admin-card-border);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: var(--admin-text-muted);
}

.info-value {
  font-size: 13px;
  color: var(--admin-text);
  font-weight: 500;
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  background: var(--admin-card-bg);
  border: 1px solid var(--admin-card-border);
  border-radius: 16px;
  margin: 20px;
}

.error-icon {
  color: var(--admin-error);
  margin-bottom: 20px;
  opacity: 0.8;
}

.error-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--admin-text);
  margin: 0 0 12px 0;
}

.error-state p {
  font-size: 15px;
  color: var(--admin-text-secondary);
  margin: 0 0 24px 0;
  max-width: 400px;
  line-height: 1.6;
}

.error-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

@media (max-width: 1024px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .editor-sidebar {
    order: -1;
  }
}
</style>
