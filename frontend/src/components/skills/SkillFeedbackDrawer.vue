<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  skill: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue', 'updated'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const items = ref([])
const total = ref(0)
const loading = ref(false)
const statusFilter = ref('')
const error = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'OPEN', label: '待处理' },
  { value: 'REVIEWED', label: '已查看' },
  { value: 'RESOLVED', label: '已闭环' },
]

const typeOptions = [
  { value: 'SUCCESS_CASE', label: '成功案例' },
  { value: 'FAILURE_CASE', label: '失败案例' },
  { value: 'IMPROVEMENT', label: '改进建议' },
  { value: 'SCOPE_NOTE', label: '适用范围补充' },
]

watch(() => props.modelValue, (value) => {
  if (value && props.skill?.id) load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: 1, size: 50 }
    if (statusFilter.value) params.status = statusFilter.value
    const data = await api.get(`/admin/skills/${props.skill.id}/feedback`, { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载反馈失败'
  } finally {
    loading.value = false
  }
}

async function updateStatus(row, status) {
  try {
    await api.put(`/admin/skills/${props.skill.id}/feedback/${row.id}/status`, { status })
    ElMessage.success('状态已更新')
    emit('updated')
    await load()
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  }
}

function optionLabel(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}
</script>

<template>
  <el-drawer v-model="visible" title="Skill 反馈" size="720px" class="feedback-drawer">
    <div v-if="skill" class="feedback-heading">
      <strong>{{ skill.name }}</strong>
      <span>共 {{ total }} 条</span>
    </div>

    <div class="feedback-toolbar">
      <el-select v-model="statusFilter" class="w-36" @change="load">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ optionLabel(typeOptions, row.feedbackType) }}</template>
      </el-table-column>
      <el-table-column prop="submitterName" label="提交人" width="110">
        <template #default="{ row }">{{ row.submitterName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="rating" label="评分" width="80">
        <template #default="{ row }">{{ row.rating || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">{{ optionLabel(statusOptions, row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-if="row.status === 'OPEN'" link type="primary" size="small" @click="updateStatus(row, 'REVIEWED')">已查看</el-button>
          <el-button v-if="row.status !== 'RESOLVED'" link type="primary" size="small" @click="updateStatus(row, 'RESOLVED')">闭环</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-alert v-if="error" :title="error" type="error" show-icon class="mt-3" />
  </el-drawer>
</template>

<style scoped>
.feedback-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  color: #374151;
}

.feedback-heading span {
  color: #9ca3af;
  font-size: 13px;
}

.feedback-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.feedback-drawer :deep(.el-drawer) {
  max-width: calc(100vw - 24px);
}
</style>
