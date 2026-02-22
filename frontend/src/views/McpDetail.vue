<template>
  <div class="mcp-detail pt-8">
    <div class="mcp-detail-container">
      <template v-if="item">
        <div class="mcp-detail-layout">
          <!-- 主内容区 -->
          <main class="mcp-detail-main">
            <header class="mcp-detail-header">
              <div class="mcp-detail-title-row">
                <img
                  v-if="item.logoUrl"
                  :src="item.logoUrl"
                  :alt="item.name"
                  class="mcp-detail-logo"
                />
                <div
                  v-else
                  class="mcp-detail-logo mcp-detail-logo-placeholder"
                >
                  {{ (item.name || '?')[0] }}
                </div>
                <div class="mcp-detail-title-block">
                  <h1 class="mcp-detail-title">{{ item.name }}</h1>
                  <div v-if="item.tagNames" class="mcp-detail-tags flex flex-wrap gap-2">
                    <span
                      v-for="tag in (item.tagNames || '').split(',').filter(Boolean)"
                      :key="tag"
                      class="tag-chip tag-chip--sm"
                    >
                      {{ tag.trim() }}
                    </span>
                  </div>
                  <a
                    v-if="item.url"
                    :href="item.url"
                    target="_blank"
                    rel="noopener nofollow"
                    class="mcp-detail-cta"
                  >
                    立即体验
                  </a>
                </div>
              </div>
            </header>

            <div v-if="item.summary" class="mcp-detail-summary">
              <p>{{ item.summary }}</p>
            </div>

            <section v-if="item.content" class="mcp-detail-content">
              <div class="markdown-body mcp-markdown" v-html="renderedContent" />
            </section>

            <section
              v-else-if="!item.summary && item.description"
              class="mcp-detail-content"
            >
              <p class="mcp-detail-fallback">{{ item.description }}</p>
            </section>
          </main>

          <!-- 侧边栏：MCP 推荐 -->
          <aside v-if="recommendedMcp.length" class="mcp-detail-sidebar">
            <h3 class="mcp-sidebar-title">MCP 推荐</h3>
            <div class="mcp-sidebar-list">
              <router-link
                v-for="t in recommendedMcp"
                :key="t.id"
                :to="'/mcp/' + t.id"
                class="mcp-sidebar-card"
              >
                <img
                  v-if="t.logoUrl"
                  :src="t.logoUrl"
                  :alt="t.name"
                  class="mcp-sidebar-card-logo"
                />
                <div
                  v-else
                  class="mcp-sidebar-card-logo mcp-sidebar-card-logo-placeholder"
                >
                  {{ (t.name || '?')[0] }}
                </div>
                <div class="mcp-sidebar-card-body">
                  <div class="mcp-sidebar-card-name">{{ t.name }}</div>
                  <p class="mcp-sidebar-card-desc">{{ t.summary || t.description || '' }}</p>
                </div>
              </router-link>
            </div>
          </aside>
        </div>

        <!-- 移动端：推荐 MCP 放底部 -->
        <section v-if="recommendedMcp.length" class="mcp-detail-recommend-mobile">
          <h2 class="mcp-section-title">MCP 推荐</h2>
          <div class="mcp-recommend-grid">
            <router-link
              v-for="t in recommendedMcp"
              :key="t.id"
              :to="'/mcp/' + t.id"
              class="mcp-recommend-card"
            >
              <img
                v-if="t.logoUrl"
                :src="t.logoUrl"
                :alt="t.name"
                class="mcp-recommend-card-logo"
              />
              <div
                v-else
                class="mcp-recommend-card-logo mcp-recommend-card-logo-placeholder"
              >
                {{ (t.name || '?')[0] }}
              </div>
              <div class="mcp-recommend-card-body">
                <div class="mcp-recommend-card-name">{{ t.name }}</div>
                <p class="mcp-recommend-card-desc">{{ t.summary || t.description || '' }}</p>
              </div>
            </router-link>
          </div>
        </section>
      </template>

      <template v-else>
        <p v-if="loading" class="mcp-loading">加载中…</p>
        <el-alert v-else type="error" :title="error || '未找到'" show-icon />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const route = useRoute()
const item = ref(null)
const recommendedMcp = ref([])

const renderedContent = computed(() => {
  const md = item.value?.content
  if (!md) return ''
  try {
    const html = marked.parse(md, { gfm: true })
    return DOMPurify.sanitize(html)
  } catch {
    return ''
  }
})
const loading = ref(true)
const error = ref('')

async function loadDetail() {
  try {
    item.value = await api.get('/mcp/' + route.params.id)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadRecommended() {
  if (!item.value?.id) return
  try {
    const params = { page: 1, size: 12 }
    const firstTag = (item.value.tagNames || '').split(',')[0]?.trim()
    if (firstTag) params.category = firstTag
    const data = await api.get('/mcp', { params })
    const list = (data.items || []).filter((t) => t.id !== item.value.id)
    recommendedMcp.value = list.slice(0, 6)
  } catch {
    recommendedMcp.value = []
  }
}

onMounted(async () => {
  await loadDetail()
  if (item.value) loadRecommended()
})

watch(() => route.params.id, async () => {
  loading.value = true
  error.value = ''
  item.value = null
  recommendedMcp.value = []
  await loadDetail()
  if (item.value) loadRecommended()
})
</script>

<style scoped>
.mcp-detail {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
}

.mcp-detail-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.back-link {
  display: inline-block;
  font-size: 14px;
  color: #2563eb;
  margin-bottom: 24px;
  text-decoration: none;
}
.back-link:hover { text-decoration: underline; }

/* 两栏布局：主内容 + 侧边栏 */
.mcp-detail-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  align-items: start;
}

@media (max-width: 1024px) {
  .mcp-detail-layout { grid-template-columns: 1fr; }
  .mcp-detail-sidebar { display: none; }
}

/* 头部：标题 + 标签 + 立即体验 */
.mcp-detail-header {
  margin-bottom: 24px;
}

.mcp-detail-title-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.mcp-detail-logo {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  object-fit: contain;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.mcp-detail-logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #9ca3af;
}

.mcp-detail-title-block { flex: 1; min-width: 0; }

.mcp-detail-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: -0.5px;
  margin: 0 0 12px 0;
  line-height: 1.3;
}

.mcp-detail-tags {
  margin-bottom: 12px;
}

.mcp-detail-cta {
  display: inline-flex;
  align-items: center;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  background: #2563eb;
  border-radius: 8px;
  text-decoration: none;
  transition: background 0.2s;
}
.mcp-detail-cta:hover {
  background: #1d4ed8;
}

/* 简介 */
.mcp-detail-summary {
  margin-bottom: 28px;
  padding: 16px 20px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.mcp-detail-summary p {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: #475569;
}

/* 主内容区 */
.mcp-detail-main {
  min-width: 0;
}

.mcp-detail-content {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 28px 32px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.mcp-detail-fallback {
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: #475569;
}

/* Markdown 样式（参考 ai.codefather.cn） */
.mcp-markdown {
  font-size: 15px;
  line-height: 1.75;
  color: #334155;
}

.markdown-body :deep(h1) { font-size: 1.5em; margin: 1.5em 0 0.6em; font-weight: 600; color: #1f2937; }
.markdown-body :deep(h2) { font-size: 1.25em; margin: 1.35em 0 0.5em; font-weight: 600; color: #1f2937; padding-bottom: 0.3em; border-bottom: 1px solid #e5e7eb; }
.markdown-body :deep(h3) { font-size: 1.1em; margin: 1.2em 0 0.5em; font-weight: 600; color: #1f2937; }
.markdown-body :deep(p) { margin: 0.8em 0; line-height: 1.7; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 0.8em 0; padding-left: 1.6em; }
.markdown-body :deep(li) { margin: 0.35em 0; }
.markdown-body :deep(code) { background: #f1f5f9; padding: 0.2em 0.45em; border-radius: 5px; font-size: 0.9em; color: #475569; }
.markdown-body :deep(pre) { background: #1e293b; color: #e2e8f0; padding: 1em 1.25em; border-radius: 8px; overflow-x: auto; margin: 1.2em 0; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; font-size: 0.9em; }
.markdown-body :deep(blockquote) { border-left: 4px solid #cbd5e1; margin: 1em 0; padding-left: 1em; color: #64748b; }
.markdown-body :deep(a) { color: #2563eb; text-decoration: none; }
.markdown-body :deep(a:hover) { text-decoration: underline; }
.markdown-body :deep(table) { border-collapse: collapse; width: 100%; margin: 1.2em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #e2e8f0; padding: 0.5em 0.75em; text-align: left; }
.markdown-body :deep(th) { background: #f8fafc; font-weight: 600; }

/* 侧边栏 */
.mcp-detail-sidebar {
  position: sticky;
  top: 24px;
}

.mcp-sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 16px 0;
}

.mcp-sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mcp-sidebar-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.mcp-sidebar-card:hover {
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
}

.mcp-sidebar-card-logo {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: contain;
  background: #f9fafb;
  flex-shrink: 0;
}

.mcp-sidebar-card-logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #9ca3af;
}

.mcp-sidebar-card-body { min-width: 0; flex: 1; }

.mcp-sidebar-card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mcp-sidebar-card-desc {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 移动端推荐区域 */
.mcp-detail-recommend-mobile {
  display: none;
  margin-top: 40px;
}

@media (max-width: 1024px) {
  .mcp-detail-recommend-mobile { display: block; }
}

.mcp-section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 20px 0;
}

.mcp-recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.mcp-recommend-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.mcp-recommend-card:hover {
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
}

.mcp-recommend-card-logo {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: contain;
  background: #f9fafb;
  flex-shrink: 0;
}

.mcp-recommend-card-logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  color: #9ca3af;
}

.mcp-recommend-card-body { min-width: 0; flex: 1; }

.mcp-recommend-card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 6px;
}

.mcp-recommend-card-desc {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mcp-loading {
  color: #6b7280;
  padding: 48px 0;
  text-align: center;
}
</style>
