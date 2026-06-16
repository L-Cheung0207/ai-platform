<template>
  <div class="article-list">
    <PageHero
      variant="amber"
      title="AI 知识库"
      subtitle="探索 AI 相关知识与实践，沉淀可复用的学习笔记"
    >
      <div class="article-hero-search global-search">
        <el-input
          v-model="keyword"
          placeholder="在当前页搜索知识标题"
          clearable
          size="large"
          class="article-search-input"
        >
          <template #prefix>
            <svg class="article-search-icon" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M9 3.5a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11ZM2 9a7 7 0 1 1 12.452 4.391l3.328 3.329a.75.75 0 1 1-1.06 1.06l-3.329-3.328A7 7 0 0 1 2 9Z" clip-rule="evenodd" />
            </svg>
          </template>
        </el-input>
      </div>
    </PageHero>

    <div class="article-content">
      <div v-if="!loading && !error && total > 0" class="article-stats">
        <span class="article-stats__count">{{ total }}</span>
        <span class="article-stats__label">篇知识文章</span>
        <span v-if="keyword.trim()" class="article-stats__filter">· 筛选「{{ keyword.trim() }}」</span>
      </div>

      <div v-if="loading" class="article-grid" aria-busy="true">
        <div v-for="i in 6" :key="i" class="article-card article-card--skeleton">
          <div class="skeleton-line skeleton-line--sm" />
          <div class="skeleton-line skeleton-line--lg" />
          <div class="skeleton-line skeleton-line--md" />
          <div class="skeleton-line skeleton-line--full" />
        </div>
      </div>

      <template v-else>
        <div v-if="displayItems.length > 0" class="article-grid">
          <router-link
            v-for="(a, i) in displayItems"
            :key="a.id"
            :to="'/articles/' + a.id"
            class="article-card"
            :style="{ animationDelay: `${i * 45}ms` }"
          >
            <div class="article-card__icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25" />
              </svg>
            </div>
            <div class="article-card__body">
              <div class="article-card__meta">
                <span class="article-type-badge" :class="'article-type-badge--' + (a.contentType || 'RICH_TEXT').toLowerCase()">
                  {{ contentTypeLabel(a.contentType) }}
                </span>
                <time :datetime="a.updatedAt">{{ formatDate(a.updatedAt) }}</time>
              </div>
              <h3 class="article-card__title">{{ a.title }}</h3>
              <p v-if="excerpt(a)" class="article-card__excerpt">{{ excerpt(a) }}</p>
              <span class="article-card__cta">
                阅读全文
                <svg viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                  <path fill-rule="evenodd" d="M3 10a.75.75 0 0 1 .75-.75h10.638L10.23 5.29a.75.75 0 1 1 1.04-1.08l5.5 5.25a.75.75 0 0 1 0 1.08l-5.5 5.25a.75.75 0 1 1-1.04-1.08l4.158-3.96H3.75A.75.75 0 0 1 3 10Z" clip-rule="evenodd" />
                </svg>
              </span>
            </div>
          </router-link>
        </div>
      </template>

      <div v-if="total > 0 && !loading && !keyword.trim()" class="article-pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>

      <p v-if="loading" class="article-status">加载中…</p>
      <el-alert v-if="error" type="error" :title="error" show-icon class="article-alert" />
      <div v-if="!loading && !error && displayItems.length === 0" class="article-empty">
        <div class="article-empty__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.25">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
          </svg>
        </div>
        <p>{{ keyword.trim() ? '未找到匹配的知识' : '暂无知识文章' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const loading = ref(false)
const error = ref('')
const keyword = ref('')

const displayItems = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((a) => (a.title || '').toLowerCase().includes(q))
})

function contentTypeLabel(type) {
  return type === 'MARKDOWN' ? 'Markdown' : '富文本'
}

function formatDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function stripHtml(html) {
  if (!html) return ''
  return html
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/\s+/g, ' ')
    .trim()
}

function excerpt(article, maxLen = 100) {
  const raw = article.content || ''
  if (!raw) return ''
  const text = article.contentType === 'MARKDOWN'
    ? raw.replace(/```[\s\S]*?```/g, ' ').replace(/[#>*`\[\]|_-]/g, ' ').replace(/\s+/g, ' ').trim()
    : stripHtml(raw)
  if (!text) return ''
  return text.length <= maxLen ? text : text.slice(0, maxLen) + '…'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    const data = await api.get('/articles', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.article-list {
  --article-accent: #d97706;
  --article-accent-hover: #b45309;
  --article-accent-soft: #fffbeb;
  --article-accent-muted: #fef3c7;
  --article-accent-border: rgba(251, 191, 36, 0.5);
  --article-accent-border-hover: rgba(217, 119, 6, 0.45);
  --article-accent-shadow: rgba(217, 119, 6, 0.1);
  --article-accent-icon: #fbbf24;

  min-height: 100vh;
  background: linear-gradient(180deg, #fffdf7 0%, #fffbeb 35%, #f8fafc 100%);
}

.article-hero-search {
  max-width: 520px;
  margin: 0.5rem auto 0;
}

.article-hero-search.global-search {
  padding-right: 1rem;
}

.article-search-icon {
  width: 1rem;
  height: 1rem;
  color: #94a3b8;
}

.article-content {
  max-width: 1080px;
  margin: 0 auto;
  padding: 2rem 1.5rem 3rem;
}

.article-stats {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  margin-bottom: 1.25rem;
  font-size: 0.875rem;
  color: #64748b;
}

.article-stats__count {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--article-accent);
  letter-spacing: -0.02em;
}

.article-stats__label {
  font-weight: 500;
}

.article-stats__filter {
  color: #94a3b8;
}

.article-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.875rem;
}

@media (min-width: 768px) {
  .article-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }
}

.article-card {
  display: flex;
  gap: 1rem;
  padding: 1.25rem 1.35rem;
  background: #fff;
  border-radius: 14px;
  border: 1px solid var(--article-accent-border);
  box-shadow: 0 1px 3px rgba(217, 119, 6, 0.05);
  text-decoration: none;
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
  animation: article-fade-up 0.45s ease-out both;
}

.article-card:hover {
  border-color: var(--article-accent-border-hover);
  box-shadow: 0 8px 24px var(--article-accent-shadow);
  transform: translateY(-2px);
}

.article-card__icon {
  flex-shrink: 0;
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--article-accent-soft) 0%, var(--article-accent-muted) 100%);
  color: var(--article-accent);
}

.article-card__icon svg {
  width: 1.25rem;
  height: 1.25rem;
}

.article-card__body {
  flex: 1;
  min-width: 0;
}

.article-card__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
  font-size: 0.75rem;
  color: #94a3b8;
}

.article-type-badge {
  display: inline-block;
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.article-type-badge--markdown {
  background: #f0fdf4;
  color: #15803d;
}

.article-type-badge--rich_text {
  background: #eff6ff;
  color: #1d4ed8;
}

.article-card__title {
  font-size: 1rem;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.45;
  margin: 0 0 0.35rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s;
}

.article-card:hover .article-card__title {
  color: var(--article-accent);
}

.article-card__excerpt {
  font-size: 0.8125rem;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 0.65rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card__cta {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--article-accent-hover);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s, transform 0.2s;
}

.article-card__cta svg {
  width: 0.875rem;
  height: 0.875rem;
}

.article-card:hover .article-card__cta {
  opacity: 1;
  transform: translateX(0);
}

.article-card--skeleton {
  pointer-events: none;
  animation: none;
}

.skeleton-line {
  height: 0.75rem;
  border-radius: 4px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.2s ease-in-out infinite;
}

.skeleton-line--sm { width: 4rem; margin-bottom: 0.75rem; }
.skeleton-line--lg { width: 75%; height: 1rem; margin-bottom: 0.5rem; }
.skeleton-line--md { width: 50%; margin-bottom: 0.75rem; }
.skeleton-line--full { width: 100%; }

.article-pagination {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

.article-status,
.article-empty p {
  text-align: center;
  color: #64748b;
  margin: 0;
}

.article-status {
  padding: 3rem 0;
}

.article-empty {
  padding: 4rem 0;
  text-align: center;
}

.article-empty__icon {
  width: 3.5rem;
  height: 3.5rem;
  margin: 0 auto 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--article-accent-soft);
  color: var(--article-accent-icon);
}

.article-empty__icon svg {
  width: 1.75rem;
  height: 1.75rem;
}

.article-alert {
  margin-top: 1rem;
}

@keyframes article-fade-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .article-card {
    animation: none;
  }
  .article-card:hover {
    transform: none;
  }
  .article-card__cta {
    opacity: 1;
    transform: none;
  }
  .skeleton-line {
    animation: none;
  }
}
</style>
