<template>
  <div class="news-detail pt-8">
    <div class="max-w-[1400px] mx-auto px-6 py-10">
      <template v-if="news">
        <!-- 主内容卡片：编辑风格 -->
        <article class="news-article">
          <p class="news-label">正文</p>
          <h1 class="news-title">{{ news.title }}</h1>
          <div v-if="news.publishDate" class="news-meta">
            <span>发布日期 {{ news.publishDate }}</span>
          </div>
          <div v-if="news.summary" class="news-body">
            <p v-for="(para, i) in paragraphs" :key="i" class="news-para">{{ para }}</p>
          </div>
        </article>

        <!-- 相关推荐 -->
        <aside v-if="related.length > 0" class="news-related">
          <div class="news-related-header">
            <span class="news-related-label">更多阅读</span>
            <h3 class="news-related-title">相关推荐</h3>
          </div>
          <div class="news-related-grid">
            <router-link
              v-for="(r, i) in related"
              :key="r.id"
              :to="'/news/' + r.id"
              class="news-related-card"
              :style="{ animationDelay: `${i * 60}ms` }"
            >
              <span class="news-related-card-index">{{ String(i + 1).padStart(2, '0') }}</span>
              <div class="news-related-card-body">
                <h4 class="news-related-card-title">{{ r.title }}</h4>
                <p v-if="r.publishDate" class="news-related-card-meta">{{ r.publishDate }}</p>
                <p v-else-if="(r.summary || r.sourceName)" class="news-related-card-desc">{{ (r.summary || r.sourceName || '').trim().slice(0, 72) }}{{ (r.summary || r.sourceName || '').trim().length > 72 ? '…' : '' }}</p>
              </div>
              <span class="news-related-card-arrow" aria-hidden="true">→</span>
            </router-link>
          </div>
        </aside>
      </template>

      <template v-else>
        <p v-if="loading" class="text-gray-500 py-16 text-center">加载中…</p>
        <el-alert v-else type="error" :title="error || '未找到'" show-icon />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'

const route = useRoute()
const news = ref(null)
const related = ref([])
const loading = ref(true)
const error = ref('')

const paragraphs = computed(() => {
  const s = news.value?.summary
  if (!s) return []
  return s.split(/\n\n+/).filter(Boolean)
})

onMounted(async () => {
  try {
    news.value = await api.get('/news/' + route.params.id)
    const data = await api.get('/news', { params: { page: 1, size: 6 } })
    const items = data.items || []
    related.value = items.filter(n => n.id !== news.value?.id).slice(0, 5)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.news-detail {
  min-height: 100vh;
  background: linear-gradient(180deg, #fafbfc 0%, #f1f3f6 100%);
}

/* 主文章区 */
.news-article {
  background: #fff;
  border-radius: 16px;
  padding: 2.5rem 2.25rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.06);
  animation: fade-up 0.5s ease-out forwards;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.news-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #94a3b8;
  margin-bottom: 1rem;
}

.news-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  letter-spacing: -0.02em;
  margin-bottom: 1.25rem;
}

@media (min-width: 640px) {
  .news-title {
    font-size: 2rem;
  }
}

.news-meta {
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 2rem;
  padding-bottom: 1.75rem;
  border-bottom: 1px solid #e2e8f0;
}

.news-body {
  margin-top: 0;
}

.news-para {
  font-size: 1rem;
  line-height: 1.75;
  color: #334155;
  margin-bottom: 1.25rem;
}

.news-para:last-child {
  margin-bottom: 0;
}

/* 相关推荐 */
.news-related {
  margin-top: 3.5rem;
  padding-top: 2.5rem;
  border-top: 1px solid #e2e8f0;
  animation: fade-up 0.5s ease-out 0.15s both;
}

.news-related-header {
  margin-bottom: 1.5rem;
}

.news-related-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #94a3b8;
  margin-bottom: 0.35rem;
}

.news-related-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin: 0;
}

.news-related-grid {
  display: grid;
  gap: 0.75rem;
}

.news-related-card {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s ease, box-shadow 0.25s ease, transform 0.2s ease;
  animation: fade-up 0.4s ease-out both;
  cursor: pointer;
}

.news-related-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.news-related-card:focus-visible {
  outline: 2px solid var(--color-primary, #2563eb);
  outline-offset: 2px;
}

.news-related-card-index {
  flex-shrink: 0;
  font-size: 0.75rem;
  font-weight: 700;
  color: #cbd5e1;
  letter-spacing: 0.02em;
  line-height: 1.2;
}

.news-related-card:hover .news-related-card-index {
  color: var(--color-primary, #2563eb);
}

.news-related-card-body {
  flex: 1;
  min-width: 0;
}

.news-related-card-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.4;
  margin: 0 0 0.25rem 0;
  transition: color 0.2s ease;
}

.news-related-card:hover .news-related-card-title {
  color: var(--color-primary, #2563eb);
}

.news-related-card-meta,
.news-related-card-desc {
  font-size: 0.8125rem;
  color: #64748b;
  line-height: 1.45;
  margin: 0;
}

.news-related-card-arrow {
  flex-shrink: 0;
  font-size: 1rem;
  color: #cbd5e1;
  transition: color 0.2s ease, transform 0.2s ease;
}

.news-related-card:hover .news-related-card-arrow {
  color: var(--color-primary, #2563eb);
  transform: translateX(3px);
}

@media (prefers-reduced-motion: reduce) {
  .news-article,
  .news-related,
  .news-related-card {
    animation: none;
  }
  .news-related-card:hover {
    transform: none;
  }
  .news-related-card:hover .news-related-card-arrow {
    transform: none;
  }
}
</style>
