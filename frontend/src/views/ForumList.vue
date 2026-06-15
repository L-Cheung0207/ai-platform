<template>
  <div class="forum-page">
    <PageHero variant="teal" title="论坛" subtitle="给同事们一个认真讨论技术问题、沉淀经验和复盘方案的地方">
      <div class="global-search forum-hero-search">
        <el-input
          v-model="keyword"
          placeholder="搜索帖子、正文、关联内容"
          clearable
          size="large"
          @keyup.enter="search"
        />
        <el-button type="primary" size="large" @click="search">搜索</el-button>
      </div>
    </PageHero>

    <div class="forum-shell max-w-[1400px] mx-auto px-6 py-8">
      <div class="forum-toolbar">
        <div class="forum-toolbar__filters">
          <button
            v-for="opt in sortOptions"
            :key="opt.value"
            type="button"
            class="forum-pill"
            :class="{ 'forum-pill--active': sort === opt.value }"
            @click="setSort(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
        <el-button type="primary" class="forum-toolbar__action" @click="goCreate">
          发帖
        </el-button>
      </div>

      <div class="forum-layout">
        <section class="forum-main">
          <div class="forum-summary">
            <div class="forum-summary__item">
              <span class="forum-summary__label">当前分类</span>
              <span class="forum-summary__value">{{ activeCategoryName || '全部' }}</span>
            </div>
            <div class="forum-summary__item">
              <span class="forum-summary__label">当前标签</span>
              <span class="forum-summary__value">{{ activeTagLabel || '全部' }}</span>
            </div>
            <div class="forum-summary__item">
              <span class="forum-summary__label">排序</span>
              <span class="forum-summary__value">{{ sortLabel }}</span>
            </div>
          </div>

          <div v-if="loading" class="forum-list forum-list--skeleton" aria-busy="true">
            <div v-for="i in 5" :key="i" class="forum-card forum-card--skeleton">
              <div class="h-5 w-3/4 bg-slate-200 rounded mb-3" />
              <div class="h-4 w-full bg-slate-100 rounded mb-2" />
              <div class="h-4 w-5/6 bg-slate-100 rounded mb-4" />
              <div class="flex gap-2">
                <div class="h-6 w-16 bg-slate-100 rounded-full" />
                <div class="h-6 w-20 bg-slate-100 rounded-full" />
              </div>
            </div>
          </div>

          <div v-else class="forum-list">
            <router-link
              v-for="post in posts"
              :key="post.id"
              :to="'/forum/' + post.id"
              class="forum-card"
            >
              <div class="forum-card__top">
                <div class="forum-card__title-row">
                  <h3 class="forum-card__title">{{ post.title }}</h3>
                  <div class="forum-card__badges">
                    <span v-if="post.pinned" class="forum-badge forum-badge--accent">置顶</span>
                    <span v-if="post.featured" class="forum-badge forum-badge--warm">加精</span>
                    <span class="forum-badge">{{ postTypeLabel(post.postType) }}</span>
                    <span v-if="post.accepted" class="forum-badge forum-badge--success">已解决</span>
                    <span v-if="post.status === 'LOCKED'" class="forum-badge forum-badge--muted">锁定</span>
                  </div>
                </div>
                <p class="forum-card__excerpt">{{ post.excerpt || '暂时没有摘要' }}</p>
              </div>

              <div class="forum-card__meta">
                <div class="forum-card__tags">
                  <span v-for="t in (post.tags || []).slice(0, 5)" :key="t.id" class="tag-chip tag-chip--sm forum-tag">
                    {{ t.name }}
                  </span>
                  <span v-if="post.relatedTitle" class="tag-chip tag-chip--sm tag-chip--muted">
                    {{ relatedLabel(post.relatedType) }} · {{ post.relatedTitle }}
                  </span>
                </div>
                <div class="forum-card__stats">
                  <span>{{ post.authorUsername || '匿名' }}</span>
                  <span>{{ formatForumRelativeTime(post.lastActivityAt || post.updatedAt) }}</span>
                  <span>回复 {{ post.replyCount || 0 }}</span>
                  <span>赞 {{ post.likeCount || 0 }}</span>
                  <span>收藏 {{ post.favoriteCount || 0 }}</span>
                </div>
              </div>
            </router-link>

            <div v-if="!loading && !error && posts.length === 0" class="forum-empty">
              <p>没有找到匹配的帖子。</p>
              <el-button type="primary" @click="goCreate">发第一个帖子</el-button>
            </div>
          </div>

          <div class="forum-pagination">
            <el-pagination
              v-if="total > 0"
              v-model:current-page="page"
              :page-size="size"
              :total="total"
              layout="prev, pager, next"
              @current-change="loadPosts"
            />
          </div>

          <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
        </section>

        <aside class="forum-sidebar">
          <section class="forum-panel">
            <div class="forum-panel__header">
              <h3>分类</h3>
              <button type="button" class="forum-panel__link" @click="clearCategory">全部</button>
            </div>
            <div class="forum-chip-grid">
              <button
                v-for="cat in categories"
                :key="cat.id"
                type="button"
                class="forum-chip"
                :class="{ 'forum-chip--active': selectedCategoryId === cat.id }"
                @click="selectCategory(cat.id)"
              >
                <span>{{ cat.name }}</span>
                <small>{{ cat.sortOrder }}</small>
              </button>
            </div>
          </section>

          <section class="forum-panel">
            <div class="forum-panel__header">
              <h3>热标签</h3>
              <button type="button" class="forum-panel__link" @click="clearTags">清空</button>
            </div>
            <div class="forum-tag-cloud">
              <button
                v-for="tag in tags"
                :key="tag.id"
                type="button"
                class="forum-tag-cloud__item"
                :class="{ 'forum-tag-cloud__item--active': selectedTagIds.includes(tag.id) }"
                @click="toggleTag(tag.id)"
              >
                #{{ tag.name }}
              </button>
            </div>
          </section>

          <section class="forum-panel forum-panel--sticky">
            <div class="forum-panel__header">
              <h3>热帖</h3>
              <router-link to="/forum?sort=hot" class="forum-panel__link">更多</router-link>
            </div>
            <div class="forum-hot-list">
              <router-link
                v-for="item in hotTopics"
                :key="item.id"
                :to="'/forum/' + item.id"
                class="forum-hot-item"
              >
                <span class="forum-hot-item__dot" :class="{ 'forum-hot-item__dot--pinned': item.pinned }">{{ item.pinned ? '置' : '热' }}</span>
                <span class="forum-hot-item__text">
                  <span class="forum-hot-item__title">{{ item.title }}</span>
                  <span class="forum-hot-item__meta">{{ item.replyCount || 0 }} 回复 · {{ item.likeCount || 0 }} 赞</span>
                </span>
              </router-link>
            </div>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'
import { formatForumRelativeTime, forumExcerpt, forumPostTypeLabels, forumRelatedTypeLabels } from '../utils/forum'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const keyword = ref('')
const sort = ref('latest')
const page = ref(1)
const size = ref(10)
const posts = ref([])
const total = ref(0)
const categories = ref([])
const tags = ref([])
const hotTopics = ref([])
const loading = ref(false)
const error = ref('')
const selectedCategoryId = ref(null)
const selectedTagIds = ref([])
const selectedTagParam = ref('')

const sortOptions = [
  { value: 'latest', label: '最新' },
  { value: 'hot', label: '最热' },
  { value: 'unresolved', label: '未解决' },
]

const activeCategoryName = computed(() => categories.value.find((c) => c.id === selectedCategoryId.value)?.name || '')
const activeTagLabel = computed(() => {
  if (!selectedTagIds.value.length) return ''
  if (selectedTagIds.value.length === 1) {
    return tags.value.find((t) => t.id === selectedTagIds.value[0])?.name || `标签 #${selectedTagIds.value[0]}`
  }
  return `${selectedTagIds.value.length} 个标签`
})
const sortLabel = computed(() => sortOptions.find((opt) => opt.value === sort.value)?.label || '最新')

watch(
  () => [route.params.id, route.params.tag, route.query.sort],
  () => {
    syncRouteFilters()
    loadPosts()
  },
  { immediate: true }
)

async function loadSidebar() {
  try {
    categories.value = await api.get('/forum/categories')
  } catch {
    categories.value = []
  }
  try {
    tags.value = await api.get('/forum/tags')
  } catch {
    tags.value = []
  }
  try {
    const data = await api.get('/forum/posts', { params: { page: 1, size: 5, sort: 'hot' } })
    hotTopics.value = data.items || []
  } catch {
    hotTopics.value = []
  }
}

function syncRouteFilters() {
  selectedCategoryId.value = route.params.id ? Number(route.params.id) || null : null
  selectedTagParam.value = route.params.tag ? decodeURIComponent(String(route.params.tag)) : ''
  selectedTagIds.value = []
  sort.value = typeof route.query.sort === 'string' ? route.query.sort : sort.value
}

async function loadPosts() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value, sort: sort.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (selectedCategoryId.value) params.categoryId = selectedCategoryId.value
    if (selectedTagIds.value.length) params.tagIds = selectedTagIds.value.join(',')
    const data = await api.get('/forum/posts', { params })
    posts.value = (data.items || []).map((post) => ({
      ...post,
      excerpt: post.excerpt || forumExcerpt(post.content),
    }))
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function resolveTagRoute() {
  if (!selectedTagParam.value) return false
  const raw = selectedTagParam.value.trim()
  const numericId = Number(raw)
  if (Number.isFinite(numericId) && numericId > 0) {
    selectedTagIds.value = [numericId]
    return true
  }
  const matched = tags.value.find((tag) => (tag.name || '').toLowerCase() === raw.toLowerCase())
  if (matched) {
    selectedTagIds.value = [matched.id]
    return true
  }
  return false
}

function search() {
  page.value = 1
  loadPosts()
}

function setSort(next) {
  sort.value = next
  page.value = 1
  loadPosts()
}

function selectCategory(id) {
  router.push(`/forum/categories/${id}`)
}

function clearCategory() {
  router.push('/forum')
}

function toggleTag(id) {
  selectedTagIds.value = selectedTagIds.value.includes(id)
    ? selectedTagIds.value.filter((tagId) => tagId !== id)
    : [...selectedTagIds.value, id]
  page.value = 1
  loadPosts()
}

function clearTags() {
  selectedTagIds.value = []
  page.value = 1
  loadPosts()
}

function goCreate() {
  if (!auth.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: '/forum/new' } })
    return
  }
  router.push('/forum/new')
}

function postTypeLabel(value) {
  return forumPostTypeLabels[value] || value || '帖子'
}

function relatedLabel(value) {
  return forumRelatedTypeLabels[value] || '关联'
}

onMounted(async () => {
  await loadSidebar()
  if (resolveTagRoute()) {
    await loadPosts()
  }
})
</script>

<style scoped>
@reference "../assets/styles.css";

.forum-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(13, 148, 136, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fbfb 0%, #eef5f4 100%);
}

.forum-shell {
  position: relative;
}

.forum-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.forum-toolbar__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.forum-pill {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.85);
  color: #475569;
  padding: 0.6rem 0.95rem;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.2s ease;
}

.forum-pill:hover {
  border-color: rgba(13, 148, 136, 0.35);
  color: #0f766e;
  transform: translateY(-1px);
}

.forum-pill--active {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: white;
  border-color: transparent;
  box-shadow: 0 10px 24px rgba(13, 148, 136, 0.2);
}

.forum-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 1.5rem;
}

@media (max-width: 1100px) {
  .forum-layout {
    grid-template-columns: 1fr;
  }
}

.forum-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

@media (max-width: 768px) {
  .forum-summary {
    grid-template-columns: 1fr;
  }
}

.forum-summary__item {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  padding: 0.9rem 1rem;
  backdrop-filter: blur(12px);
}

.forum-summary__label {
  display: block;
  font-size: 0.78rem;
  color: #64748b;
  margin-bottom: 0.2rem;
}

.forum-summary__value {
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f172a;
}

.forum-list {
  display: grid;
  gap: 0.9rem;
}

.forum-card {
  display: block;
  padding: 1.1rem 1.1rem 1rem;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.07);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 252, 252, 0.94));
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.forum-card:hover {
  transform: translateY(-2px);
  border-color: rgba(20, 184, 166, 0.35);
  box-shadow: 0 18px 32px rgba(13, 148, 136, 0.12);
}

.forum-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.85rem;
  margin-bottom: 0.55rem;
}

.forum-card__title {
  margin: 0;
  font-size: 1.05rem;
  line-height: 1.45;
  font-weight: 800;
  color: #0f172a;
}

.forum-card__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.35rem;
}

.forum-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.28rem 0.6rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  background: #e2e8f0;
  color: #475569;
}

.forum-badge--accent {
  background: #d1fae5;
  color: #047857;
}

.forum-badge--warm {
  background: #fef3c7;
  color: #92400e;
}

.forum-badge--success {
  background: #dcfce7;
  color: #166534;
}

.forum-badge--muted {
  background: #e2e8f0;
  color: #475569;
}

.forum-card__excerpt {
  margin: 0;
  color: #475569;
  line-height: 1.75;
  font-size: 0.94rem;
}

.forum-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 0.9rem;
  align-items: flex-end;
}

@media (max-width: 768px) {
  .forum-card__meta {
    flex-direction: column;
    align-items: flex-start;
  }
}

.forum-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.forum-tag {
  border-radius: 999px;
}

.forum-card__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  justify-content: flex-end;
  color: #64748b;
  font-size: 0.78rem;
}

.forum-sidebar {
  display: grid;
  gap: 1rem;
}

.forum-panel {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 20px;
  padding: 1rem;
  backdrop-filter: blur(12px);
}

.forum-panel--sticky {
  position: sticky;
  top: 1rem;
}

.forum-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.forum-panel__header h3 {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 800;
  color: #0f172a;
}

.forum-panel__link {
  border: none;
  background: transparent;
  color: #0f766e;
  font-size: 0.85rem;
  font-weight: 700;
}

.forum-chip-grid {
  display: grid;
  gap: 0.5rem;
}

.forum-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #f8fafc;
  color: #334155;
  padding: 0.65rem 0.8rem;
  border-radius: 14px;
  font-size: 0.9rem;
  font-weight: 700;
  text-align: left;
  transition: all 0.2s ease;
}

.forum-chip small {
  color: #94a3b8;
}

.forum-chip:hover,
.forum-chip--active {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  color: white;
  border-color: transparent;
}

.forum-chip--active small,
.forum-chip:hover small {
  color: rgba(255, 255, 255, 0.82);
}

.forum-tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.forum-tag-cloud__item {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #f8fafc;
  color: #475569;
  padding: 0.5rem 0.72rem;
  border-radius: 999px;
  font-size: 0.86rem;
  transition: all 0.2s ease;
}

.forum-tag-cloud__item:hover,
.forum-tag-cloud__item--active {
  background: #ccfbf1;
  color: #0f766e;
  border-color: rgba(20, 184, 166, 0.3);
}

.forum-hot-list {
  display: grid;
  gap: 0.7rem;
}

.forum-hot-item {
  display: flex;
  gap: 0.7rem;
  align-items: flex-start;
  color: inherit;
  text-decoration: none;
}

.forum-hot-item__dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 800;
  flex-shrink: 0;
}

.forum-hot-item__dot--pinned {
  background: #d1fae5;
  color: #047857;
}

.forum-hot-item__text {
  min-width: 0;
  display: grid;
}

.forum-hot-item__title {
  font-size: 0.92rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.45;
}

.forum-hot-item__meta {
  font-size: 0.77rem;
  color: #64748b;
  margin-top: 0.15rem;
}

.forum-pagination {
  display: flex;
  justify-content: center;
  margin-top: 1rem;
}

.forum-empty {
  display: grid;
  place-items: center;
  gap: 0.8rem;
  padding: 3rem 1rem;
  border-radius: 18px;
  border: 1px dashed rgba(15, 23, 42, 0.12);
  color: #64748b;
  background: rgba(255, 255, 255, 0.7);
}

@media (max-width: 768px) {
  .forum-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .forum-toolbar__action {
    width: 100%;
  }
}
</style>
