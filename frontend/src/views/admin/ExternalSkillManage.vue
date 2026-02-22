<template>
  <div>
    <h1 class="text-xl font-bold text-gray-800 mb-2">外部 Skill 管理</h1>

    <el-dialog v-model="editing" title="编辑外部 Skill" width="700" draggable class="admin-dialog">
      <el-form :model="form" label-position="top" @submit.prevent="saveEdit">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="form.tagsStr" placeholder="如 vue, 前端" />
        </el-form-item>
        <el-form-item label="安装命令" required>
          <el-input v-model="form.installCommand" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="来源链接">
          <el-input v-model="form.sourceUrl" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="50000" show-word-limit class="font-mono text-sm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button native-type="button" @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <div class="flex gap-3 mb-4">
      <el-button type="primary" :loading="triggering" :disabled="status === 'running'" @click="triggerScrape">
        {{ status === 'running' ? '爬取中...' : '触发爬取' }}
      </el-button>
      <el-button :loading="statusLoading" @click="fetchStatus">刷新状态</el-button>
      <el-input v-model="keyword" placeholder="关键词" clearable class="w-48" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
      <router-link to="/skills" class="el-button el-button--default">查看公开列表</router-link>
    </div>

    <div v-if="status === 'running' || statusData?.added != null" class="mb-4 text-sm text-gray-600">
      状态: {{ statusLabel }}
      <span v-if="statusData?.added != null">，新增 {{ statusData.added }} 条</span>
      <span v-if="statusData?.error" class="text-red-600">，错误: {{ statusData.error }}</span>
    </div>

    <el-table :data="items" stripe>
      <el-table-column prop="name" label="名称" min-width="150">
        <template #default="{ row }">
          <router-link :to="'/external-skills/' + row.id" class="text-primary hover:underline">{{ row.name }}</router-link>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="180">
        <template #default="{ row }">
          <template v-if="(row.tagNames || []).length">
            <el-tag
              v-for="tag in (row.tagNames || [])"
              :key="tag"
              size="small"
              effect="plain"
              class="mr-1 mb-1"
            >
              {{ tag }}
            </el-tag>
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
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
const error = ref('')
const triggering = ref(false)
const statusLoading = ref(false)
const statusData = ref(null)
const status = ref('idle')
const editing = ref(false)
const editId = ref(null)
const form = ref({
  name: '',
  description: '',
  tagsStr: '',
  installCommand: '',
  sourceUrl: '',
  content: '',
})
const saving = ref(false)
let pollTimer = null

const statusLabel = computed(() => ({ idle: '未运行', running: '运行中', completed: '已完成', failed: '失败' }[status.value] || status.value))

onMounted(() => load())

function startPolling() {
  stopPolling()
  pollTimer = setInterval(fetchStatus, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function fetchStatus() {
  statusLoading.value = true
  try {
    statusData.value = await api.get('/admin/scrape-status/external-skills')
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
    await api.post('/admin/scrape/external-skills')
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

onBeforeUnmount(() => {
  stopPolling()
})

function onSizeChange() {
  page.value = 1
  load()
}

function parseTags(str) {
  if (!str || !str.trim()) return []
  return str.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

async function startEdit(row) {
  error.value = ''
  try {
    const data = await api.get('/admin/external-skills/' + row.id)
    editId.value = data.id
    form.value = {
      name: data.name || '',
      description: data.description || '',
      tagsStr: (data.tagNames || []).join(', '),
      installCommand: data.installCommand || '',
      sourceUrl: data.sourceUrl || '',
      content: data.content || '',
    }
    editing.value = true
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

async function saveEdit() {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  if (!form.value.installCommand?.trim()) {
    ElMessage.warning('请输入安装命令')
    return
  }
  saving.value = true
  error.value = ''
  try {
    await api.put('/admin/external-skills/' + editId.value, {
      name: form.value.name.trim(),
      description: form.value.description || '',
      tags: parseTags(form.value.tagsStr),
      installCommand: form.value.installCommand.trim(),
      sourceUrl: form.value.sourceUrl || '',
      content: form.value.content || '',
    })
    ElMessage.success('更新成功')
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function load() {
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const data = await api.get('/admin/external-skills', { params })
    items.value = data.items || []
    total.value = data.total ?? 0
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该外部 Skill？', '确认', { type: 'warning' })
    await api.delete('/admin/external-skills/' + row.id)
    ElMessage.success('已删除')
    load()
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
