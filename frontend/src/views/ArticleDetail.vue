<template>
  <div class="pt-8">
    <div class="max-w-[1400px] mx-auto px-6 py-8">
    <template v-if="article">
      <h1 class="text-2xl font-bold text-gray-800 mb-2">{{ article.title }}</h1>
      <div
        :class="['article-body', 'mt-6', article.contentType === 'MARKDOWN' && 'article-body--markdown']"
        v-html="renderedContent"
      />
    </template>
    <template v-else>
      <p v-if="loading" class="text-gray-500">加载中…</p>
      <el-alert v-else type="error" :title="error || '未找到'" show-icon />
    </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import api from '../services/api'

const route = useRoute()
const article = ref(null)
const loading = ref(true)
const error = ref('')

/** 根据后端返回的 contentType 决定用 Markdown 渲染还是富文本 HTML */
const renderedContent = computed(() => {
  const raw = article.value?.content
  if (raw == null || raw === '') return ''
  const isMarkdown = article.value?.contentType === 'MARKDOWN'
  if (isMarkdown) {
    try {
      const html = marked.parse(raw, { async: false, gfm: true })
      return DOMPurify.sanitize(html, { ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'a', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'pre', 'code', 'blockquote', 'hr', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td'] })
    } catch (_) {
      return DOMPurify.sanitize(raw.replace(/</g, '&lt;').replace(/>/g, '&gt;'))
    }
  }
  return DOMPurify.sanitize(raw)
})

onMounted(async () => {
  try {
    article.value = await api.get('/articles/' + route.params.id)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
@reference "../assets/styles.css";

/* 正文基础：适用于富文本与 Markdown */
.article-body {
  font-size: 15px;
  line-height: 1.75;
  color: var(--text-primary, #1f2937);
}

.article-body :deep(p) {
  margin: 0.85em 0;
  line-height: 1.75;
}

.article-body :deep(a) {
  color: var(--el-color-primary, #2563eb);
  text-decoration: none;
}
.article-body :deep(a:hover) {
  text-decoration: underline;
}

.article-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-md, 8px);
  display: block;
}

.article-body :deep(ul),
.article-body :deep(ol) {
  margin: 0.85em 0;
  padding-left: 1.6em;
}
.article-body :deep(ul) { list-style-type: disc; }
.article-body :deep(ol) { list-style-type: decimal; }
.article-body :deep(li) {
  margin: 0.35em 0;
  line-height: 1.7;
}

.article-body :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 0 0.5em 1em;
  border-left: 4px solid var(--border-color, #e5e7eb);
  color: var(--text-secondary, #6b7280);
  font-style: italic;
}

.article-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.2em 0;
  font-size: 14px;
}
.article-body :deep(th),
.article-body :deep(td) {
  border: 1px solid var(--border-color, #e5e7eb);
  padding: 0.5em 0.75em;
  text-align: left;
}
.article-body :deep(th) {
  background: #f8fafc;
  font-weight: 600;
  color: var(--text-primary, #1f2937);
}

.article-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color, #e5e7eb);
  margin: 1.5em 0;
}

/* 富文本与 Markdown 通用标题 */
.article-body :deep(h1) { font-size: 1.5em; font-weight: 600; margin: 1.25em 0 0.5em; color: var(--text-primary); }
.article-body :deep(h2) { font-size: 1.25em; font-weight: 600; margin: 1.35em 0 0.5em; color: var(--text-primary); }
.article-body :deep(h3) { font-size: 1.1em; font-weight: 600; margin: 1.2em 0 0.4em; color: var(--text-primary); }
.article-body :deep(h4), .article-body :deep(h5), .article-body :deep(h6) { font-size: 1em; font-weight: 600; margin: 1em 0 0.35em; color: var(--text-primary); }

/* Markdown 专用：更清晰的层级与代码块 */
.article-body--markdown :deep(h1) { font-size: 1.55em; margin: 1.5em 0 0.6em; }
.article-body--markdown :deep(h2) {
  font-size: 1.3em;
  margin: 1.5em 0 0.6em;
  padding-bottom: 0.35em;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}
.article-body--markdown :deep(h3) { font-size: 1.15em; margin: 1.35em 0 0.5em; }

.article-body--markdown :deep(code) {
  background: #f1f5f9;
  color: #475569;
  padding: 0.2em 0.45em;
  border-radius: 6px;
  font-size: 0.9em;
  font-family: ui-monospace, "SF Mono", Monaco, "Cascadia Mono", monospace;
}

.article-body--markdown :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 1em 1.25em;
  border-radius: var(--radius-md, 8px);
  overflow-x: auto;
  margin: 1.2em 0;
  font-size: 0.9em;
  line-height: 1.6;
}
.article-body--markdown :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
  font-size: inherit;
}
.article-body--markdown :deep(blockquote) {
  border-left-color: #cbd5e1;
  color: #64748b;
}
</style>
