<template>
  <div>
    <h1 class="text-xl font-bold text-gray-800 mb-2">资讯管理</h1>
    <div class="flex flex-nowrap items-center gap-3 mb-4">
      <el-button type="primary" :loading="triggering" :disabled="status === 'running'" @click="triggerScrape">
        {{ status === 'running' ? '爬取中...' : '触发爬取' }}
      </el-button>
      <el-button :loading="statusLoading" @click="fetchStatus">刷新状态</el-button>
      <el-input v-model="keyword" placeholder="关键词" clearable maxlength="200" class="w-40" @keyup.enter="load" />
      <el-date-picker
        v-model="filterDate"
        type="date"
        placeholder="发布日期"
        value-format="YYYY-MM-DD"
        clearable
        class="w-40"
      />
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="startNew">新建资讯</el-button>
      <router-link to="/news" class="el-button el-button--default">查看公开列表</router-link>
    </div>

    <div v-if="status === 'running' || statusData?.added != null" class="mb-4 text-sm text-gray-600">
      状态: {{ statusLabel }}
      <span v-if="statusData?.added != null">，新增 {{ statusData.added }} 条</span>
      <span v-if="statusData?.error" class="text-red-600">，错误: {{ statusData.error }}</span>
    </div>

    <el-dialog v-model="editing" :title="editId ? '编辑资讯' : '新建资讯'" width="500" draggable class="admin-dialog">
      <el-form :model="form" label-position="top" @submit.prevent="saveNews">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="来源 URL">
          <el-input v-model="form.sourceUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="来源名称">
          <el-input v-model="form.sourceName" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="form.publishDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button native-type="button" @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveNews">保存</el-button>
      </template>
    </el-dialog>

    <el-table :data="items" stripe>
      <el-table-column prop="title" label="标题" min-width="180">
        <template #default="{ row }">
          <router-link :to="'/news/' + row.id" class="text-primary hover:underline">{{ row.title }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sourceName" label="来源" width="120" />
      <el-table-column prop="publishDate" label="发布日期" width="120" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="startEdit(row)">编辑</el-button>
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
const filterDate = ref(null)
const editing = ref(false)
const editId = ref(null)
const form = ref({ title: '', summary: '', sourceUrl: '', sourceName: '', publishDate: '' })
const saving = ref(false)
const error = ref('')
const triggering = ref(false)
const statusLoading = ref(false)
const statusData = ref(null)
const status = ref('idle')
let pollTimer = null

const statusLabel = computed(() => ({ idle: '未运行', running: '运行中', completed: '已完成', failed: '失败' }[status.value] || status.value))

async function fetchStatus() {
  statusLoading.value = true
  try {
    statusData.value = await api.get('/admin/scrape-status')
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
    await api.post('/admin/scrape/aibase-news')
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

onMounted(() => {
  load()
  fetchStatus()
})
onBeforeUnmount(stopPolling)

function onSizeChange() {
  page.value = 1
  load()
}

async function load() {
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (filterDate.value) params.publishDate = filterDate.value
    const data = await api.get('/admin/news', { params })
    items.value = data.items || []
    total.value = data.total ?? 0
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

function startNew() {
  editId.value = null
  form.value = { title: '', summary: '', sourceUrl: '', sourceName: '', publishDate: '' }
  editing.value = true
  error.value = ''
}

function startEdit(n) {
  editId.value = n.id
  form.value = {
    title: n.title,
    summary: n.summary || '',
    sourceUrl: n.sourceUrl || '',
    sourceName: n.sourceName || '',
    publishDate: n.publishDate ? n.publishDate.slice(0, 10) : '',
  }
  editing.value = true
  error.value = ''
}

async function saveNews() {
  saving.value = true
  error.value = ''
  try {
    if (editId.value) await api.put('/news/' + editId.value, form.value)
    else await api.post('/news', form.value)
    await load()
    editing.value = false
    ElMessage.success('保存成功')
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove(n) {
  try {
    await ElMessageBox.confirm('确定删除资讯「' + n.title + '」？', '提示', { type: 'warning' })
    await api.delete('/news/' + n.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    if (e !== 'cancel') error.value = e.message || '删除失败'
  }
}
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
.admin-pagination :deep(.el-pagination .el-pager li) {
  min-width: 32px;
  height: 32px;
  line-height: 32px;
}
</style>
