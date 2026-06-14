<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'

const periods = [
  { label: '周榜', value: 'WEEKLY' },
  { label: '月榜', value: 'MONTHLY' },
]

const period = ref('WEEKLY')
const statusData = ref(null)
const statusLoading = ref(false)
const configLoading = ref(false)
const configSaving = ref(false)
const tableLoading = ref(false)
const syncStarting = ref(false)
const error = ref('')
const rows = ref([])
const savingIds = ref(new Set())
const regeneratingIds = ref(new Set())
const editDrafts = reactive({})
const configForm = reactive({
  languageFilter: '',
  keywordFilter: '',
  homeDisplayCount: 10,
})

let pollTimerId = null
let pollTimeoutId = null

const isRunning = computed(() => normalizeStatus(statusData.value?.status) === 'RUNNING')

const statusLabel = computed(() => {
  const status = normalizeStatus(statusData.value?.status)
  if (status === 'RUNNING') return '同步中'
  if (status === 'COMPLETED') return '已完成'
  if (status === 'FAILED') return '失败'
  return '未同步'
})

const statusTone = computed(() => {
  const status = normalizeStatus(statusData.value?.status)
  if (status === 'RUNNING') return 'running'
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'idle'
})

const statusHint = computed(() => {
  if (isRunning.value) return '后台任务仍在执行，请稍后刷新'
  if (normalizeStatus(statusData.value?.status) === 'FAILED') return '请查看错误信息后重试'
  if (statusData.value?.finishedAt) return `完成于 ${formatDateTime(statusData.value.finishedAt)}`
  return '尚未记录同步完成时间'
})

const latestBatchText = computed(() => {
  const raw = period.value === 'WEEKLY'
    ? statusData.value?.latestWeeklyBatch
    : statusData.value?.latestMonthlyBatch
  return formatDateTime(raw)
})

const currentPeriodLabel = computed(() => {
  return periods.find((item) => item.value === period.value)?.label || '榜单'
})

function normalizeStatus(status) {
  return status ? String(status).toUpperCase() : ''
}

function formatDateTime(value) {
  if (!value) return '-'
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

function formatNumber(value) {
  if (value == null) return '-'
  return Number(value).toLocaleString('zh-CN')
}

function summaryType(status) {
  const normalized = normalizeStatus(status)
  if (normalized === 'MANUAL') return 'success'
  if (normalized === 'FAILED') return 'danger'
  if (normalized === 'NEEDS_REVIEW') return 'warning'
  return 'info'
}

function summaryLabel(status) {
  const normalized = normalizeStatus(status)
  if (normalized === 'MANUAL') return '人工'
  if (normalized === 'GENERATED') return '生成'
  if (normalized === 'NEEDS_REVIEW') return '待审'
  if (normalized === 'FAILED') return '失败'
  return '未知'
}

function rowDraft(row) {
  if (!editDrafts[row.id]) {
    editDrafts[row.id] = {
      effectCn: row.effectCn || '',
      scenarioCn: row.scenarioCn || '',
    }
  }
  return editDrafts[row.id]
}

function syncDrafts(items) {
  const ids = new Set(items.map((item) => item.id))
  Object.keys(editDrafts).forEach((id) => {
    if (!ids.has(Number(id))) {
      delete editDrafts[id]
    }
  })
  items.forEach((item) => {
    editDrafts[item.id] = {
      effectCn: item.effectCn || '',
      scenarioCn: item.scenarioCn || '',
    }
  })
}

function setSaving(id, value) {
  const next = new Set(savingIds.value)
  if (value) next.add(id)
  else next.delete(id)
  savingIds.value = next
}

function setRegenerating(id, value) {
  const next = new Set(regeneratingIds.value)
  if (value) next.add(id)
  else next.delete(id)
  regeneratingIds.value = next
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

function startStatusPolling() {
  clearPollTimers()
  pollTimerId = setInterval(async () => {
    await fetchStatus()
    if (!isRunning.value) {
      clearPollTimers()
      syncStarting.value = false
      await Promise.all([loadRows(), loadConfig()])
      if (normalizeStatus(statusData.value?.status) === 'COMPLETED') {
        ElMessage.success('GitHub Trending 同步已完成')
      }
    }
  }, 3000)

  pollTimeoutId = setTimeout(() => {
    clearPollTimers()
    syncStarting.value = false
  }, 180000)
}

async function fetchStatus() {
  statusLoading.value = true
  try {
    statusData.value = await api.get('/admin/github-trending/status')
  } catch (e) {
    error.value = e.message || '同步状态加载失败'
  } finally {
    statusLoading.value = false
  }
}

async function loadConfig() {
  configLoading.value = true
  try {
    const data = await api.get('/admin/github-trending/config')
    configForm.languageFilter = data.languageFilter || ''
    configForm.keywordFilter = data.keywordFilter || ''
    configForm.homeDisplayCount = data.homeDisplayCount || 10
  } catch (e) {
    error.value = e.message || '配置加载失败'
  } finally {
    configLoading.value = false
  }
}

async function loadRows() {
  tableLoading.value = true
  try {
    const data = await api.get('/admin/github-trending', {
      params: { period: period.value },
    })
    rows.value = Array.isArray(data) ? data : []
    syncDrafts(rows.value)
  } catch (e) {
    rows.value = []
    error.value = e.message || '榜单加载失败'
  } finally {
    tableLoading.value = false
  }
}

async function refreshAll() {
  error.value = ''
  await Promise.all([fetchStatus(), loadConfig(), loadRows()])
  if (isRunning.value) startStatusPolling()
}

async function saveConfig() {
  configSaving.value = true
  try {
    await api.put('/admin/github-trending/config', {
      languageFilter: configForm.languageFilter || null,
      keywordFilter: configForm.keywordFilter || null,
      homeDisplayCount: configForm.homeDisplayCount,
    })
    ElMessage.success('配置已保存')
    await loadConfig()
  } catch (e) {
    ElMessage.error(e.message || '配置保存失败')
  } finally {
    configSaving.value = false
  }
}

async function startSync() {
  syncStarting.value = true
  try {
    statusData.value = await api.post('/admin/github-trending/sync')
    ElMessage.success('同步任务已启动')
    await fetchStatus()
    if (isRunning.value) startStatusPolling()
  } catch (e) {
    ElMessage.error(e.message || '启动同步失败')
    await fetchStatus()
  } finally {
    if (!isRunning.value) syncStarting.value = false
  }
}

async function saveRow(row) {
  const draft = rowDraft(row)
  setSaving(row.id, true)
  try {
    const updated = await api.put(`/admin/github-trending/${row.id}`, {
      effectCn: draft.effectCn,
      scenarioCn: draft.scenarioCn,
    })
    Object.assign(row, updated)
    syncDrafts(rows.value)
    ElMessage.success('条目已保存')
  } catch (e) {
    ElMessage.error(e.message || '条目保存失败')
  } finally {
    setSaving(row.id, false)
  }
}

async function regenerateRow(row) {
  try {
    await ElMessageBox.confirm(
      `重新生成会覆盖 ${row.repoFullName} 当前中文摘要，确认继续？`,
      '重生成摘要',
      { type: 'warning', confirmButtonText: '重生成', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  setRegenerating(row.id, true)
  try {
    const updated = await api.post(`/admin/github-trending/${row.id}/regenerate-summary`)
    Object.assign(row, updated)
    syncDrafts(rows.value)
    ElMessage.success('摘要已重生成')
  } catch (e) {
    ElMessage.error(e.message || '重生成失败')
  } finally {
    setRegenerating(row.id, false)
  }
}

watch(period, loadRows)
onMounted(refreshAll)
onBeforeUnmount(clearPollTimers)
</script>

<template>
  <div class="github-trending-admin">
    <header class="github-trending-admin__header">
      <div>
        <h1>GitHub Trending</h1>
        <p>维护首页 GitHub 趋势项目的抓取配置、中文摘要和展示内容</p>
      </div>
      <div class="github-trending-admin__actions">
        <el-button :loading="statusLoading || tableLoading" @click="refreshAll">刷新</el-button>
        <el-button
          type="primary"
          :loading="syncStarting"
          :disabled="isRunning"
          @click="startSync"
        >
          {{ isRunning ? '同步进行中' : '手动同步' }}
        </el-button>
      </div>
    </header>

    <div class="github-trending-admin__stats">
      <article class="stat-card">
        <span class="stat-card__label">同步状态</span>
        <strong class="stat-card__value stat-card__value--sm">
          <span class="status-pill" :class="'status-pill--' + statusTone">{{ statusLabel }}</span>
        </strong>
        <span class="stat-card__hint">{{ statusHint }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">当前榜单</span>
        <strong class="stat-card__value">{{ rows.length }}</strong>
        <span class="stat-card__hint">{{ currentPeriodLabel }} 条目</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">最近批次</span>
        <strong class="stat-card__value stat-card__value--sm">{{ latestBatchText }}</strong>
        <span class="stat-card__hint">{{ currentPeriodLabel }}数据更新时间</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">自动刷新</span>
        <strong class="stat-card__value stat-card__value--sm">每天 08:00</strong>
        <span class="stat-card__hint">后端固定计划任务</span>
      </article>
    </div>

    <el-alert
      v-if="statusData?.error"
      type="error"
      :title="statusData.error"
      show-icon
      :closable="false"
    />
    <el-alert
      v-if="error"
      type="error"
      :title="error"
      show-icon
      class="github-trending-admin__alert"
      @close="error = ''"
    />

    <section class="config-panel" v-loading="configLoading">
      <div class="panel-head">
        <div>
          <h2>抓取配置</h2>
          <p>筛选语言、关键词和首页展示数量；刷新时间由后端固定执行</p>
        </div>
      </div>

      <el-form :model="configForm" label-width="110px" class="config-form">
        <el-form-item label="语言筛选">
          <el-input v-model="configForm.languageFilter" placeholder="例如 JavaScript、Python；留空为全部语言" clearable />
        </el-form-item>
        <el-form-item label="关键词筛选">
          <el-input
            v-model="configForm.keywordFilter"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="按仓库名或描述筛选；留空为不过滤"
          />
        </el-form-item>
        <el-form-item label="首页数量">
          <el-input-number v-model="configForm.homeDisplayCount" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="刷新计划">
          <el-input model-value="每天 08:00" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="configSaving" @click="saveConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <div class="table-panel__head">
        <div>
          <h2>榜单内容</h2>
          <p>编辑中文作用和使用场景，或对单条记录重新生成摘要</p>
        </div>
        <el-segmented v-model="period" :options="periods" />
      </div>

      <el-table v-loading="tableLoading" :data="rows" row-key="id" class="trending-table">
        <el-table-column prop="rank" label="#" width="64" fixed />
        <el-table-column label="仓库" min-width="260" fixed>
          <template #default="{ row }">
            <a
              v-if="row.repoUrl"
              :href="row.repoUrl"
              target="_blank"
              rel="noopener"
              class="repo-link"
            >
              {{ row.repoFullName }}
            </a>
            <strong v-else class="repo-name">{{ row.repoFullName }}</strong>
            <p class="repo-desc">{{ row.description || '暂无描述' }}</p>
            <div class="repo-meta">
              <span>{{ row.language || 'Unknown' }}</span>
              <span>Stars {{ formatNumber(row.stars) }}</span>
              <span>Forks {{ formatNumber(row.forks) }}</span>
              <span>新增 {{ formatNumber(row.starsGained) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作用" min-width="260">
          <template #default="{ row }">
            <el-input
              v-model="rowDraft(row).effectCn"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="中文说明这个项目能解决什么问题"
            />
          </template>
        </el-table-column>
        <el-table-column label="场景" min-width="260">
          <template #default="{ row }">
            <el-input
              v-model="rowDraft(row).scenarioCn"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="中文说明适合哪些使用场景"
            />
          </template>
        </el-table-column>
        <el-table-column label="摘要状态" width="110">
          <template #default="{ row }">
            <el-tag :type="summaryType(row.summaryStatus)" effect="plain">
              {{ summaryLabel(row.summaryStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt || row.sourceFetchedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button
                type="primary"
                size="small"
                :loading="savingIds.has(row.id)"
                @click="saveRow(row)"
              >
                保存
              </el-button>
              <el-button
                size="small"
                :loading="regeneratingIds.has(row.id)"
                @click="regenerateRow(row)"
              >
                重生成
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.github-trending-admin {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.github-trending-admin__header,
.table-panel__head,
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.github-trending-admin__header h1,
.panel-head h2,
.table-panel__head h2 {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
}

.panel-head h2,
.table-panel__head h2 {
  font-size: 16px;
}

.github-trending-admin__header p,
.panel-head p,
.table-panel__head p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.github-trending-admin__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.github-trending-admin__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-card,
.config-panel,
.table-panel {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.stat-card {
  padding: 16px 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fafb 100%);
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

.github-trending-admin__alert {
  margin-top: -8px;
}

.config-panel,
.table-panel {
  padding: 20px;
}

.config-form {
  max-width: 760px;
  margin-top: 18px;
}

.trending-table {
  margin-top: 16px;
}

.repo-link,
.repo-name {
  color: #111827;
  font-weight: 700;
  text-decoration: none;
}

.repo-link:hover {
  color: #0d9488;
  text-decoration: underline;
}

.repo-desc {
  margin: 6px 0 8px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.5;
}

.repo-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.repo-meta span {
  border-radius: 999px;
  background: #f3f4f6;
  color: #6b7280;
  padding: 2px 8px;
  font-size: 12px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 1100px) {
  .github-trending-admin__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .github-trending-admin__header,
  .table-panel__head,
  .panel-head {
    flex-direction: column;
  }

  .github-trending-admin__stats {
    grid-template-columns: 1fr;
  }
}
</style>
