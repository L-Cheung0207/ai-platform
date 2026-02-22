<template>
  <div>
    <h1 class="text-xl font-bold text-gray-800 mb-2">AI 工具管理</h1>
    <div class="flex gap-3 mb-4">
      <el-button type="primary" :loading="triggering" :disabled="status === 'running'" @click="triggerScrape">
        {{ status === 'running' ? '爬取中...' : '触发爬取' }}
      </el-button>
      <el-button :loading="statusLoading" @click="fetchStatus">刷新状态</el-button>
      <el-input v-model="keyword" placeholder="关键词" clearable class="w-48" @keyup.enter="load" />
      <el-select v-model="selectedCategory" placeholder="分类" clearable class="w-40">
        <el-option label="全部分类" value="" />
        <el-option v-for="opt in categories" :key="opt" :label="opt" :value="opt" />
      </el-select>
      <el-button type="primary" @click="load">搜索</el-button>
      <router-link to="/ai-tools" class="el-button el-button--default">查看公开列表</router-link>
    </div>

    <div v-if="status === 'running' || statusData?.added != null" class="mb-4 text-sm text-gray-600">
      状态: {{ statusLabel }}
      <span v-if="statusData?.added != null">，新增 {{ statusData.added }} 条</span>
      <span v-if="statusData?.error" class="text-red-600">，错误: {{ statusData.error }}</span>
    </div>

    <el-table :data="items" stripe>
      <el-table-column label="Logo" width="60">
        <template #default="{ row }">
          <img
            v-if="row.logoPath"
            :src="'/uploads/' + row.logoPath"
            :alt="row.name"
            class="w-8 h-8 rounded object-contain"
          />
          <img
            v-else-if="row.logoUrl"
            :src="row.logoUrl"
            :alt="row.name"
            class="w-8 h-8 rounded object-contain"
          />
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="150">
        <template #default="{ row }">
          <router-link :to="'/ai-tools/' + row.id" class="text-primary hover:underline">{{ row.name }}</router-link>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="180">
        <template #default="{ row }">
          <template v-if="(row.tagNames || '').trim()">
            <el-tag
              v-for="tag in (row.tagNames || '').split(',').filter(Boolean)"
              :key="tag"
              size="small"
              effect="plain"
              class="mr-1 mb-1"
            >
              {{ tag.trim() }}
            </el-tag>
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import api from '../../services/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const selectedCategory = ref('')
const categories = ref([])
const error = ref('')
const triggering = ref(false)
const statusLoading = ref(false)
const statusData = ref(null)
const status = ref('idle')
let pollTimer = null

const statusLabel = computed(() => ({ idle: '未运行', running: '运行中', completed: '已完成', failed: '失败' }[status.value] || status.value))

async function load() {
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (selectedCategory.value) params.category = selectedCategory.value
    const data = await api.get('/admin/ai-tools', { params })
    items.value = data.items || []
    total.value = data.total ?? 0
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

async function loadCategories() {
  try {
    categories.value = await api.get('/ai-tools/filter-options')
  } catch (_) {
    categories.value = []
  }
}

function onSizeChange() {
  page.value = 1
  load()
}

async function fetchStatus() {
  statusLoading.value = true
  try {
    statusData.value = await api.get('/admin/scrape-status/ai-tools')
    status.value = statusData.value?.status || 'idle'
    if (status.value !== 'running') stopPolling()
  } catch (e) {
    error.value = e.message || '获取状态失败'
  } finally {
    statusLoading.value = false
  }
}

async function triggerScrape() {
  triggering.value = true
  error.value = ''
  try {
    await api.post('/admin/scrape/ai-tools')
    ElMessage.success('爬取任务已启动')
    status.value = 'running'
    statusData.value = { status: 'running' }
    startPolling()
  } catch (e) {
    error.value = e.message || '触发失败'
  } finally {
    triggering.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(fetchStatus, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除「' + row.name + '」？', '提示', { type: 'warning' })
    await api.delete('/admin/ai-tools/' + row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') error.value = e.message || '删除失败'
  }
}

onMounted(() => {
  loadCategories()
  load()
  fetchStatus()
})
onBeforeUnmount(stopPolling)
</script>

<style scoped>
.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
  padding: 1rem 0;
}
.admin-pagination :deep(.el-pagination) {
  font-weight: 500;
}
</style>
