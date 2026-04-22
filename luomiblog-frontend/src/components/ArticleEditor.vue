<template>
  <div class="article-edit-page">
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
        <button class="admin-btn admin-btn-secondary" @click="togglePreview">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          {{ showPreview ? '编辑' : '预览' }}
        </button>
        <button
          class="admin-btn admin-btn-primary"
          :class="{ 'is-loading': isSubmitting }"
          :disabled="isSubmitting"
          @click="saveArticle"
        >
          <svg v-if="!isSubmitting" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
            <polyline points="17 21 17 13 7 13 7 21"/>
            <polyline points="7 3 7 8 15 8"/>
          </svg>
          <span v-if="isSubmitting" class="btn-spinner"></span>
          {{ isSubmitting ? '保存中...' : (isNew ? '发布文章' : '保存修改') }}
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
      <div class="editor-main">
        <div class="glass-panel editor-panel">
          <div class="form-group" :class="{ 'has-error': validationErrors.title }">
            <label class="form-label">
              文章标题
              <span class="required-mark">*</span>
            </label>
            <input
              type="text"
              class="admin-input title-input"
              :class="{ 'input-error': validationErrors.title }"
              v-model="article.title"
              @input="clearFieldError('title')"
              placeholder="请输入文章标题"
              maxlength="200"
            />
            <span v-if="validationErrors.title" class="field-error">{{ validationErrors.title }}</span>
            <span v-else class="form-hint">文章标题是必填项，建议控制在 200 字符以内</span>
          </div>

          <div class="form-group" :class="{ 'has-error': validationErrors.slug }" v-if="isNew">
            <label class="form-label">
              Slug (URL标识)
              <span class="required-mark">*</span>
            </label>
            <input
              type="text"
              class="admin-input"
              :class="{ 'input-error': validationErrors.slug }"
              v-model="article.slug"
              @input="clearFieldError('slug')"
              placeholder="请输入URL标识，如：my-first-article"
              maxlength="100"
            />
            <span v-if="validationErrors.slug" class="field-error">{{ validationErrors.slug }}</span>
            <span v-else class="form-hint">只能包含小写字母、数字和连字符，确定后尽量不要修改</span>
          </div>

          <div class="form-group">
            <label class="form-label">文章摘要</label>
            <textarea class="admin-input summary-input" v-model="article.summary" rows="3"></textarea>
          </div>

          <div class="form-group" :class="{ 'has-error': validationErrors.content }">
            <label class="form-label">
              文章内容 (Markdown + LaTeX)
              <span class="required-mark">*</span>
            </label>
            <div class="editor-toolbar">
              <div class="toolbar-group">
                <button type="button" class="toolbar-btn" @click="insertMarkdown('**', '**')" title="加粗">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/><path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('*', '*')" title="斜体">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" x2="10" y1="4" y2="4"/><line x1="14" x2="5" y1="20" y2="20"/><line x1="15" x2="9" y1="4" y2="20"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('~~', '~~')" title="删除线">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 4H9a3 3 0 0 0 0 6h6a3 3 0 0 1 0 6H8"/><path d="M4 12h16"/></svg>
                </button>
              </div>

              <div class="toolbar-divider"></div>

              <div class="toolbar-group">
                <button type="button" class="toolbar-btn" @click="insertMarkdown('## ', '')" title="二级标题">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 12h12"/><path d="M6 20V4"/><path d="M18 20V4"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('- ', '')" title="无序列表">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" x2="21" y1="6" y2="6"/><line x1="8" x2="21" y1="12" y2="12"/><line x1="8" x2="21" y1="18" y2="18"/><line x1="3" x2="3.01" y1="6" y2="6"/><line x1="3" x2="3.01" y1="12" y2="12"/><line x1="3" x2="3.01" y1="18" y2="18"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('1. ', '')" title="有序列表">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="10" x2="21" y1="6" y2="6"/><line x1="10" x2="21" y1="12" y2="12"/><line x1="10" x2="21" y1="18" y2="18"/><path d="M4 6h1v4"/><path d="M4 10h2"/><path d="M6 18H4c0-1 2-2 2-3s-1-1.5-2-1"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('- [ ] ', '')" title="任务列表">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2"/></svg>
                </button>
              </div>

              <div class="toolbar-divider"></div>

              <div class="toolbar-group">
                <button type="button" class="toolbar-btn" @click="insertMarkdown('> ', '')" title="引用">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('```\n', '\n```')" title="代码块">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertTable" title="表格">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2"/><line x1="3" x2="21" y1="9" y2="9"/><line x1="3" x2="21" y1="15" y2="15"/><line x1="9" x2="9" y1="3" y2="21"/><line x1="15" x2="15" y1="3" y2="21"/></svg>
                </button>
              </div>

              <div class="toolbar-divider"></div>

              <div class="toolbar-group">
                <button type="button" class="toolbar-btn" @click="insertMarkdown('[', '](url)')" title="链接">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('![alt](', ')')" title="图片">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
                </button>
              </div>

              <div class="toolbar-divider"></div>

              <div class="toolbar-group">
                <button type="button" class="toolbar-btn latex-btn" @click="insertMarkdown('$', '$')" title="行内公式 (LaTeX)">
                  <span class="toolbar-btn-text">f(x)</span>
                </button>
                <button type="button" class="toolbar-btn latex-btn" @click="insertBlockFormula" title="块级公式 (LaTeX)">
                  <span class="toolbar-btn-text">F(x)</span>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMermaid" title="流程图 (Mermaid)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="6" height="6" x="2" y="2" rx="1"/><rect width="6" height="6" x="16" y="2" rx="1"/><rect width="6" height="6" x="9" y="16" rx="1"/><path d="M5 8v3a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8"/><path d="M12 12v4"/></svg>
                </button>
                <button type="button" class="toolbar-btn" @click="insertMarkdown('<details>\n<summary>', '</summary>\n\n内容\n</details>')" title="折叠块">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/><rect width="18" height="18" x="3" y="3" rx="2"/></svg>
                </button>
              </div>
            </div>

            <div class="editor-content-area">
              <textarea
                ref="contentRef"
                v-show="!showPreview"
                class="admin-input content-input"
                :class="{ 'input-error': validationErrors.content }"
                v-model="article.content"
                @input="clearFieldError('content'); updateWordCount()"
                rows="25"
                placeholder="请输入文章内容，支持 Markdown + LaTeX 格式...&#10;&#10;示例：&#10;## 二级标题&#10;正文内容，行内公式 $E=mc^2$&#10;&#10;$$&#10;块级公式：&#10;J(\theta) = \frac{1}{2m}\sum_{i=1}^{m}(h_\theta(x^{(i)}) - y^{(i)})^2&#10;$$"
              ></textarea>

              <div v-if="showPreview" class="preview-panel prose" v-html="renderedContent"></div>
            </div>

            <div class="content-status-bar">
              <span v-if="validationErrors.content" class="field-error">{{ validationErrors.content }}</span>
              <span v-else class="form-hint">支持 Markdown + LaTeX ($...$ / $$...$$) + Mermaid 流程图</span>
              <div class="word-count-info">
                <span class="word-count">{{ wordCount }} 字</span>
                <span class="read-time">约 {{ readTime }} 分钟阅读</span>
              </div>
            </div>
          </div>
        </div>
      </div>

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

          <div class="form-group" :class="{ 'has-error': validationErrors.categoryId }">
            <label class="form-label">
              分类
              <span v-if="article.status === 'published'" class="required-mark">*</span>
            </label>
            <select
              class="admin-input"
              :class="{ 'input-error': validationErrors.categoryId }"
              v-model="article.categoryId"
              @change="clearFieldError('categoryId')"
            >
              <option value="">选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
            <span v-if="validationErrors.categoryId" class="field-error">{{ validationErrors.categoryId }}</span>
            <span v-else-if="article.status === 'published'" class="form-hint">发布文章必须选择分类</span>
          </div>

          <div class="form-group">
            <label class="form-label">标签</label>
            <input type="text" class="admin-input" v-model="tagsInput" />
            <span class="form-hint">用逗号分隔</span>
          </div>

          <div class="form-group">
            <label class="form-label">封面图片</label>
            <input type="text" class="admin-input" v-model="article.coverImage" placeholder="输入图片URL" />
          </div>

          <div class="form-group author-field">
            <label class="form-label">
              作者
              <span class="field-hint" v-if="isNew">(默认为当前登录用户)</span>
            </label>
            <input
              type="text"
              class="admin-input"
              v-model="article.author"
              :placeholder="getCurrentUserDisplayName()"
              :disabled="!canEditAuthor()"
            />
            <span v-if="!canEditAuthor()" class="field-hint">仅博主和管理员可修改作者</span>
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

        <div class="glass-panel format-help-panel">
          <h3 class="panel-title">格式帮助</h3>
          <div class="help-item">
            <code>**粗体**</code>
            <span>粗体文字</span>
          </div>
          <div class="help-item">
            <code>*斜体*</code>
            <span>斜体文字</span>
          </div>
          <div class="help-item">
            <code>~~删除线~~</code>
            <span>删除文字</span>
          </div>
          <div class="help-item">
            <code>$公式$</code>
            <span>行内公式</span>
          </div>
          <div class="help-item">
            <code>$$公式$$</code>
            <span>块级公式</span>
          </div>
          <div class="help-item">
            <code>```mermaid</code>
            <span>流程图</span>
          </div>
          <div class="help-item">
            <code>[链接](url)</code>
            <span>超链接</span>
          </div>
          <div class="help-item">
            <code>![alt](url)</code>
            <span>图片</span>
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
import { getUser } from '../stores/user';

interface Article {
  id?: number;
  title: string;
  slug: string;
  summary: string;
  content: string;
  status: 'draft' | 'published' | 'archived';
  categoryId: number | null;
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
const showPreview = ref(false);
const wordCount = ref(0);
const readTime = ref(0);

const currentSlug = computed(() => {
  if (props.slug) return props.slug;
  const pathParts = window.location.pathname.split('/');
  const slugFromUrl = pathParts[pathParts.length - 1];
  return slugFromUrl && slugFromUrl !== 'new' ? slugFromUrl : '';
});

const validationErrors = ref<{
  title: string;
  content: string;
  slug: string;
  categoryId: string;
}>({
  title: '',
  content: '',
  slug: '',
  categoryId: ''
});

const isSubmitting = ref(false);
const hasAttemptedSubmit = ref(false);

const article = ref<Article>({
  title: '',
  slug: '',
  summary: '',
  content: '',
  status: 'draft',
  categoryId: null,
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

const isNew = computed(() => !currentSlug.value);

const tagsInput = computed({
  get: () => article.value.tags.join(', '),
  set: (val: string) => {
    article.value.tags = val.split(',').map(t => t.trim()).filter(t => t);
  }
});

const renderedContent = computed(() => {
  return renderMarkdown(article.value.content);
});

function renderMarkdown(content: string): string {
  if (!content) return '<p class="preview-empty">暂无内容，请在编辑区输入 Markdown 内容</p>';

  let html = content;

  html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, (_match, lang, code) => {
    const language = lang || 'text';
    return `<pre><code class="language-${language}">${escapeHtml(code.trim())}</code></pre>`;
  });

  html = html.replace(/\$\$([\s\S]*?)\$\$/g, (_match, formula) => {
    return `<div class="math-block">$$${formula.trim()}$$</div>`;
  });

  html = html.replace(/\$([^\$\n]+?)\$/g, (_match, formula) => {
    return `<span class="math-inline">$${formula}$</span>`;
  });

  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');

  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
  html = html.replace(/~~(.+?)~~/g, '<del>$1</del>');

  html = html.replace(/^> (.+)$/gm, '<blockquote><p>$1</p></blockquote>');

  html = html.replace(/^(\d+)\. (.+)$/gm, '<li>$2</li>');

  html = html.replace(/^- \[x\] (.+)$/gm, '<li><input type="checkbox" checked disabled /> $1</li>');
  html = html.replace(/^- \[ \] (.+)$/gm, '<li><input type="checkbox" disabled /> $1</li>');
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>');

  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" />');
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');

  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

  html = html.replace(/\n\n/g, '</p><p>');
  html = html.replace(/\n/g, '<br />');
  html = '<p>' + html + '</p>';

  html = html.replace(/<p><\/p>/g, '');
  html = html.replace(/<p>(<h[1-6]>)/g, '$1');
  html = html.replace(/(<\/h[1-6]>)<\/p>/g, '$1');
  html = html.replace(/<p>(<pre>)/g, '$1');
  html = html.replace(/(<\/pre>)<\/p>/g, '$1');
  html = html.replace(/<p>(<blockquote>)/g, '$1');
  html = html.replace(/(<\/blockquote>)<\/p>/g, '$1');
  html = html.replace(/<p>(<div class="math-block">)/g, '$1');
  html = html.replace(/(<\/div>)<\/p>/g, '$1');

  return html;
}

function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  };
  return text.replace(/[&<>"']/g, m => map[m]);
}

function updateWordCount() {
  const content = article.value.content || '';
  const chineseChars = (content.match(/[\u4e00-\u9fa5]/g) || []).length;
  const englishWords = (content.match(/[a-zA-Z]+/g) || []).length;
  wordCount.value = chineseChars + englishWords;
  readTime.value = Math.max(1, Math.ceil(wordCount.value / 300));
}

function togglePreview() {
  showPreview.value = !showPreview.value;
  if (showPreview.value) {
    renderKaTeX();
  }
}

function renderKaTeX() {
  setTimeout(() => {
    const previewPanel = document.querySelector('.preview-panel');
    if (!previewPanel) return;

    const mathBlocks = previewPanel.querySelectorAll('.math-block');
    mathBlocks.forEach((block) => {
      const formula = block.textContent?.replace(/^\$\$/, '').replace(/\$\$$/, '').trim() || '';
      try {
        if ((window as any).katex) {
          block.innerHTML = (window as any).katex.renderToString(formula, { displayMode: true, throwOnError: false });
        }
      } catch (e) {
        console.warn('KaTeX render error:', e);
      }
    });

    const mathInlines = previewPanel.querySelectorAll('.math-inline');
    mathInlines.forEach((inline) => {
      const formula = inline.textContent?.replace(/^\$/, '').replace(/\$$/, '').trim() || '';
      try {
        if ((window as any).katex) {
          inline.innerHTML = (window as any).katex.renderToString(formula, { displayMode: false, throwOnError: false });
        }
      } catch (e) {
        console.warn('KaTeX render error:', e);
      }
    });
  }, 100);
}

function getToken() {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
}

function clearFieldError(field: keyof typeof validationErrors.value) {
  validationErrors.value[field] = '';
}

function validateArticle(): boolean {
  hasAttemptedSubmit.value = true;
  let isValid = true;
  const errors: string[] = [];

  validationErrors.value = {
    title: '',
    content: '',
    slug: '',
    categoryId: ''
  };

  const title = article.value.title?.trim() || '';
  if (!title) {
    validationErrors.value.title = '请输入文章标题';
    errors.push('文章标题不能为空');
    isValid = false;
  } else if (title.length < 2) {
    validationErrors.value.title = '文章标题至少需要2个字符';
    errors.push('文章标题过短');
    isValid = false;
  } else if (title.length > 200) {
    validationErrors.value.title = '文章标题不能超过200个字符';
    errors.push('文章标题过长');
    isValid = false;
  }

  const content = article.value.content?.trim() || '';
  if (!content) {
    validationErrors.value.content = '请输入文章内容';
    errors.push('文章内容不能为空');
    isValid = false;
  } else if (content.length < 10) {
    validationErrors.value.content = '文章内容至少需要10个字符';
    errors.push('文章内容过短');
    isValid = false;
  }

  if (isNew.value) {
    const slug = article.value.slug?.trim() || '';
    if (!slug) {
      validationErrors.value.slug = '请输入URL标识';
      errors.push('URL标识不能为空');
      isValid = false;
    } else if (!/^[a-z0-9-]+$/.test(slug)) {
      validationErrors.value.slug = 'URL标识只能包含小写字母、数字和连字符';
      errors.push('URL标识格式不正确');
      isValid = false;
    } else if (slug.length > 100) {
      validationErrors.value.slug = 'URL标识不能超过100个字符';
      errors.push('URL标识过长');
      isValid = false;
    }
  }

  if (article.value.status === 'published' && !article.value.categoryId) {
    validationErrors.value.categoryId = '发布文章需要选择分类';
    errors.push('请选择文章分类');
    isValid = false;
  }

  if (!isValid && errors.length > 0) {
    const errorMessage = errors.length === 1
      ? errors[0]
      : `表单填写有误，共有 ${errors.length} 处错误需要修正`;

    ElMessage({
      message: errorMessage,
      type: 'warning',
      duration: 4000,
      showClose: true
    });

    setTimeout(() => {
      const firstErrorField = document.querySelector('.field-error');
      if (firstErrorField) {
        firstErrorField.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }, 100);
  }

  return isValid;
}

async function showSaveConfirm(): Promise<boolean> {
  const isDraft = article.value.status === 'draft';
  const missingFields: string[] = [];

  if (!article.value.summary?.trim()) {
    missingFields.push('文章摘要');
  }
  if (!article.value.categoryId) {
    missingFields.push('文章分类');
  }
  if (!article.value.coverImage?.trim()) {
    missingFields.push('封面图片');
  }
  if (article.value.tags.length === 0) {
    missingFields.push('文章标签');
  }

  if (missingFields.length > 0 && !isDraft) {
    try {
      await ElMessageBox.confirm(
        `文章还可以进一步完善：\n\n• ${missingFields.join('\n• ')}\n\n建议补充以上信息以提升文章质量和SEO效果。是否继续保存？`,
        '完善文章信息',
        {
          confirmButtonText: '继续保存',
          cancelButtonText: '去完善',
          type: 'info',
          dangerouslyUseHTMLString: true,
          customClass: 'article-save-confirm'
        }
      );
      return true;
    } catch {
      return false;
    }
  }

  return true;
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
  if (!currentSlug.value) {
    loading.value = false;
    return;
  }

  try {
    loadError.value = null;
    const response = await fetch(`${API_BASE_URL}/api/articles/${currentSlug.value}`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });

    if (!response.ok) {
      if (response.status === 404) {
        await checkFileSystemArticle();
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
      categoryId: data.category?.id || null,
      tags: data.tags || [],
      coverImage: data.coverImage || '',
      author: data.authorName || (data.author?.nickname || data.author?.username) || '',
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

    updateWordCount();
  } catch (error) {
    console.error('加载文章失败:', error);
    loadError.value = '无法连接到服务器，请检查网络连接或稍后重试';
  } finally {
    loading.value = false;
  }
}

async function checkFileSystemArticle() {
  try {
    const checkResponse = await fetch(`${API_BASE_URL}/api/admin/articles/sync/check`, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });

    if (checkResponse.ok) {
      const checkResult = await checkResponse.json();
      if (checkResult.code === 200 && checkResult.data?.isSyncNeeded) {
        loadError.value = '此文章尚未同步到数据库。请在文章管理页面点击"同步文件"按钮将文章导入数据库后再编辑。';
        return;
      }
    }

    loadError.value = '文章不存在，可能已被删除或移动';
  } catch (error) {
    console.error('检查文件系统文章失败:', error);
    loadError.value = '文章不存在，可能已被删除或移动';
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

  updateWordCount();
}

function insertBlockFormula() {
  const textarea = contentRef.value;
  if (!textarea) return;

  const start = textarea.selectionStart;
  const text = article.value.content;
  const selected = text.substring(start, textarea.selectionEnd);

  const formula = selected || 'J(\\theta) = \\frac{1}{2m}\\sum_{i=1}^{m}(h_\\theta(x^{(i)}) - y^{(i)})^2';
  const insertion = `\n$$\n${formula}\n$$\n`;

  article.value.content = text.substring(0, start) + insertion + text.substring(textarea.selectionEnd);

  setTimeout(() => {
    textarea.focus();
    const newPos = start + 4;
    textarea.selectionStart = newPos;
    textarea.selectionEnd = newPos + formula.length;
  }, 0);

  updateWordCount();
}

function insertMermaid() {
  const textarea = contentRef.value;
  if (!textarea) return;

  const start = textarea.selectionStart;
  const text = article.value.content;

  const mermaidTemplate = `\n\`\`\`mermaid\nflowchart LR\n    A[开始] --> B[处理]\n    B --> C[结束]\n\`\`\`\n`;

  article.value.content = text.substring(0, start) + mermaidTemplate + text.substring(textarea.selectionEnd);

  setTimeout(() => {
    textarea.focus();
    const newPos = start + 17;
    textarea.selectionStart = newPos;
    textarea.selectionEnd = newPos + 42;
  }, 0);

  updateWordCount();
}

function insertTable() {
  const textarea = contentRef.value;
  if (!textarea) return;

  const start = textarea.selectionStart;
  const text = article.value.content;

  const tableTemplate = `\n| 列1 | 列2 | 列3 |\n|------|------|------|\n| 内容 | 内容 | 内容 |\n`;

  article.value.content = text.substring(0, start) + tableTemplate + text.substring(textarea.selectionEnd);

  setTimeout(() => {
    textarea.focus();
    const newPos = start + 1;
    textarea.selectionStart = newPos;
    textarea.selectionEnd = newPos + 36;
  }, 0);

  updateWordCount();
}

async function saveArticle() {
  if (!validateArticle()) {
    return;
  }

  const shouldContinue = await showSaveConfirm();
  if (!shouldContinue) {
    return;
  }

  isSubmitting.value = true;

  try {
    const payload = { ...article.value };

    let response;
    if (isNew.value) {
      response = await fetch(`${API_BASE_URL}/api/articles`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${getToken()}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
    } else {
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

    const successMessages: Record<string, string> = {
      'draft': '草稿保存成功',
      'published': '文章发布成功',
      'archived': '文章已归档'
    };

    ElMessage({
      message: successMessages[article.value.status] || '文章保存成功',
      type: 'success',
      duration: 3000
    });

    if (isNew.value) {
      window.location.href = `/admin/articles/${result.data.slug}`;
    } else {
      articleInfo.value.updatedAt = result.data.updatedAt;
    }
  } catch (error) {
    console.error('保存文章失败:', error);

    const errorMsg = (error as Error).message;
    let displayMsg = '保存失败';

    if (errorMsg.includes('Duplicate entry') || errorMsg.includes('唯一约束')) {
      displayMsg = '文章URL标识已存在，请更换一个';
      validationErrors.value.slug = '该URL标识已被使用';
    } else if (errorMsg.includes('timeout') || errorMsg.includes('超时')) {
      displayMsg = '保存超时，请检查网络连接后重试';
    } else if (errorMsg.includes('Unauthorized') || errorMsg.includes('401')) {
      displayMsg = '登录已过期，请重新登录';
    } else if (errorMsg) {
      displayMsg = errorMsg;
    }

    ElMessage({
      message: displayMsg,
      type: 'error',
      duration: 5000,
      showClose: true
    });
  } finally {
    isSubmitting.value = false;
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

function getCurrentUserDisplayName(): string {
  const user = getUser();
  if (!user) return '管理员';
  return user.nickname || user.username || '管理员';
}

function canEditAuthor(): boolean {
  const user = getUser();
  if (!user) return false;
  return user.role === 'blogger' || user.role === 'admin';
}

onMounted(async () => {
  await loadCategories();
  await loadArticle();

  if (isNew.value) {
    const user = getUser();
    if (user) {
      if (user.role === 'blogger' || user.role === 'admin') {
        article.value.author = user.nickname || user.username || '';
      }
    }
  }

  updateWordCount();

  if (!(window as any).katex) {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css';
    link.crossOrigin = 'anonymous';
    document.head.appendChild(link);

    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js';
    script.crossOrigin = 'anonymous';
    document.head.appendChild(script);
  }
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

.editor-toolbar {
  display: flex;
  gap: 2px;
  padding: 8px 10px;
  background: var(--admin-input-bg);
  border: 1px solid var(--admin-card-border);
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  flex-wrap: wrap;
  align-items: center;
}

.toolbar-group {
  display: flex;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: var(--admin-card-border);
  margin: 0 6px;
  flex-shrink: 0;
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

.toolbar-btn-text {
  font-size: 11px;
  font-weight: 600;
  font-style: italic;
  font-family: 'Times New Roman', serif;
}

.latex-btn {
  width: auto;
  padding: 0 8px;
}

.latex-btn:hover {
  color: var(--admin-primary);
  background: rgba(255, 107, 157, 0.1);
}

.editor-content-area {
  position: relative;
}

.content-input {
  resize: vertical;
  min-height: 400px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 0 0 8px 8px;
}

.content-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.word-count-info {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--admin-text-muted);
}

.preview-panel {
  min-height: 400px;
  padding: 24px;
  background: var(--admin-input-bg);
  border: 1px solid var(--admin-card-border);
  border-top: none;
  border-radius: 0 0 8px 8px;
  overflow-y: auto;
  max-height: 600px;
  line-height: 1.8;
  font-size: 15px;
  color: var(--admin-text);
}

.preview-panel :deep(h1),
.preview-panel :deep(h2),
.preview-panel :deep(h3) {
  color: var(--admin-text);
  margin: 24px 0 12px;
  font-weight: 600;
}

.preview-panel :deep(h2) {
  font-size: 1.5rem;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--admin-card-border);
}

.preview-panel :deep(h3) {
  font-size: 1.25rem;
}

.preview-panel :deep(pre) {
  background: #1e1e2e;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}

.preview-panel :deep(code) {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
}

.preview-panel :deep(:not(pre) > code) {
  background: rgba(255, 107, 157, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--admin-primary);
}

.preview-panel :deep(pre code) {
  background: transparent;
  color: #cdd6f4;
}

.preview-panel :deep(blockquote) {
  border-left: 4px solid var(--admin-primary);
  padding: 12px 16px;
  margin: 16px 0;
  background: rgba(255, 107, 157, 0.05);
  border-radius: 0 8px 8px 0;
}

.preview-panel :deep(.math-block) {
  margin: 20px 0;
  text-align: center;
  overflow-x: auto;
  padding: 12px 0;
}

.preview-panel :deep(.math-inline) {
  display: inline;
}

.preview-panel :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 16px 0;
}

.preview-panel :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.preview-panel :deep(th),
.preview-panel :deep(td) {
  padding: 8px 12px;
  border: 1px solid var(--admin-card-border);
  text-align: left;
}

.preview-panel :deep(th) {
  background: var(--admin-hover-bg);
  font-weight: 600;
}

.preview-panel :deep(a) {
  color: var(--admin-primary);
  text-decoration: none;
}

.preview-panel :deep(a:hover) {
  text-decoration: underline;
}

.preview-empty {
  color: var(--admin-text-muted);
  text-align: center;
  padding: 60px 20px;
  font-style: italic;
}

.editor-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-panel,
.info-panel,
.format-help-panel {
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

.format-help-panel {
  padding: 20px;
}

.help-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 12px;
}

.help-item code {
  background: var(--admin-input-bg);
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 11px;
  color: var(--admin-primary);
}

.help-item span {
  color: var(--admin-text-muted);
  font-size: 12px;
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

.required-mark {
  color: #ef4444;
  margin-left: 4px;
}

.form-group.has-error .form-label {
  color: #ef4444;
}

.admin-input.input-error {
  border-color: #ef4444;
  background-color: rgba(239, 68, 68, 0.05);
}

.admin-input.input-error:focus {
  border-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.field-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ef4444;
  font-size: 13px;
  margin-top: 6px;
  padding: 6px 10px;
  background: rgba(239, 68, 68, 0.08);
  border-radius: 6px;
  border-left: 3px solid #ef4444;
}

.field-error::before {
  content: '';
  display: inline-block;
  width: 14px;
  height: 14px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%23ef4444' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'/%3E%3Cline x1='12' y1='8' x2='12' y2='12'/%3E%3Cline x1='12' y1='16' x2='12.01' y2='16'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  flex-shrink: 0;
}

.admin-btn.is-loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.admin-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

:global(.article-save-confirm) {
  max-width: 420px;
}

:global(.article-save-confirm .el-message-box__content) {
  padding: 20px;
}

:global(.article-save-confirm .el-message-box__message) {
  line-height: 1.8;
  color: var(--admin-text);
}

.author-field .field-hint {
  font-size: 12px;
  color: var(--admin-text-secondary);
  margin-left: 8px;
}

.author-field .admin-input:disabled {
  background: var(--admin-bg-secondary);
  cursor: not-allowed;
  opacity: 0.7;
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
