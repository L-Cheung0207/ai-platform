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
          <h3 class="news-related-title">相关推荐</h3>
          <ul class="news-related-list">
            <li v-for="(r, i) in related" :key="r.id">
              <router-link
                :to="'/news/' + r.id"
                class="news-related-link"
                :style="{ animationDelay: `${i * 50}ms` }"
              >
                {{ r.title }}
              </router-link>
            </li>
          </ul>
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
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 1px solid #e2e8f0;
  animation: fade-up 0.5s ease-out 0.15s both;
}

.news-related-title {
  font-size: 1rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 1rem;
}

.news-related-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.news-related-link {
  display: block;
  padding: 0.65rem 0;
  font-size: 0.9375rem;
  color: #475569;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
  animation: fade-up 0.4s ease-out both;
}

.news-related-link:hover {
  color: var(--color-primary, #2563eb);
  background: rgba(37, 99, 235, 0.04);
  padding-left: 0.5rem;
  padding-right: 0.5rem;
  margin-left: -0.5rem;
  margin-right: -0.5rem;
}

@media (prefers-reduced-motion: reduce) {
  .news-article,
  .news-related,
  .news-related-link {
    animation: none;
  }
}
</style>
