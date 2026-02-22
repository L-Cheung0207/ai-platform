<template>
  <div class="mcp-list-page">
    <PageHero
      variant="teal"
      title="发现全球好用的 MCP 服务器"
      subtitle="持续更新优质 MCP，重塑你的 AI 工作流"
    >
      <div class="global-search mx-auto mb-4">
        <el-input
          v-model="keyword"
          placeholder="搜索 MCP 服务器"
          maxlength="200"
          size="large"
          clearable
          @keyup.enter="search"
        />
        <el-button type="primary" size="large" @click="search">搜索</el-button>
      </div>
      <div class="mcp-categories">
        <button
          v-for="c in allCategoryOptions"
          :key="c.value"
          :class="[
            'mcp-category-btn',
            selectedCategory === c.value ? 'mcp-category-btn-active' : ''
          ]"
          @click="selectCategory(c.value)"
        >
          {{ c.label }}
        </button>
      </div>
    </PageHero>

    <!-- 列表区域：扁平分页网格 -->
    <div class="mcp-content">
      <div v-if="loading" class="mcp-loading">
        <div v-for="i in 2" :key="i" class="space-y-4">
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            <div v-for="j in 8" :key="j" class="mcp-card-skeleton" />
          </div>
        </div>
      </div>

      <template v-else>
        <div v-if="items.length === 0" class="mcp-empty">
          {{ keyword ? '未找到匹配的 MCP' : '暂无 MCP 服务器' }}
        </div>
        <div v-else class="mcp-grid">
          <router-link
            v-for="t in items"
            :key="t.id"
            :to="'/mcp/' + t.id"
            class="mcp-card"
          >
            <img
              v-if="t.logoUrl"
              :src="t.logoUrl"
              :alt="t.name"
              class="mcp-card-logo"
            />
            <div v-else class="mcp-card-logo mcp-card-logo-placeholder">
              {{ (t.name || '?')[0] }}
            </div>
            <div class="mcp-card-body">
              <div class="mcp-card-title">{{ t.name }}</div>
              <span v-if="firstTag(t)" class="tag-chip tag-chip--sm mcp-card-tag">{{ firstTag(t) }}</span>
              <p class="mcp-card-desc">{{ t.summary || t.description || '暂无描述' }}</p>
            </div>
          </router-link>
        </div>

        <div class="mcp-pagination">
          <el-pagination
            v-if="total > 0 && !loading"
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[12, 24, 48, 96]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="loadList"
            @size-change="onSizeChange"
          />
        </div>
      </template>

      <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
    </div>

    <!-- FAQ 常见问题 -->
    <section class="mcp-faq">
      <div class="mcp-faq-inner">
        <h2 class="mcp-faq-title">
          <span class="mcp-faq-title-icon" aria-hidden="true">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </span>
          常见问题
        </h2>
        <dl class="mcp-faq-grid">
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">1. 什么是 MCP 服务器？</dt>
            <dd class="mcp-faq-a">MCP（模型上下文协议）服务器是连接 AI 助手（如 Claude、Cursor 等）与外部数据源、API 及服务的工具。它们使 AI 模型能够获取实时信息、执行代码、与数据库交互，并实现超越其基础能力范围的操作。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">2. MCP 服务器是如何工作的？</dt>
            <dd class="mcp-faq-a">MCP 服务器充当人工智能助手与外部系统之间的中介。当您要求 Claude 或 Cursor 执行需要外部数据的任务时，MCP 服务器会处理请求，获取所需数据，并将其转换为 AI 能够理解和使用的格式返回。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">3. MCP 服务器能提供什么？</dt>
            <dd class="mcp-faq-a">MCP 可实现各类资源的共享（包括文件、文档、数据），开放各类工具能力（如 API 集成、操作指令调用），还能提供标准化的交互提示模板；服务器端可自主管控自有资源，同时为保障安全，会搭建清晰的系统边界，做到权限与隔离可控。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">4. MCP 服务器安全吗？</dt>
            <dd class="mcp-faq-a">是的，MCP 协议内置了完善的安全机制。服务器能够独立管理自身资源，因此无需向大型语言模型提供商共享 API 密钥，整个系统界限清晰。每个服务器均自行负责身份验证和访问控制，确保操作安全隔离。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">5. 如何在 Cursor 中安装 MCP 服务器？</dt>
            <dd class="mcp-faq-a">要在 Cursor 中安装 MCP 服务器：1) 打开 Cursor 设置 (Cmd+,)，2) 导航至 MCP 服务器部分，3) 点击「添加服务器」，4) 输入服务器配置（名称和命令），5) 重启 Cursor。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">6. 如何选择合适的 MCP 服务器？</dt>
            <dd class="mcp-faq-a">建议重点关注：1）兼容性：确保服务器能与您常用的编辑器顺畅协作；2）匹配应用场景：根据具体需求选择为此类任务专门设计的服务器；3）关注维护状态：优先选择正在活跃维护、并有近期更新记录的服务器；4）参考社区热度：在 GitHub 上，较高的星标数和频繁的提交更新往往是项目质量的体现。</dd>
          </div>
        </dl>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(24)
const keyword = ref('')
const selectedCategory = ref('')
const tags = ref([])
const loading = ref(false)
const error = ref('')

/** 是否含中文（用于仅展示中文 tag） */
function isChineseTag(s) {
  if (!s || typeof s !== 'string') return false
  return /[\u4e00-\u9fff]/.test(s.trim())
}

const allCategoryOptions = computed(() => {
  const opts = [{ label: '全部', value: '' }]
  const chinese = tags.value.filter(isChineseTag).slice(0, 10)
  for (const t of chinese) opts.push({ label: t, value: t })
  return opts
})

function firstTag(t) {
  const parts = (t.tagNames || '').split(',').map((s) => s.trim()).filter(Boolean)
  const chinese = parts.find(isChineseTag)
  return chinese || null
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (selectedCategory.value) params.category = selectedCategory.value
    const data = await api.get('/mcp', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    tags.value = await api.get('/mcp/tags')
  } catch (_) {
    tags.value = []
  }
}

function selectCategory(val) {
  selectedCategory.value = val
}

function search() {
  page.value = 1
  loadList()
}

function onSizeChange() {
  page.value = 1
  loadList()
}

watch(selectedCategory, () => {
  page.value = 1
  loadList()
})

onMounted(() => {
  loadTags()
  loadList()
})
</script>

<style scoped>
.mcp-categories {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.5rem;
}
.mcp-category-btn {
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
}
.mcp-category-btn:hover {
  border-color: rgb(37 99 235);
  color: rgb(37 99 235);
}
.mcp-category-btn-active {
  background: rgb(37 99 235);
  color: #fff;
  border-color: rgb(37 99 235);
  box-shadow: 0 4px 6px -1px rgba(37 99 235, 0.2);
}

.mcp-list-page {
  overflow-x: hidden;
  box-sizing: border-box;
}
.mcp-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 1.5rem;
  min-width: 0;
  box-sizing: border-box;
}
.mcp-loading .mcp-card-skeleton {
  height: 120px;
  border-radius: 12px;
  background: linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%);
  background-size: 200% 100%;
  animation: mcp-skeleton 1.5s ease-in-out infinite;
}
@keyframes mcp-skeleton {
  to { background-position: 200% 0; }
}
.mcp-empty {
  text-align: center;
  color: #6b7280;
  padding: 3rem 1rem;
}
.mcp-grid {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 1rem;
  min-width: 0;
}
@media (min-width: 640px) {
  .mcp-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (min-width: 1024px) {
  .mcp-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (min-width: 1280px) {
  .mcp-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}
.mcp-card {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  min-width: 0;
  min-height: 120px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.2s;
  overflow: hidden;
}
.mcp-card:hover {
  border-color: rgb(37 99 235);
  box-shadow: 0 4px 12px rgba(37 99 235, 0.1);
}
.mcp-card-logo {
  width: 48px;
  height: 48px;
  min-width: 48px;
  border-radius: 8px;
  object-fit: contain;
  flex-shrink: 0;
  background: #f9fafb;
}
.mcp-card-logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 1.25rem;
  font-weight: 700;
}
.mcp-card-body {
  min-width: 0;
  flex: 1;
  overflow: hidden;
}
.mcp-card-title {
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mcp-card-tag {
  margin-top: 0.25rem;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mcp-card-desc {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  margin-top: 0.25rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.mcp-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 2rem;
}
.mcp-pagination :deep(.el-pagination) {
  font-weight: 500;
}

/* FAQ 常见问题 */
.mcp-faq {
  background: #fff;
  border-top: 1px solid #e5e7eb;
  padding: 3.5rem 1.5rem;
}
.mcp-faq-inner {
  max-width: 960px;
  margin: 0 auto;
}
.mcp-faq-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-size: 1.375rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2rem;
  text-align: center;
}
.mcp-faq-title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.1);
  color: rgb(37, 99, 235);
}
.mcp-faq-title-icon svg {
  width: 18px;
  height: 18px;
}
.mcp-faq-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}
@media (min-width: 640px) {
  .mcp-faq-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1.25rem;
  }
}
.mcp-faq-card {
  padding: 1.25rem 1.5rem;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}
.mcp-faq-card:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.mcp-faq-q {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #334155;
  margin: 0 0 0.5rem;
  line-height: 1.4;
}
.mcp-faq-a {
  margin: 0;
  font-size: 0.875rem;
  color: #64748b;
  line-height: 1.65;
}
</style>
