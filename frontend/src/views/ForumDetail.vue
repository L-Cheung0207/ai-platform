<template>
  <div class="forum-detail-page">
    <PageHero variant="teal" compact>
      <div class="forum-detail-hero">
        <router-link to="/forum" class="forum-detail-back">← 返回论坛</router-link>
        <div v-if="post" class="forum-detail-hero__body">
          <div class="forum-detail-hero__badges">
            <span v-if="post.pinned" class="forum-badge forum-badge--accent">置顶</span>
            <span v-if="post.featured" class="forum-badge forum-badge--warm">加精</span>
            <span class="forum-badge">{{ postTypeLabel(post.postType) }}</span>
            <span v-if="post.status === 'LOCKED'" class="forum-badge forum-badge--muted">锁定</span>
            <span v-if="post.accepted" class="forum-badge forum-badge--success">已解决</span>
          </div>
          <h1 class="forum-detail-title">{{ post.title }}</h1>
          <p class="forum-detail-meta">
            <span>{{ post.authorUsername || '匿名' }}</span>
            <span>{{ formatForumTime(post.createdAt) }}</span>
            <span>回复 {{ post.replyCount || 0 }}</span>
            <span>赞 {{ post.likeCount || 0 }}</span>
            <span>收藏 {{ post.favoriteCount || 0 }}</span>
          </p>
        </div>
      </div>
    </PageHero>

    <div class="forum-detail-shell max-w-[1400px] mx-auto px-6 py-8">
      <template v-if="post">
        <div class="forum-detail-grid">
          <main class="forum-detail-main">
            <section class="forum-panel forum-article">
              <div class="forum-article__head">
                <div class="forum-article__tags">
                  <span v-if="post.categoryName" class="tag-chip tag-chip--sm tag-chip--muted">{{ post.categoryName }}</span>
                  <span v-for="t in post.tags || []" :key="t.id" class="tag-chip tag-chip--sm">{{ t.name }}</span>
                </div>
                <div class="forum-article__actions">
                  <el-button size="small" :type="post.likedByMe ? 'primary' : 'default'" plain @click="toggleLike">
                    {{ post.likedByMe ? '已点赞' : '点赞' }} {{ post.likeCount || 0 }}
                  </el-button>
                  <el-button size="small" :type="post.favoritedByMe ? 'success' : 'default'" plain @click="toggleFavorite">
                    {{ post.favoritedByMe ? '已收藏' : '收藏' }} {{ post.favoriteCount || 0 }}
                  </el-button>
                  <el-button v-if="canEditPost" size="small" @click="editPost">编辑</el-button>
                  <el-button v-if="canEditPost" size="small" type="danger" plain @click="deletePost">删除</el-button>
                </div>
              </div>

              <div class="forum-article__body" v-html="renderedContent" />
            </section>

            <section class="forum-panel forum-reply-panel">
              <div class="forum-panel__header">
                <h3>回复</h3>
                <span class="forum-reply-count">{{ post.replyCount || 0 }} 条</span>
              </div>

              <div v-if="post.status === 'LOCKED'" class="forum-lock-tip">
                帖子已锁定，暂时不能继续回复。
              </div>

              <div v-if="post.postType === 'QUESTION' && post.acceptedReply" class="forum-accepted-box">
                <div class="forum-accepted-box__title">已采纳答案</div>
                <div class="forum-accepted-box__content">
                  <strong>{{ post.acceptedReply.authorUsername }}</strong>
                  <p>{{ post.acceptedReply.content }}</p>
                </div>
              </div>

              <div v-for="reply in visibleReplies" :key="reply.id" class="forum-reply">
                <div class="forum-reply__head">
                  <div>
                    <strong>{{ reply.authorUsername || '匿名' }}</strong>
                    <span class="forum-reply__meta">{{ formatForumTime(reply.createdAt) }}</span>
                    <span v-if="reply.accepted" class="forum-badge forum-badge--success forum-reply__accepted">已采纳</span>
                  </div>
                  <div class="forum-reply__actions">
                    <button type="button" class="forum-reply__link" @click="beginReply(reply)">回复</button>
                    <button v-if="reply.canEdit" type="button" class="forum-reply__link" @click="beginReplyEdit(reply)">编辑</button>
                    <button type="button" class="forum-reply__link" @click="toggleReplyLike(reply)">
                      {{ reply.likedByMe ? '已赞' : '赞' }} {{ reply.likeCount || 0 }}
                    </button>
                    <button
                      v-if="canAcceptReply(reply)"
                      type="button"
                      class="forum-reply__link forum-reply__link--accent"
                      @click="acceptReply(reply)"
                    >
                      采纳
                    </button>
                  </div>
                </div>
                <div class="forum-reply__content">{{ reply.content }}</div>

                <div v-if="reply.children && reply.children.length" class="forum-reply__children">
                  <div v-for="child in reply.children" :key="child.id" class="forum-reply forum-reply--child">
                    <div class="forum-reply__head">
                      <div>
                        <strong>{{ child.authorUsername || '匿名' }}</strong>
                        <span class="forum-reply__meta">{{ formatForumTime(child.createdAt) }}</span>
                        <span v-if="child.accepted" class="forum-badge forum-badge--success forum-reply__accepted">已采纳</span>
                      </div>
                      <div class="forum-reply__actions">
                        <button type="button" class="forum-reply__link" @click="beginReply(child)">回复</button>
                        <button v-if="child.canEdit" type="button" class="forum-reply__link" @click="beginReplyEdit(child)">编辑</button>
                        <button type="button" class="forum-reply__link" @click="toggleReplyLike(child)">
                          {{ child.likedByMe ? '已赞' : '赞' }} {{ child.likeCount || 0 }}
                        </button>
                        <button
                          v-if="canAcceptReply(child)"
                          type="button"
                          class="forum-reply__link forum-reply__link--accent"
                          @click="acceptReply(child)"
                        >
                          采纳
                        </button>
                      </div>
                    </div>
                    <div class="forum-reply__content">{{ child.content }}</div>
                  </div>
                </div>
              </div>

              <div class="forum-reply-form" ref="replyFormRef">
                <div class="forum-reply-form__header">
                  <h3>{{ editingReplyId ? '编辑回复' : '写下你的回答' }}</h3>
                  <p v-if="replyParentLabel" class="forum-reply-form__hint">{{ replyParentLabel }}</p>
                </div>
                <el-input
                  v-model="replyContent"
                  type="textarea"
                  :rows="7"
                  placeholder="输入 Markdown 回复内容"
                  resize="vertical"
                  :disabled="post.status === 'LOCKED'"
                />
                <div class="forum-reply-form__footer">
                  <el-button v-if="editingReplyId" @click="cancelReplyEdit">取消编辑</el-button>
                  <el-button
                    type="primary"
                    :loading="replySaving"
                    :disabled="post.status === 'LOCKED'"
                    @click="submitReply"
                  >
                    {{ editingReplyId ? '保存修改' : '提交回复' }}
                  </el-button>
                </div>
              </div>
            </section>
          </main>

          <aside class="forum-detail-side">
            <section class="forum-panel">
              <div class="forum-panel__header">
                <h3>帖子信息</h3>
              </div>
              <dl class="forum-info-list">
                <div><dt>分类</dt><dd>{{ post.categoryName || '-' }}</dd></div>
                <div><dt>类型</dt><dd>{{ postTypeLabel(post.postType) }}</dd></div>
                <div><dt>状态</dt><dd>{{ post.status || '-' }}</dd></div>
                <div><dt>最后活跃</dt><dd>{{ formatForumRelativeTime(post.lastActivityAt) }}</dd></div>
              </dl>
            </section>
          </aside>
        </div>
      </template>

      <div v-else class="forum-loading-panel">
        <p v-if="loading">加载中…</p>
        <el-alert v-else type="error" :title="error || '帖子不存在'" show-icon />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'
import { formatForumRelativeTime, formatForumTime, renderForumMarkdown, forumPostTypeLabels } from '../utils/forum'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const post = ref(null)
const loading = ref(true)
const error = ref('')
const replyContent = ref('')
const replySaving = ref(false)
const replyParentId = ref(null)
const replyParentLabel = ref('')
const editingReplyId = ref(null)
const replyFormRef = ref(null)

const currentUserId = computed(() => auth.user?.id || null)
const renderedContent = computed(() => renderForumMarkdown(post.value?.content || ''))
const canEditPost = computed(() => post.value && currentUserId.value && post.value.authorId === currentUserId.value)
const visibleReplies = computed(() => post.value?.replies || [])

watch(
  () => route.params.id,
  () => loadPost(),
  { immediate: true }
)

async function loadPost() {
  loading.value = true
  error.value = ''
  try {
    post.value = await api.get('/forum/posts/' + route.params.id)
  } catch (e) {
    post.value = null
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function ensureLogin() {
  if (auth.isAuthenticated) return true
  router.push({ name: 'Login', query: { redirect: route.fullPath } })
  return false
}

function postTypeLabel(value) {
  return forumPostTypeLabels[value] || value || '帖子'
}

async function toggleLike() {
  if (!ensureLogin()) return
  await api[post.value.likedByMe ? 'delete' : 'post'](`/forum/posts/${post.value.id}/like`)
  await loadPost()
}

async function toggleFavorite() {
  if (!ensureLogin()) return
  await api[post.value.favoritedByMe ? 'delete' : 'post'](`/forum/posts/${post.value.id}/favorite`)
  await loadPost()
}

function editPost() {
  router.push(`/forum/${post.value.id}/edit`)
}

async function deletePost() {
  try {
    await ElMessageBox.confirm('确定删除这个帖子吗？', '提示', { type: 'warning' })
    await api.delete(`/forum/posts/${post.value.id}`)
    ElMessage.success('已删除')
    router.push('/forum')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function beginReply(reply) {
  if (!ensureLogin()) return
  replyParentId.value = reply.id
  replyParentLabel.value = `回复 @${reply.authorUsername || '匿名'}`
  editingReplyId.value = null
  replyContent.value = ''
  scrollToReplyForm()
}

function beginReplyEdit(reply) {
  if (!ensureLogin()) return
  editingReplyId.value = reply.id
  replyParentId.value = reply.parentReplyId || null
  replyParentLabel.value = reply.parentReplyId ? '编辑楼中楼回复' : '编辑回复'
  replyContent.value = reply.content || ''
  scrollToReplyForm()
}

function cancelReplyEdit() {
  editingReplyId.value = null
  replyParentId.value = null
  replyParentLabel.value = ''
  replyContent.value = ''
}

async function submitReply() {
  if (!ensureLogin()) return
  const content = (replyContent.value || '').trim()
  if (!content) {
    ElMessage.warning('请先输入回复内容')
    return
  }
  replySaving.value = true
  try {
    const payload = { content, parentReplyId: replyParentId.value }
    if (editingReplyId.value) {
      await api.put(`/forum/replies/${editingReplyId.value}`, payload)
      ElMessage.success('回复已更新')
    } else {
      await api.post(`/forum/posts/${post.value.id}/replies`, payload)
      ElMessage.success('回复已发布')
    }
    cancelReplyEdit()
    await loadPost()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    replySaving.value = false
  }
}

async function toggleReplyLike(reply) {
  if (!ensureLogin()) return
  await api[reply.likedByMe ? 'delete' : 'post'](`/forum/replies/${reply.id}/like`)
  await loadPost()
}

function canAcceptReply(reply) {
  return post.value?.postType === 'QUESTION' && canEditPost.value && !reply.accepted
}

async function acceptReply(reply) {
  if (!ensureLogin()) return
  try {
    await api.post(`/forum/posts/${post.value.id}/accept/${reply.id}`)
    ElMessage.success('已采纳回复')
    await loadPost()
  } catch (e) {
    ElMessage.error(e.message || '采纳失败')
  }
}

function scrollToReplyForm() {
  requestAnimationFrame(() => {
    replyFormRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}
</script>

<style scoped>
@reference "../assets/styles.css";

.forum-detail-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.08), transparent 26%),
    linear-gradient(180deg, #f8fbfb 0%, #eef5f4 100%);
}

.forum-detail-hero {
  max-width: 1100px;
  margin: 0 auto;
  text-align: left;
}

.forum-detail-back {
  display: inline-block;
  margin-bottom: 0.75rem;
  color: #0f766e;
  font-weight: 700;
  text-decoration: none;
}

.forum-detail-hero__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.75rem;
}

.forum-detail-title {
  margin: 0;
  font-size: clamp(1.8rem, 3.8vw, 2.8rem);
  line-height: 1.15;
  font-weight: 900;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.forum-detail-meta {
  margin: 0.75rem 0 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
  color: #64748b;
  font-size: 0.9rem;
}

.forum-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 1.5rem;
}

@media (max-width: 1100px) {
  .forum-detail-grid {
    grid-template-columns: 1fr;
  }
}

.forum-detail-main {
  display: grid;
  gap: 1rem;
}

.forum-panel {
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 20px;
  padding: 1rem;
  backdrop-filter: blur(12px);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.forum-article__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

@media (max-width: 768px) {
  .forum-article__head {
    flex-direction: column;
  }
}

.forum-article__tags,
.forum-article__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.forum-article__body {
  font-size: 1rem;
  line-height: 1.8;
  color: #1f2937;
}

.forum-article__body :deep(p) { margin: 0.85em 0; }
.forum-article__body :deep(h1) { font-size: 1.55em; margin: 1.2em 0 0.55em; font-weight: 800; }
.forum-article__body :deep(h2) { font-size: 1.3em; margin: 1.2em 0 0.5em; font-weight: 800; }
.forum-article__body :deep(h3) { font-size: 1.12em; margin: 1em 0 0.45em; font-weight: 700; }
.forum-article__body :deep(a) { color: #0f766e; text-decoration: none; }
.forum-article__body :deep(a:hover) { text-decoration: underline; }
.forum-article__body :deep(code) { background: #e2e8f0; padding: 0.18em 0.42em; border-radius: 6px; }
.forum-article__body :deep(pre) { background: #0f172a; color: #e2e8f0; padding: 1rem; border-radius: 14px; overflow: auto; }
.forum-article__body :deep(pre code) { background: none; padding: 0; color: inherit; }
.forum-article__body :deep(blockquote) { border-left: 4px solid #cbd5e1; padding-left: 1rem; color: #64748b; }
.forum-article__body :deep(table) { width: 100%; border-collapse: collapse; }
.forum-article__body :deep(th), .forum-article__body :deep(td) { border: 1px solid #e2e8f0; padding: 0.5rem 0.7rem; }

.forum-reply-panel {
  display: grid;
  gap: 0.9rem;
}

.forum-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.forum-panel__header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
}

.forum-reply-count {
  color: #64748b;
  font-size: 0.85rem;
}

.forum-lock-tip,
.forum-empty-copy {
  padding: 0.85rem 1rem;
  border-radius: 14px;
  background: #f8fafc;
  color: #475569;
}

.forum-accepted-box {
  border-radius: 18px;
  border: 1px solid rgba(22, 163, 74, 0.16);
  background: linear-gradient(135deg, rgba(220, 252, 231, 0.8), rgba(240, 253, 244, 0.94));
  padding: 0.95rem 1rem;
}

.forum-accepted-box__title {
  font-weight: 800;
  color: #166534;
  margin-bottom: 0.5rem;
}

.forum-accepted-box__content p {
  margin: 0.35rem 0 0;
  color: #14532d;
}

.forum-reply {
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 18px;
  background: #fff;
  padding: 0.95rem 1rem;
}

.forum-reply + .forum-reply {
  margin-top: 0.75rem;
}

.forum-reply--child {
  margin-top: 0.75rem;
  margin-left: 1.2rem;
  background: #f8fafc;
}

.forum-reply__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

@media (max-width: 768px) {
  .forum-reply__head {
    flex-direction: column;
  }
}

.forum-reply__meta {
  margin-left: 0.5rem;
  color: #94a3b8;
  font-size: 0.82rem;
}

.forum-reply__accepted {
  margin-left: 0.5rem;
}

.forum-reply__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.forum-reply__link {
  border: none;
  background: transparent;
  color: #0f766e;
  font-size: 0.85rem;
  font-weight: 700;
  padding: 0;
}

.forum-reply__link--accent {
  color: #166534;
}

.forum-reply__content {
  margin-top: 0.7rem;
  color: #1f2937;
  line-height: 1.75;
  white-space: pre-wrap;
}

.forum-reply__children {
  margin-top: 0.8rem;
}

.forum-reply-form {
  display: grid;
  gap: 0.75rem;
  padding-top: 0.5rem;
}

.forum-reply-form__header h3 {
  margin: 0;
}

.forum-reply-form__hint {
  margin: 0.25rem 0 0;
  color: #0f766e;
  font-size: 0.88rem;
}

.forum-reply-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.forum-detail-side {
  display: grid;
  gap: 1rem;
  align-self: start;
}

.forum-info-list {
  display: grid;
  gap: 0.65rem;
  margin: 0;
}

.forum-info-list div {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
}

.forum-info-list dt {
  color: #64748b;
}

.forum-info-list dd {
  margin: 0;
  font-weight: 700;
  color: #0f172a;
  text-align: right;
}

.forum-loading-panel {
  padding: 3rem 1rem;
  text-align: center;
  color: #64748b;
}
</style>
