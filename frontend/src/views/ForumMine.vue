<template>
  <div class="forum-mine-page">
    <PageHero
      variant="teal"
      title="我的论坛"
      subtitle="查看自己发过的帖子、回复过的讨论和收藏的内容"
    />

    <div class="forum-mine-shell max-w-[1280px] mx-auto px-6 py-8">
      <el-tabs v-model="activeTab" class="forum-mine-tabs" @tab-change="onTabChange">
        <el-tab-pane label="我的帖子" name="posts">
          <section class="forum-mine-section">
            <template v-if="loading.posts">
              <p class="forum-mine-loading">加载中…</p>
            </template>
            <template v-else>
              <router-link v-for="item in posts" :key="item.id" :to="'/forum/' + item.id" class="forum-mine-card">
                <div class="forum-mine-card__title">{{ item.title }}</div>
                <div class="forum-mine-card__meta">
                  <span>{{ item.categoryName || '未分类' }}</span>
                  <span>{{ formatForumRelativeTime(item.lastActivityAt || item.updatedAt) }}</span>
                  <span>回复 {{ item.replyCount || 0 }}</span>
                  <span>赞 {{ item.likeCount || 0 }}</span>
                </div>
                <p class="forum-mine-card__excerpt">{{ item.excerpt || forumExcerpt(item.content) }}</p>
              </router-link>
              <el-empty v-if="!posts.length" description="暂无帖子" />
            </template>
          </section>
        </el-tab-pane>

        <el-tab-pane label="我的回复" name="replies">
          <section class="forum-mine-section">
            <template v-if="loading.replies">
              <p class="forum-mine-loading">加载中…</p>
            </template>
            <template v-else>
              <div v-for="item in replies" :key="item.id" class="forum-mine-card">
                <div class="forum-mine-card__title">{{ item.authorUsername || '我' }} 的回复</div>
                <div class="forum-mine-card__meta">
                  <span>{{ formatForumRelativeTime(item.createdAt) }}</span>
                  <span>赞 {{ item.likeCount || 0 }}</span>
                </div>
                <p class="forum-mine-card__excerpt">{{ item.content }}</p>
                <router-link v-if="item.postId" :to="'/forum/' + item.postId" class="forum-mine-card__link">查看原帖 →</router-link>
              </div>
              <el-empty v-if="!replies.length" description="暂无回复" />
            </template>
          </section>
        </el-tab-pane>

        <el-tab-pane label="我的收藏" name="favorites">
          <section class="forum-mine-section">
            <template v-if="loading.favorites">
              <p class="forum-mine-loading">加载中…</p>
            </template>
            <template v-else>
              <router-link v-for="item in favorites" :key="item.id" :to="'/forum/' + item.id" class="forum-mine-card">
                <div class="forum-mine-card__title">{{ item.title }}</div>
                <div class="forum-mine-card__meta">
                  <span>{{ item.categoryName || '未分类' }}</span>
                  <span>{{ formatForumRelativeTime(item.lastActivityAt || item.updatedAt) }}</span>
                  <span>收藏 {{ item.favoriteCount || 0 }}</span>
                </div>
                <p class="forum-mine-card__excerpt">{{ item.excerpt || forumExcerpt(item.content) }}</p>
              </router-link>
              <el-empty v-if="!favorites.length" description="暂无收藏" />
            </template>
          </section>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'
import { forumExcerpt, formatForumRelativeTime } from '../utils/forum'

const activeTab = ref('posts')
const posts = ref([])
const replies = ref([])
const favorites = ref([])
const loading = ref({ posts: true, replies: true, favorites: true })

onMounted(async () => {
  await Promise.all([loadPosts(), loadReplies(), loadFavorites()])
})

async function onTabChange(name) {
  if (name === 'posts' && !posts.value.length) await loadPosts()
  if (name === 'replies' && !replies.value.length) await loadReplies()
  if (name === 'favorites' && !favorites.value.length) await loadFavorites()
}

async function loadPosts() {
  loading.value.posts = true
  try {
    const data = await api.get('/forum/mine/posts', { params: { page: 1, size: 100 } })
    posts.value = data.items || []
  } finally {
    loading.value.posts = false
  }
}

async function loadReplies() {
  loading.value.replies = true
  try {
    const data = await api.get('/forum/mine/replies', { params: { page: 1, size: 100 } })
    replies.value = data.items || []
  } finally {
    loading.value.replies = false
  }
}

async function loadFavorites() {
  loading.value.favorites = true
  try {
    const data = await api.get('/forum/mine/favorites', { params: { page: 1, size: 100 } })
    favorites.value = data.items || []
  } finally {
    loading.value.favorites = false
  }
}
</script>

<style scoped>
@reference "../assets/styles.css";

.forum-mine-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.08), transparent 26%),
    linear-gradient(180deg, #f8fbfb 0%, #eef5f4 100%);
}

.forum-mine-tabs :deep(.el-tabs__header) {
  margin-bottom: 1rem;
}

.forum-mine-section {
  display: grid;
  gap: 0.85rem;
}

.forum-mine-card {
  display: block;
  border-radius: 18px;
  padding: 1rem 1.1rem;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(15, 23, 42, 0.06);
  color: inherit;
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.forum-mine-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.forum-mine-card__title {
  font-size: 1rem;
  font-weight: 800;
  color: #0f172a;
}

.forum-mine-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  margin-top: 0.45rem;
  color: #64748b;
  font-size: 0.82rem;
}

.forum-mine-card__excerpt {
  margin: 0.65rem 0 0;
  color: #475569;
  line-height: 1.7;
}

.forum-mine-card__link {
  display: inline-block;
  margin-top: 0.75rem;
  color: #0f766e;
  font-weight: 700;
  text-decoration: none;
}

.forum-mine-loading {
  color: #64748b;
}
</style>
