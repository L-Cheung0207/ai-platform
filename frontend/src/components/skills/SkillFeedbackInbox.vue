<script setup>
import { onMounted, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'

const emit = defineEmits(['changed'])

const items = ref([])
const total = shallowRef(0)
const page = shallowRef(1)
const size = shallowRef(10)
const status = shallowRef('OPEN')
const loading = shallowRef(false)
const updatingId = shallowRef(null)
const error = shallowRef('')

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'OPEN', label: '待处理' },
  { value: 'REVIEWED', label: '已审阅' },
  { value: 'RESOLVED', label: '已解决' },
]

const typeLabels = {
  SUCCESS_CASE: '成功案例',
  FAILURE_CASE: '失败案例',
  IMPROVEMENT: '改进建议',
  SCOPE_NOTE: '范围补充',
}

const statusLabels = {
  OPEN: '待处理',
  REVIEWED: '已审阅',
  RESOLVED: '已解决',
}

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (status.value) params.status = status.value
    const data = await api.get('/admin/skill-feedback', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '反馈加载失败'
  } finally {
    loading.value = false
  }
}

async function updateStatus(row, nextStatus) {
  if (!row?.skillId || !row?.id) return
  updatingId.value = row.id
  error.value = ''
  try {
    await api.put(`/admin/skills/${row.skillId}/feedback/${row.id}/status`, { status: nextStatus })
    ElMessage.success('反馈状态已更新')
    await load()
    emit('changed')
  } catch (e) {
    error.value = e.message || '状态更新失败'
  } finally {
    updatingId.value = null
  }
}

function onFilterChange() {
  page.value = 1
  load()
}

function onSizeChange() {
  page.value = 1
  load()
}

function typeLabel(value) {
  return typeLabels[value] || '—'
}

function statusLabel(value) {
  return statusLabels[value] || '—'
}

function statusTagType(value) {
  if (value === 'RESOLVED') return 'success'
  if (value === 'REVIEWED') return 'warning'
  return 'danger'
}
</script>

<template>
  <section class="feedback-inbox">
    <div class="feedback-inbox__header">
      <div>
        <h2>反馈收件箱</h2>
        <p>失败案例、改进建议和适用范围补充</p>
      </div>
      <el-select v-model="status" class="feedback-inbox__filter" @change="onFilterChange">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="skillName" label="Skill" min-width="160" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeLabel(row.feedbackType) }}</template>
      </el-table-column>
      <el-table-column prop="content" label="反馈内容" min-width="260" show-overflow-tooltip />
      <el-table-column label="收益" width="90">
        <template #default="{ row }">{{ Math.round((row.estimatedSavedMinutes || 0) / 6) / 10 }}h</template>
      </el-table-column>
      <el-table-column label="评分" width="90">
        <template #default="{ row }">{{ row.rating || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'OPEN'"
            size="small"
            link
            type="primary"
            :loading="updatingId === row.id"
            @click="updateStatus(row, 'REVIEWED')"
          >
            标记已审
          </el-button>
          <el-button
            v-if="row.status !== 'RESOLVED'"
            size="small"
            link
            type="primary"
            :loading="updatingId === row.id"
            @click="updateStatus(row, 'RESOLVED')"
          >
            关闭
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="feedback-inbox__footer">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon class="feedback-inbox__error" />
  </section>
</template>

<style scoped>
.feedback-inbox {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.feedback-inbox__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.feedback-inbox__header h2 {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.feedback-inbox__header p {
  color: #6b7280;
  font-size: 13px;
  margin: 4px 0 0;
}

.feedback-inbox__filter {
  width: 140px;
}

.feedback-inbox__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.feedback-inbox__error {
  margin-top: 12px;
}

@media (max-width: 680px) {
  .feedback-inbox__header {
    flex-direction: column;
  }

  .feedback-inbox__filter {
    width: 100%;
  }
}
</style>
