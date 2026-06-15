<template>
  <div class="home-page">
    <header class="home-hero">
      <div class="home-hero-inner">
        <p class="home-eyebrow">AI原生实践中心 · AI Native Hub</p>
        <h1 class="home-title">让 <span class="home-title-gradient">AI Native</span> 成为公司的默认工作方式</h1>
      </div>
    </header>

    <main class="home-main max-w-[1400px] mx-auto px-6 py-10">
      <div class="home-layout">
        <div class="home-main-col">
          <section v-if="latestSkills?.length" class="home-section home-section--skills home-section-anim">
            <div class="home-section-header">
              <div>
                <p class="home-section-kicker">Asset Library</p>
                <h2 class="home-section-title">
                  <span class="home-section-icon">
                    <Icons name="sparkle" :size="20" />
                  </span>
                  最新 Skill 资产
                </h2>
              </div>
              <router-link to="/skills" class="home-more">更多</router-link>
            </div>
            <div class="home-grid-2">
              <router-link
                v-for="(s, i) in latestSkills"
                :key="s.id"
                :to="'/skills/' + s.id"
                class="home-card home-card-skill"
                :style="{ animationDelay: `${i * 40}ms` }"
              >
                <div class="home-card-skill-marker">{{ assetLevelShort(s.assetLevel) }}</div>
                <div class="home-card-body">
                  <div class="home-card-title-row">
                    <h3 class="home-card-title">{{ s.name }}</h3>
                    <span class="home-card-status">{{ lifecycleLabel(s.lifecycleStatus) }}</span>
                  </div>
                  <p class="home-card-desc">{{ s.description || '暂无描述' }}</p>
                  <div class="home-card-meta">
                    <span>{{ s.teamName || '未分配团队' }}</span>
                    <span>{{ s.skillCategory || '通用能力' }}</span>
                  </div>
                </div>
              </router-link>
            </div>
          </section>

          <section v-if="hasGithubTrending" class="home-section home-section--github home-section-anim">
            <div class="home-section-header home-trending-header">
              <div>
                <p class="home-section-kicker">Open Source</p>
                <h2 class="home-section-title">
                  <span class="home-section-icon">
                    <Icons name="link" :size="20" />
                  </span>
                  GitHub Trending
                </h2>
                <p class="home-trending-time">{{ formattedGithubTrendingUpdatedAt }}</p>
              </div>
              <div class="home-trending-tabs" role="tablist" aria-label="GitHub Trending 周期">
                <button
                  type="button"
                  class="home-trending-tab"
                  :class="{ 'home-trending-tab--active': githubTrendingPeriod === 'weekly' }"
                  role="tab"
                  :aria-selected="githubTrendingPeriod === 'weekly'"
                  @click="githubTrendingPeriod = 'weekly'"
                >
                  周榜
                </button>
                <button
                  type="button"
                  class="home-trending-tab"
                  :class="{ 'home-trending-tab--active': githubTrendingPeriod === 'monthly' }"
                  role="tab"
                  :aria-selected="githubTrendingPeriod === 'monthly'"
                  @click="githubTrendingPeriod = 'monthly'"
                >
                  月榜
                </button>
              </div>
            </div>
            <div class="home-trending-grid">
              <a
                v-for="(repo, i) in currentGithubTrending"
                :key="`${repo.period || githubTrendingPeriod}-${repo.repoFullName}`"
                :href="repo.repoUrl || `https://github.com/${repo.repoFullName}`"
                target="_blank"
                rel="noopener"
                class="home-trending-card"
                :style="{ animationDelay: `${i * 35}ms` }"
              >
                <span class="home-trending-rank" :class="repo.rank <= 3 ? 'home-trending-rank--hot' : 'home-trending-rank--normal'">
                  {{ repo.rank }}
                </span>
                <span class="home-trending-copy">
                  <span class="home-trending-repo">{{ repo.repoFullName }}</span>
                  <span v-if="repo.effectCn" class="home-trending-effect">{{ repo.effectCn }}</span>
                  <span v-if="repo.scenarioCn" class="home-trending-scenario">{{ repo.scenarioCn }}</span>
                </span>
              </a>
            </div>
          </section>

          <section class="home-section home-section--knowledge home-section-anim">
            <div class="home-section-header">
              <div>
                <p class="home-section-kicker">Knowledge</p>
                <h2 class="home-section-title">
                  <span class="home-section-icon">
                    <Icons name="book" :size="20" />
                  </span>
                  最新知识
                </h2>
              </div>
              <router-link to="/articles" class="home-more">更多</router-link>
            </div>
            <ul v-if="latestArticles?.length" class="home-list">
              <li v-for="(a, i) in latestArticles" :key="a.id" class="home-list-item" :style="{ animationDelay: `${i * 35}ms` }">
                <router-link :to="'/articles/' + a.id" class="home-list-link">{{ a.title }}</router-link>
              </li>
            </ul>
            <p v-else-if="!loading" class="home-empty">暂无知识内容</p>
          </section>

          <section class="home-section home-section--forum home-section-anim">
            <div class="home-section-header">
              <div>
                <p class="home-section-kicker">Forum</p>
                <h2 class="home-section-title">
                  <span class="home-section-icon">
                    <Icons name="document" :size="20" />
                  </span>
                  技术交流论坛
                </h2>
              </div>
              <router-link to="/forum" class="home-more">进入</router-link>
            </div>
            <router-link to="/forum" class="home-forum-card">
              <span class="home-forum-card__mark">讨论场</span>
              <span class="home-forum-card__copy">
                <strong>把问题、方案和经验留在一个可以搜索、回复、采纳和收藏的地方。</strong>
                <small>适合提问求助、方案讨论、踩坑复盘和最佳实践沉淀。</small>
              </span>
            </router-link>
          </section>
        </div>

        <aside v-if="latestNews?.length || latestLlmLeaderboard?.length" class="home-sidebar">
          <section v-if="latestLlmLeaderboard?.length" class="home-section home-section--leaderboard home-section-news home-section-anim">
            <div class="home-section-header">
              <div>
                <p class="home-section-kicker">Leaderboard</p>
                <h2 class="home-section-title">
                  <span class="home-section-icon">
                    <Icons name="sparkle" :size="20" />
                  </span>
                  最新编程模型排行榜
                </h2>
              </div>
              <router-link to="/llm-leaderboard" class="home-more">更多</router-link>
            </div>
            <ul class="home-news-list">
              <li v-for="(m, i) in latestLlmLeaderboard" :key="m.id" class="home-news-item" :style="{ animationDelay: `${i * 35}ms` }">
                <router-link to="/llm-leaderboard" class="home-news-link">
                  <span class="home-news-num" :class="i < 3 ? 'home-news-num-hot' : 'home-news-num-normal'">{{ i + 1 }}</span>
                  <span class="home-news-copy">
                    <span class="home-news-title">{{ m.rankBadge }}{{ m.modelName }}</span>
                    <span v-if="m.organization || m.provider" class="home-news-meta">{{ m.organization || m.provider }}</span>
                  </span>
                </router-link>
              </li>
            </ul>
          </section>

          <section v-if="latestNews?.length" class="home-section home-section--news home-section-news home-section-anim">
            <div class="home-section-header">
              <div>
                <p class="home-section-kicker">News</p>
                <h2 class="home-section-title">
                  <span class="home-section-icon">
                    <Icons name="document" :size="20" />
                  </span>
                  AI 资讯
                </h2>
              </div>
              <router-link to="/news" class="home-more">更多</router-link>
            </div>
            <ul class="home-news-list">
              <li v-for="(n, i) in latestNews" :key="n.id" class="home-news-item" :style="{ animationDelay: `${i * 35}ms` }">
                <router-link :to="'/news/' + n.id" class="home-news-link">
                  <span class="home-news-num" :class="i < 3 ? 'home-news-num-hot' : 'home-news-num-normal'">{{ i + 1 }}</span>
                  <span class="home-news-copy">
                    <span class="home-news-title">{{ n.title }}</span>
                    <span v-if="n.source" class="home-news-meta">{{ n.source }}</span>
                  </span>
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
import { computed, ref, onMounted } from 'vue'
import Icons from '../components/Icons.vue'
import api from '../services/api'

const latestSkills = ref([])
const latestArticles = ref([])
const latestNews = ref([])
const latestLlmLeaderboard = ref([])
const githubTrendingWeekly = ref([])
const githubTrendingMonthly = ref([])
const githubTrendingUpdatedAt = ref('')
const githubTrendingPeriod = ref('weekly')
const loading = ref(true)
const error = ref('')

const hasGithubTrending = computed(() =>
  githubTrendingWeekly.value.length > 0 || githubTrendingMonthly.value.length > 0
)

const currentGithubTrending = computed(() =>
  githubTrendingPeriod.value === 'weekly'
    ? githubTrendingWeekly.value
    : githubTrendingMonthly.value
)

const formattedGithubTrendingUpdatedAt = computed(() => {
  if (!githubTrendingUpdatedAt.value) return '暂无同步记录'
  const date = new Date(githubTrendingUpdatedAt.value)
  if (Number.isNaN(date.getTime())) return '暂无同步记录'
  return date.toLocaleString('zh-CN', { hour12: false })
})

onMounted(async () => {
  try {
    const data = await api.get('/home')
    latestSkills.value = data.latestSkills || []
    latestArticles.value = data.latestArticles || []
    latestNews.value = (data.latestNews || []).slice(0, 10)
    latestLlmLeaderboard.value = data.latestLlmLeaderboard || []
    githubTrendingWeekly.value = data.githubTrendingWeekly || []
    githubTrendingMonthly.value = data.githubTrendingMonthly || []
    githubTrendingUpdatedAt.value = data.githubTrendingUpdatedAt || ''
    if (!githubTrendingWeekly.value.length && githubTrendingMonthly.value.length) {
      githubTrendingPeriod.value = 'monthly'
    }
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function assetLevelShort(value) {
  if (value === 'COMPANY') return '公司'
  if (value === 'TEAM') return '团队'
  if (value === 'PERSONAL') return '个人'
  return '团队'
}

function lifecycleLabel(value) {
  const labels = {
    CANDIDATE: '候选',
    TRIAL: '试用中',
    REVIEWING: '评审中',
    APPROVED: '已入库',
    NEEDS_REVIEW: '待复审',
    ARCHIVED: '已归档',
  }
  return labels[value] || '候选'
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fafbfc 0%, #f1f3f6 100%);
}

.home-hero {
  position: relative;
  padding: 3.25rem 1.5rem 3.75rem;
  overflow: hidden;
  background:
    linear-gradient(rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(165deg, #f0f7ff 0%, #e8f4f8 40%, #f5f9fc 100%);
  background-size: 28px 28px, 28px 28px, 100% 100%;
  border-bottom: 1px solid rgba(226, 232, 240, 0.68);
}

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
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  text-align: center;
}

.home-eyebrow {
  margin: 0 0 0.75rem;
  color: #2E68B8;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0;
}

.home-title {
  font-size: clamp(1rem, 2.4vw + 0.55rem, 3.35rem);
  font-weight: 760;
  color: #0f172a;
  letter-spacing: 0;
  line-height: 1.1;
  margin: 0 0 0.75rem;
  white-space: nowrap;
}

.home-title-gradient {
  background: linear-gradient(90deg, #2E68B8 0%, #2EC9D7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.home-subtitle {
  max-width: 32rem;
  margin: 0 auto;
  font-size: 0.96rem;
  color: #64748b;
  line-height: 1.75;
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
  width: 340px;
  position: sticky;
  top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

@media (max-width: 1023px) {
  .home-sidebar {
    width: 100%;
  }
}

.home-section {
  position: relative;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 14px;
  padding: 1.45rem 1.65rem;
  box-shadow: 0 2px 14px rgba(15, 23, 42, 0.045), 0 0 1px rgba(15, 23, 42, 0.06);
  animation: home-fade-up 0.5s ease-out both;
}

.home-section::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--home-section-accent, #2E68B8);
}

.home-section--skills {
  --home-section-accent: #2EC9D7;
  --home-section-soft: rgba(46, 201, 215, 0.1);
  --home-section-text: #087987;
}

.home-section--knowledge {
  --home-section-accent: #2E68B8;
  --home-section-soft: rgba(46, 104, 184, 0.1);
  --home-section-text: #2E68B8;
}

.home-section--forum {
  --home-section-accent: #14b8a6;
  --home-section-soft: rgba(20, 184, 166, 0.12);
  --home-section-text: #0f766e;
}

.home-section--leaderboard {
  --home-section-accent: #EBC050;
  --home-section-soft: rgba(235, 192, 80, 0.18);
  --home-section-text: #b58105;
}

.home-section--github {
  --home-section-accent: #24292f;
  --home-section-soft: rgba(36, 41, 47, 0.09);
  --home-section-text: #24292f;
}

.home-section--news {
  --home-section-accent: #EB8888;
  --home-section-soft: rgba(235, 136, 136, 0.14);
  --home-section-text: #d87070;
}

.home-section-anim:nth-child(1) { animation-delay: 0.05s; }
.home-section-anim:nth-child(2) { animation-delay: 0.1s; }
.home-section-anim:nth-child(3) { animation-delay: 0.15s; }
.home-section-anim:nth-child(4) { animation-delay: 0.2s; }
.home-section-anim:nth-child(5) { animation-delay: 0.25s; }

.home-section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.home-section-kicker {
  margin: 0 0 0.34rem;
  color: #94a3b8;
  font-size: 0.73rem;
  font-weight: 800;
  letter-spacing: 0;
}

.home-section-title {
  margin: 0;
  font-size: 1.08rem;
  font-weight: 720;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 0.58rem;
}

.home-section-icon {
  width: 2.05rem;
  height: 2.05rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  color: var(--home-section-text, #2E68B8);
  background: var(--home-section-soft, rgba(46, 104, 184, 0.1));
}

.home-more {
  flex-shrink: 0;
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--home-section-text, var(--color-primary, #2563eb));
  transition: opacity 0.2s;
}

.home-more:hover {
  opacity: 0.82;
  text-decoration: underline;
}

.home-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  transition: border-color 0.25s, box-shadow 0.25s, transform 0.25s;
  animation: home-fade-up 0.45s ease-out both;
}

.home-card-skill {
  flex-direction: row;
  align-items: flex-start;
  gap: 1rem;
  min-height: 8.5rem;
  padding: 1rem 1.25rem;
}

.home-card-skill .home-card-body {
  flex: 1;
  min-width: 0;
  padding: 0;
}

.home-card-skill-marker {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #2E68B8 0%, #2EC9D7 100%);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 760;
}

.home-card:hover {
  border-color: rgba(46, 104, 184, 0.38);
  box-shadow: 0 10px 24px rgba(46, 104, 184, 0.12);
  transform: translateY(-2px);
}

.home-card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.home-card-title {
  min-width: 0;
  margin: 0;
  font-size: 1rem;
  font-weight: 680;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-card-status {
  flex-shrink: 0;
  padding: 0.16rem 0.42rem;
  border-radius: 6px;
  color: #b58105;
  background: rgba(235, 192, 80, 0.18);
  font-size: 0.72rem;
  font-weight: 760;
}

.home-card-desc {
  font-size: 0.8125rem;
  color: #64748b;
  line-height: 1.55;
  margin-top: 0.48rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.8rem;
  color: #64748b;
  font-size: 0.75rem;
}

.home-card-meta span {
  padding: 0.2rem 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #f8fafc;
}

.home-forum-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  min-height: 96px;
  padding: 1rem;
  border-radius: 18px;
  color: inherit;
  text-decoration: none;
  border: 1px solid rgba(13, 148, 136, 0.14);
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.16), transparent 34%),
    linear-gradient(135deg, rgba(240, 253, 250, 0.96), rgba(255, 255, 255, 0.96));
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.home-forum-card:hover {
  transform: translateY(-2px);
  border-color: rgba(13, 148, 136, 0.32);
  box-shadow: 0 18px 32px rgba(13, 148, 136, 0.12);
}

.home-forum-card__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: white;
  font-weight: 800;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.home-forum-card__copy {
  display: grid;
  gap: 0.35rem;
}

.home-forum-card__copy strong {
  color: #0f172a;
  line-height: 1.55;
}

.home-forum-card__copy small {
  color: #64748b;
  line-height: 1.5;
}

.home-list {
  list-style: none;
  padding: 0;
  margin: 0;
  border-top: 1px solid #f1f5f9;
}

.home-list-item {
  padding: 0.82rem 0;
  border-bottom: 1px solid #f1f5f9;
  animation: home-fade-up 0.4s ease-out both;
}

.home-list-item:last-child {
  border-bottom: none;
}

.home-list-link {
  font-size: 0.94rem;
  color: #334155;
  transition: color 0.2s;
}

.home-list-link:hover {
  color: var(--home-section-text, var(--color-primary, #2563eb));
}

.home-section-news {
  min-width: 0;
}

.home-news-list {
  list-style: none;
  padding: 0;
  margin: 0;
  border-top: 1px solid #f1f5f9;
}

.home-news-item {
  animation: home-fade-up 0.4s ease-out both;
}

.home-news-link {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  padding: 0.68rem 0;
  font-size: 0.875rem;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  transition: color 0.2s;
}

.home-news-item:last-child .home-news-link {
  border-bottom: none;
}

.home-news-link:hover {
  color: var(--home-section-text, var(--color-primary, #2563eb));
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
  width: 1.35rem;
  height: 1.35rem;
  line-height: 1.35rem;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 5px;
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

.home-news-copy {
  flex: 1;
  min-width: 0;
}

.home-news-title {
  line-height: 1.42;
  font-weight: 620;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-news-meta {
  display: block;
  margin-top: 0.22rem;
  color: #64748b;
  font-size: 0.76rem;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-trending-header {
  align-items: flex-start;
}

.home-trending-time {
  margin: 0.35rem 0 0;
  color: #94a3b8;
  font-size: 0.74rem;
  line-height: 1.35;
}

.home-trending-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.home-trending-card {
  display: flex;
  align-items: flex-start;
  gap: 0.82rem;
  min-height: 9rem;
  padding: 1rem;
  border: 1px solid rgba(203, 213, 225, 0.84);
  border-radius: 10px;
  color: inherit;
  text-decoration: none;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.9), rgba(255, 255, 255, 0.98));
  animation: home-fade-up 0.4s ease-out both;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.home-trending-card:hover {
  border-color: rgba(36, 41, 47, 0.24);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.home-trending-rank {
  flex-shrink: 0;
  width: 2.35rem;
  height: 2.35rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 780;
}

.home-trending-rank--hot {
  color: #ffffff;
  background: #24292f;
  box-shadow: 0 8px 18px rgba(36, 41, 47, 0.18);
}

.home-trending-rank--normal {
  color: #475569;
  background: #eef2f7;
}

.home-trending-copy {
  min-width: 0;
  display: grid;
  gap: 0.48rem;
}

.home-trending-repo {
  color: #0f172a;
  font-size: 0.98rem;
  font-weight: 720;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.home-trending-effect {
  color: #334155;
  font-size: 0.84rem;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-trending-tabs {
  flex-shrink: 0;
  display: inline-flex;
  gap: 0.25rem;
  padding: 0.18rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.home-trending-tab {
  min-width: 3rem;
  min-height: 1.8rem;
  padding: 0.34rem 0.62rem;
  border: 0;
  border-radius: 6px;
  color: #64748b;
  background: transparent;
  font-size: 0.78rem;
  font-weight: 760;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s, background 0.2s, box-shadow 0.2s;
}

.home-trending-tab:hover,
.home-trending-tab--active {
  color: #0f172a;
  background: #fff;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.home-trending-scenario {
  color: #64748b;
  font-size: 0.78rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 420px) {
  .home-trending-header {
    flex-direction: column;
    gap: 0.8rem;
  }

  .home-trending-tabs {
    width: 100%;
  }

  .home-trending-tab {
    flex: 1;
  }
}

@media (max-width: 639px) {
  .home-trending-grid {
    grid-template-columns: 1fr;
  }

  .home-trending-card {
    min-height: auto;
  }
}

.home-loading,
.home-empty {
  text-align: center;
  color: #64748b;
  padding: 2.25rem 0;
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
  .home-trending-card,
  .home-list-item,
  .home-news-item {
    animation: none !important;
  }
  .home-card:hover,
  .home-trending-card:hover {
    transform: none;
  }
}
</style>
