<template>
  <div class="home-page">
    <header class="home-hero">
      <div class="home-hero-inner">
        <h1 class="home-title"><span class="home-title-gradient">探索 AI，</span>从这里开始</h1>
        <p class="home-subtitle">分享 Skill、Rule、AI知识库与最新的AI资讯</p>
      </div>
    </header>

    <main class="home-main max-w-[1400px] mx-auto px-6 py-10">
      <div class="home-layout">
        <div class="home-main-col">
          <section v-if="latestAiTools?.length" class="home-section home-section-anim">
            <div class="home-section-header">
              <h2 class="home-section-title">
                <Icons name="sparkle" :size="20" class="text-primary" />
                AI 工具
              </h2>
              <router-link to="/ai-tools" class="home-more">更多</router-link>
            </div>
            <div class="home-grid-2">
              <router-link
                v-for="(t, i) in latestAiTools"
                :key="t.id"
                :to="'/ai-tools/' + t.id"
                class="home-card home-card-aitool"
                :style="{ animationDelay: `${i * 40}ms` }"
              >
                <img
                  v-if="t.logoUrl"
                  :src="t.logoUrl"
                  :alt="t.name"
                  class="home-card-aitool-logo"
                />
                <div v-else class="home-card-aitool-logo home-card-aitool-logo-placeholder">
                  {{ (t.name || '?')[0] }}
                </div>
                <div class="home-card-body">
                  <h3 class="home-card-title">{{ t.name }}</h3>
                  <p class="home-card-desc">{{ t.summary || t.description || '暂无描述' }}</p>
                </div>
              </router-link>
            </div>
          </section>

          <section v-if="latestArticles?.length" class="home-section home-section-anim">
            <div class="home-section-header">
              <h2 class="home-section-title">
                <Icons name="book" :size="20" class="text-primary" />
                最新知识
              </h2>
              <router-link to="/articles" class="home-more">更多</router-link>
            </div>
            <ul class="home-list">
              <li v-for="(a, i) in latestArticles" :key="a.id" class="home-list-item" :style="{ animationDelay: `${i * 35}ms` }">
                <router-link :to="'/articles/' + a.id" class="home-list-link">{{ a.title }}</router-link>
              </li>
            </ul>
          </section>
        </div>

        <aside v-if="latestNews?.length || latestLlmLeaderboard?.length" class="home-sidebar">
          <section v-if="latestLlmLeaderboard?.length" class="home-section home-section-news home-section-anim">
            <div class="home-section-header">
              <h2 class="home-section-title">
                <Icons name="sparkle" :size="20" class="text-primary" />
                最新编程模型排行榜
              </h2>
              <router-link to="/llm-leaderboard" class="home-more">更多</router-link>
            </div>
            <ul class="home-news-list">
              <li v-for="(m, i) in latestLlmLeaderboard" :key="m.id" class="home-news-item" :style="{ animationDelay: `${i * 35}ms` }">
                <router-link to="/llm-leaderboard" class="home-news-link">
                  <span class="home-news-num" :class="i < 3 ? 'home-news-num-hot' : 'home-news-num-normal'">{{ i + 1 }}</span>
                  <span class="home-news-title">{{ m.rankBadge }}{{ m.modelName }}</span>
                </router-link>
              </li>
            </ul>
          </section>
          <section v-if="latestNews?.length" class="home-section home-section-news home-section-anim">
            <div class="home-section-header">
              <h2 class="home-section-title">
                <Icons name="sparkle" :size="20" class="text-primary" />
                AI 资讯
              </h2>
              <router-link to="/news" class="home-more">更多</router-link>
            </div>
            <ul class="home-news-list">
              <li v-for="(n, i) in latestNews" :key="n.id" class="home-news-item" :style="{ animationDelay: `${i * 35}ms` }">
                <router-link :to="'/news/' + n.id" class="home-news-link">
                  <span class="home-news-num" :class="i < 3 ? 'home-news-num-hot' : 'home-news-num-normal'">{{ i + 1 }}</span>
                  <span class="home-news-title">{{ n.title }}</span>
                </router-link>
              </li>
            </ul>
          </section>
        </aside>
      </div>

      <p v-if="loading" class="home-loading">加载中…</p>
      <el-alert v-if="error" type="error" :title="error" show-icon class="home-alert" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Icons from '../components/Icons.vue'
import api from '../services/api'

const latestAiTools = ref([])
const latestArticles = ref([])
const latestNews = ref([])
const latestLlmLeaderboard = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const data = await api.get('/home')
    latestAiTools.value = data.latestAiTools || []
    latestArticles.value = data.latestArticles || []
    latestNews.value = (data.latestNews || []).slice(0, 10)
    latestLlmLeaderboard.value = data.latestLlmLeaderboard || []
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fafbfc 0%, #f1f3f6 100%);
}

.home-hero {
  position: relative;
  padding: 3.5rem 1.5rem 4rem;
  overflow: hidden;
  /* 多层背景：渐变基底 +  subtle grid */
  background: 
    linear-gradient(rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(165deg, #f0f7ff 0%, #e8f4f8 40%, #f5f9fc 100%);
  background-size: 28px 28px, 28px 28px, 100% 100%;
  background-position: 0 0, 0 0, 0 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}
/* 右上角渐变光斑 */
.home-hero::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -20%;
  width: 70%;
  height: 120%;
  background: radial-gradient(ellipse at center, rgba(46, 201, 215, 0.12) 0%, transparent 70%);
  pointer-events: none;
}
/* 左下角渐变光斑 */
.home-hero::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -15%;
  width: 50%;
  height: 100%;
  background: radial-gradient(ellipse at center, rgba(46, 104, 184, 0.08) 0%, transparent 65%);
  pointer-events: none;
}

.home-hero-inner {
  position: relative;
  z-index: 1;
  max-width: 640px;
  margin: 0 auto;
  text-align: center;
}

.home-title {
  font-size: 3.5rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin-bottom: 0.5rem;
}

.home-title-gradient {
  background: linear-gradient(90deg, #2E68B8 0%, #2EC9D7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.home-subtitle {
  font-size: 0.9375rem;
  color: #64748b;
  margin-bottom: 2rem;
}

.home-layout {
  display: flex;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 1023px) {
  .home-layout {
    flex-direction: column;
  }
  .home-sidebar {
    order: -1;
  }
}

.home-main-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.home-sidebar {
  flex-shrink: 0;
  width: 320px;
  position: sticky;
  top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

@media (max-width: 1023px) {
  .home-sidebar {
    width: 100%;
  }
}

.home-section {
  background: #fff;
  border-radius: 16px;
  padding: 1.5rem 1.75rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.06);
  animation: home-fade-up 0.5s ease-out both;
}

.home-section-anim:nth-child(1) { animation-delay: 0.05s; }
.home-section-anim:nth-child(2) { animation-delay: 0.1s; }
.home-section-anim:nth-child(3) { animation-delay: 0.15s; }
.home-section-anim:nth-child(4) { animation-delay: 0.2s; }
.home-section-anim:nth-child(5) { animation-delay: 0.25s; }

.home-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}

.home-section-title {
  font-size: 1.0625rem;
  font-weight: 600;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.home-more {
  font-size: 0.875rem;
  color: var(--color-primary, #2563eb);
  transition: opacity 0.2s;
}

.home-more:hover {
  opacity: 0.85;
  text-decoration: underline;
}

.home-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

@media (max-width: 639px) {
  .home-grid-2 {
    grid-template-columns: 1fr;
  }
}

.home-card {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #fff;
  transition: border-color 0.25s, box-shadow 0.25s, transform 0.25s;
  animation: home-fade-up 0.45s ease-out both;
}

.home-card-no-icon .home-card-body {
  padding: 1.25rem 1.5rem;
}

.home-card-aitool {
  flex-direction: row;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 1.25rem;
}

.home-card-aitool .home-card-body {
  flex: 1;
  min-width: 0;
  padding: 0;
}

.home-card-aitool-logo {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  object-fit: contain;
  border-radius: 12px;
  background: #f9fafb;
}

.home-card-aitool-logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  color: #9ca3af;
  background: #f3f4f6;
}

.home-card:hover {
  border-color: rgba(37, 99, 235, 0.4);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.12);
  transform: translateY(-2px);
}

.home-card-icon {
  height: 120px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.home-card-icon-rule {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.home-card-body {
  padding: 1rem 1.25rem;
}

.home-card-title {
  font-size: 1rem;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-card-desc {
  font-size: 0.8125rem;
  color: #64748b;
  line-height: 1.5;
  margin-top: 0.375rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-card-tag {
  margin-top: 0.75rem;
}

.home-list {
  list-style: none;
  padding: 0;
  margin: 0;
  border-top: 1px solid #f1f5f9;
}

.home-list-item {
  padding: 0.75rem 0;
  border-bottom: 1px solid #f1f5f9;
  animation: home-fade-up 0.4s ease-out both;
}

.home-list-item:last-child {
  border-bottom: none;
}

.home-list-link {
  font-size: 0.9375rem;
  color: #334155;
  transition: color 0.2s;
}

.home-list-link:hover {
  color: var(--color-primary, #2563eb);
}

/* 右侧 AI 资讯 */
.home-section-news {
  min-width: 0;
}

.home-news-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.home-news-item {
  animation: home-fade-up 0.4s ease-out both;
}

.home-news-link {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.6rem 0;
  font-size: 0.875rem;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  transition: color 0.2s;
}

.home-news-item:last-child .home-news-link {
  border-bottom: none;
}

.home-news-link:hover {
  color: var(--color-primary, #2563eb);
}

.home-news-link:hover .home-news-num-hot {
  color: #d87070;
  background: rgba(235, 136, 136, 0.25);
}

.home-news-link:hover .home-news-num-normal {
  color: #d4a830;
  background: rgba(235, 192, 80, 0.25);
}

.home-news-num {
  flex-shrink: 0;
  width: 1.25rem;
  height: 1.25rem;
  line-height: 1.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 4px;
  text-align: center;
  transition: color 0.2s, background 0.2s;
}

.home-news-num-hot {
  color: #EB8888;
  background: rgba(235, 136, 136, 0.15);
}

.home-news-num-normal {
  color: #EBC050;
  background: rgba(235, 192, 80, 0.15);
}

.home-news-title {
  flex: 1;
  min-width: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-loading {
  text-align: center;
  color: #64748b;
  padding: 3rem 0;
}

.home-alert {
  margin-top: 1rem;
}

@keyframes home-fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-section,
  .home-card,
  .home-list-item {
    animation: none !important;
  }
  .home-card:hover {
    transform: none;
  }
}
</style>
