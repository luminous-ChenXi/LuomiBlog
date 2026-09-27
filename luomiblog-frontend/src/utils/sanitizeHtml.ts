import DOMPurify from 'isomorphic-dompurify';

/**
 * 文章 HTML 白名单消毒（存储型 XSS 防线）。
 *
 * 所有进入 set:html / v-html 的用户文章内容（后端返回的 contentHtml、
 * renderMarkdown / renderMarkdownBasic 的输出）必须先经过本函数。
 *
 * 白名单覆盖现有渲染特性：
 * - 标题 h1-h6 / 段落 p / 换行 br / 分隔线 hr
 * - 强调 strong / em / del
 * - 列表 ul / ol / li，任务列表 input[type=checkbox]
 * - 表格 table / thead / tbody / tfoot / tr / th / td
 * - 代码块 pre / code（class="language-*" 供 shiki/highlight 与 mermaid 引导使用）
 * - 数学公式 span.math-inline / div.math-block（KaTeX 输出 class="katex*" 同样放行）
 * - 链接 a（仅 http/https/mailto 等安全协议；target=_blank 自动补 rel=noopener）
 * - 图片 img（仅 http/https 与站内相对路径；javascript:/data: 一律拒绝）
 * - 折叠块 details / summary，上下标 sup / sub，引用 blockquote
 * - mermaid 容器 div.mermaid-container
 *
 * 超出白名单的标签（script/iframe/style/svg/事件属性等）一律剥除。
 */

const ALLOWED_TAGS = [
  // 文本与结构
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'p', 'br', 'hr',
  'strong', 'em', 'del', 's',
  'sup', 'sub', 'span',
  'blockquote',
  'ul', 'ol', 'li',
  'details', 'summary',
  // 表格
  'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td',
  // 代码
  'pre', 'code',
  // 媒体与链接
  'a', 'img',
  // 任务列表
  'input',
  // 容器（math-block / mermaid-container 等）
  'div',
];

const ALLOWED_ATTR = [
  'class', 'id',
  'href', 'src', 'alt', 'title',
  'target', 'rel',
  'type', 'checked', 'disabled',
  'align', 'colspan', 'rowspan',
];

// 与 DOMPurify 默认一致的 URI 协议白名单（显式排除 data:/javascript:/vbscript: 等）
const ALLOWED_URI_REGEXP = /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|sms):|[^a-z]|[a-z+.-]+(?:[^a-z+.-:]|$))/i;

let hookInstalled = false;

function installHookOnce(): void {
  if (hookInstalled) return;
  hookInstalled = true;
  // target="_blank" 的链接强制补 rel="noopener noreferrer"，防 reverse tabnabbing
  DOMPurify.addHook('afterSanitizeAttributes', (node) => {
    if (node.tagName === 'A' && node.getAttribute('target') === '_blank') {
      node.setAttribute('rel', 'noopener noreferrer');
    }
  });
  // 强制剥离 data: 协议的 src/href（DOMPurify 默认对 img 等媒体标签放行 data:，
  // 本站文章图片统一走 http(s)/站内路径，这里按最严口径拒绝）
  DOMPurify.addHook('uponSanitizeAttribute', (_node, data) => {
    if ((data.attrName === 'src' || data.attrName === 'href')
        && typeof data.attrValue === 'string'
        && /^\s*data:/i.test(data.attrValue)) {
      data.keepAttr = false;
    }
  });
}

/**
 * 按文章白名单消毒任意 HTML 字符串。输入不可信（后端 contentHtml 或
 * 正则渲染器输出），输出保证不含可执行脚本。
 */
export function sanitizeArticleHtml(html: string): string {
  if (!html) return '';
  installHookOnce();
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOWED_URI_REGEXP,
    ALLOW_DATA_ATTR: false,
    ALLOW_UNKNOWN_PROTOCOLS: false,
    KEEP_CONTENT: true,
  });
}
