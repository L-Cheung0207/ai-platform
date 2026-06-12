<template>
  <div class="llm-admin">
    <header class="llm-admin__header">
      <div>
        <h1>LLM 排行榜</h1>
        <p>同步 OpenLM Chatbot Arena 数据，维护公开编程模型榜单</p>
      </div>
      <div class="llm-admin__actions">
        <el-button :loading="statusLoading" @click="refreshAll">刷新</el-button>
        <router-link to="/llm-leaderboard" class="el-button el-button--default">查看公开榜单</router-link>
      </div>
    </header>

    <div class="llm-admin__stats">
      <article class="stat-card">
        <span class="stat-card__label">榜单模型数</span>
        <strong class="stat-card__value">{{ totalModels }}</strong>
        <span class="stat-card__hint">当前库内记录</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">同步状态</span>
        <strong class="stat-card__value stat-card__value--sm">
          <span class="status-pill" :class="'status-pill--' + statusTone">{{ statusLabel }}</span>
        </strong>
        <span class="stat-card__hint">{{ statusHint }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">上次同步</span>
        <strong class="stat-card__value stat-card__value--sm">{{ lastSyncText }}</strong>
        <span class="stat-card__hint">全量替换写入</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">上次写入</span>
        <strong class="stat-card__value">{{ lastAddedCount }}</strong>
        <span class="stat-card__hint">条模型记录</span>
      </article>
    </div>

    <div class="llm-admin__grid">
      <section class="sync-panel">
        <div class="panel-head">
          <div>
            <h2>数据同步</h2>
            <p>从 OpenLM Chatbot Arena 拉取最新排行榜</p>
          </div>
          <a
            href="https://openlm.ai/chatbot-arena/"
            target="_blank"
            rel="noopener"
            class="source-link"
          >
            数据来源
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
              <polyline points="15 3 21 3 21 9" />
              <line x1="10" y1="14" x2="21" y2="3" />
            </svg>
          </a>
        </div>

        <p class="sync-panel__desc">
          爬取为<strong>全量替换</strong>：先清空现有数据，再写入 OpenLM 最新榜单。涵盖 Arena Elo、Coding、Vision、AAII、MMLU-Pro、ARC-AGI 等指标。
        </p>

        <div class="metric-tags">
          <span v-for="tag in metricTags" :key="tag" class="metric-tag">{{ tag }}</span>
        </div>

        <div class="sync-panel__actions">
          <el-button
            type="primary"
            size="large"
            :loading="scraping"
            :disabled="statusData?.status === 'running'"
            @click="startScrape"
          >
            {{ statusData?.status === 'running' ? '同步进行中…' : '一键同步榜单' }}
          </el-button>
          <p v-if="statusData?.status === 'running'" class="sync-panel__running">
            <span class="sync-dot" aria-hidden="true" />
            正在从 OpenLM 拉取数据，通常需要 1–3 分钟
          </p>
        </div>

        <el-alert
          v-if="statusData?.error"
          type="error"
          :title="statusData.error"
          show-icon
          :closable="false"
          class="sync-panel__alert"
        />
      </section>

      <section class="info-panel">
        <h2>指标说明</h2>
        <ul class="info-list">
          <li v-for="item in metricDescriptions" :key="item.label">
            <span class="info-list__label">{{ item.label }}</span>
            <p>{{ item.text }}</p>
          </li>
        </ul>
      </section>
    </div>

    <section class="preview-panel">
      <div class="preview-panel__head">
        <div>
          <h2>榜单预览</h2>
          <p>按编程能力排序，展示前 {{ previewSize }} 名</p>
        </div>
        <router-link to="/llm-leaderboard" class="preview-link">查看完整榜单 →</router-link>
      </div>

      <div v-if="previewLoading" class="preview-skeleton">
        <div v-for="i in 6" :key="i" class="preview-skeleton__row" />
      </div>

      <div v-else-if="previewItems.length" class="preview-table-wrap">
        <table class="preview-table">
          <thead>
            <tr>
              <th>排名</th>
              <th>模型</th>
              <th>编程</th>
              <th>Elo</th>
              <th>机构</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in previewItems" :key="row.id">
              <td>
                <span class="rank-badge" :class="rankClass(idx)">{{ idx + 1 }}</span>
              </td>
              <td>
                <span v-if="row.rankBadge" class="model-badge" :title="row.rankBadge">{{ row.rankBadge }}</span>
                <a
                  v-if="row.modelUrl"
                  :href="row.modelUrl"
                  target="_blank"
                  rel="noopener"
                  class="model-link"
                >{{ row.modelName }}</a>
                <span v-else class="model-name">{{ row.modelName }}</span>
              </td>
              <td class="num-cell">{{ formatNum(row.coding) }}</td>
              <td class="num-cell num-cell--muted">{{ formatNum(row.arenaElo) }}</td>
              <td class="org-cell">{{ row.organization || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="preview-empty">
        <p>暂无榜单数据</p>
        <span>点击「一键同步榜单」从 OpenLM 拉取最新数据</span>
      </div>
    </section>

    <el-alert v-if="error" type="error" :title="error" show-icon />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'

const previewSize = 10
const metricTags = ['Arena Elo', 'Coding', 'Vision', 'AAII', 'MMLU-Pro', 'ARC-AGI']
const metricDescriptions = [
  { label: '竞技场 Elo', text: 'Chatbot Arena 众包对战评分，反映用户偏好下的相对实力。' },
  { label: '编程', text: '编程专项竞技场评分，衡量代码生成与编程任务表现。' },
  { label: '视觉', text: '视觉专项竞技场评分，衡量图像理解与视觉任务表现。' },
  { label: 'AAII', text: '人工分析智能指数，聚合 10 项挑战性评估的综合指数。' },
  { label: 'MMLU-Pro', text: '多任务语言理解与推理的进阶基准测试。' },
  { label: 'ARC-AGI', text: '通用人工智能推理基准，考察流体智力与推理能力。' },
]

const statusData = ref(null)
const statusLoading = ref(false)
const scraping = ref(false)
const error = ref('')
const totalModels = ref(0)
const previewItems = ref([])
const previewLoading = ref(false)
let pollTimerId = null
let pollTimeoutId = null

const statusLabel = computed(() => {
  const status = statusData.value?.status
  if (!status) return '未同步'
  if (status === 'running') return '同步中'
  if (status === 'completed') return '已完成'
  if (status === 'failed') return '失败'
  return status
})

const statusTone = computed(() => {
  const status = statusData.value?.status
  if (status === 'running') return 'running'
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  return 'idle'
})

const statusHint = computed(() => {
  const status = statusData.value?.status
  if (status === 'running') return '后台任务执行中'
  if (status === 'completed') return '数据已写入数据库'
  if (status === 'failed') return '请查看错误信息后重试'
  return '尚未执行过同步'
})

const lastSyncText = computed(() => {
  const raw = statusData.value?.finishedAt || previewItems.value[0]?.scrapedAt
  return raw ? formatDateTime(raw) : '—'
})

const lastAddedCount = computed(() => {
  const added = statusData.value?.added
  return added != null ? added : '—'
})

function formatNum(value) {
  if (value == null) return '—'
  return Number(value)
}

function formatDateTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function rankClass(index) {
  if (index === 0) return 'rank-badge--gold'
  if (index === 1) return 'rank-badge--silver'
  if (index === 2) return 'rank-badge--bronze'
  return ''
}

function clearPollTimers() {
  if (pollTimerId) {
    clearInterval(pollTimerId)
    pollTimerId = null
  }
  if (pollTimeoutId) {
    clearTimeout(pollTimeoutId)
    pollTimeoutId = null
  }
}

async function fetchStatus() {
  statusLoading.value = true
  try {
    statusData.value = await api.get('/admin/scrape-status/llm-leaderboard')
  } catch {
    statusData.value = null
  } finally {
    statusLoading.value = false
  }
}

async function loadPreview() {
  previewLoading.value = true
  try {
    const data = await api.get('/llm-leaderboard', {
      params: { page: 1, size: previewSize, sortBy: 'coding', sortOrder: 'desc' },
    })
    previewItems.value = data.items || []
    totalModels.value = data.total ?? 0
  } catch (e) {
    previewItems.value = []
    totalModels.value = 0
    error.value = e.message || '榜单预览加载失败'
  } finally {
    previewLoading.value = false
  }
}

async function refreshAll() {
  error.value = ''
  await Promise.all([fetchStatus(), loadPreview()])
}

async function startScrape() {
  scraping.value = true
  error.value = ''
  clearPollTimers()
  try {
    await api.post('/admin/scrape/llm-leaderboard')
    statusData.value = { status: 'running' }
    ElMessage.success('同步任务已启动')
    await fetchStatus()

    pollTimerId = setInterval(async () => {
      await fetchStatus()
      if (statusData.value?.status === 'completed' || statusData.value?.status === 'failed') {
        clearPollTimers()
        scraping.value = false
        if (statusData.value?.status === 'completed') {
          ElMessage.success(`同步完成，共写入 ${statusData.value.added ?? 0} 条`)
          await loadPreview()
        } else {
          ElMessage.error('同步失败')
        }
      }
    }, 2500)

    pollTimeoutId = setTimeout(() => {
      clearPollTimers()
      scraping.value = false
    }, 180000)
  } catch (e) {
    scraping.value = false
    error.value = e.message || '启动同步失败'
    ElMessage.error(error.value)
  }
}

onMounted(refreshAll)
onBeforeUnmount(clearPollTimers)
</script>

<style scoped>
.llm-admin {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.llm-admin__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.llm-admin__header h1 {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
}

.llm-admin__header p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.llm-admin__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.llm-admin__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fafb 100%);
  padding: 16px 18px;
}

.stat-card__label {
  display: block;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 8px;
}

.stat-card__value {
  display: block;
  color: #111827;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}

.stat-card__value--sm {
  font-size: 16px;
  font-weight: 600;
}

.stat-card__hint {
  display: block;
  margin-top: 8px;
  color: #9ca3af;
  font-size: 12px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 13px;
  font-weight: 600;
}

.status-pill--idle {
  background: #f3f4f6;
  color: #6b7280;
}

.status-pill--running {
  background: #fef3c7;
  color: #b45309;
}

.status-pill--success {
  background: #d1fae5;
  color: #047857;
}

.status-pill--danger {
  background: #fee2e2;
  color: #b91c1c;
}

.llm-admin__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr);
  gap: 16px;
}

.sync-panel,
.info-panel,
.preview-panel {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.sync-panel {
  padding: 22px 24px;
  background:
    radial-gradient(ellipse 80% 60% at 100% 0%, rgba(20, 184, 166, 0.08), transparent 70%),
    #fff;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2,
.preview-panel__head h2,
.info-panel h2 {
  margin: 0 0 4px;
  color: #1f2937;
  font-size: 16px;
  font-weight: 700;
}

.panel-head p,
.preview-panel__head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.source-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #0d9488;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  white-space: nowrap;
}

.source-link svg {
  width: 14px;
  height: 14px;
}

.source-link:hover {
  color: #0f766e;
  text-decoration: underline;
}

.sync-panel__desc {
  margin: 0 0 14px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.65;
}

.sync-panel__desc strong {
  color: #111827;
}

.metric-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.metric-tag {
  border: 1px solid #ccfbf1;
  border-radius: 999px;
  background: #f0fdfa;
  color: #0f766e;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
}

.sync-panel__actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.sync-panel__running {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #b45309;
  font-size: 13px;
}

.sync-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f59e0b;
  animation: sync-pulse 1.2s ease-in-out infinite;
}

@keyframes sync-pulse {
  0%, 100% { opacity: 0.4; transform: scale(0.9); }
  50% { opacity: 1; transform: scale(1.1); }
}

.sync-panel__alert {
  margin-top: 16px;
}

.info-panel {
  padding: 20px 22px;
}

.info-list {
  list-style: none;
  margin: 14px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-list__label {
  display: block;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 2px;
}

.info-list p {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.55;
}

.preview-panel {
  overflow: hidden;
}

.preview-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid #f3f4f6;
  background: #fafafa;
}

.preview-link {
  color: #0d9488;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  white-space: nowrap;
}

.preview-link:hover {
  text-decoration: underline;
}

.preview-table-wrap {
  overflow-x: auto;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
}

.preview-table th {
  padding: 12px 18px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  text-align: left;
  background: #fff;
  border-bottom: 1px solid #f3f4f6;
}

.preview-table td {
  padding: 14px 18px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
}

.preview-table tbody tr:hover {
  background: #f9fafb;
}

.preview-table tbody tr:last-child td {
  border-bottom: none;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 13px;
  font-weight: 700;
}

.rank-badge--gold {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #92400e;
}

.rank-badge--silver {
  background: linear-gradient(135deg, #f3f4f6, #e5e7eb);
  color: #4b5563;
}

.rank-badge--bronze {
  background: linear-gradient(135deg, #ffedd5, #fed7aa);
  color: #9a3412;
}

.model-badge {
  margin-right: 4px;
}

.model-link {
  color: #0d9488;
  font-weight: 600;
  text-decoration: none;
}

.model-link:hover {
  text-decoration: underline;
}

.model-name {
  color: #1f2937;
  font-weight: 600;
}

.num-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.num-cell--muted {
  color: #6b7280;
  font-weight: 500;
}

.org-cell {
  color: #4b5563;
  font-size: 13px;
}

.preview-empty {
  padding: 48px 24px;
  text-align: center;
}

.preview-empty p {
  margin: 0 0 6px;
  color: #374151;
  font-size: 15px;
  font-weight: 600;
}

.preview-empty span {
  color: #9ca3af;
  font-size: 13px;
}

.preview-skeleton {
  padding: 8px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-skeleton__row {
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease infinite;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 1080px) {
  .llm-admin__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .llm-admin__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .llm-admin__header,
  .preview-panel__head {
    flex-direction: column;
  }

  .llm-admin__actions {
    width: 100%;
  }

  .llm-admin__actions :deep(.el-button),
  .llm-admin__actions .el-button {
    flex: 1;
  }

  .llm-admin__stats {
    grid-template-columns: 1fr;
  }

  .sync-panel__actions :deep(.el-button) {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .sync-dot,
  .preview-skeleton__row {
    animation: none;
  }
}
</style>
