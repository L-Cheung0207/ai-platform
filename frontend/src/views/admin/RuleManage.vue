<template>
  <div>
    <h1 class="text-xl font-bold text-gray-800 mb-2">Rule 管理</h1>
    <p class="text-gray-500 text-sm mb-4">管理员可创建、编辑、隐藏或删除 Rule。隐藏后公开列表不可见。</p>

    <el-dialog v-model="editing" :title="editId ? '编辑 Rule' : '新建 Rule'" width="700" draggable class="admin-dialog">
      <el-form :model="form" label-position="top" @submit.prevent="saveRule">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="form.tagsStr" placeholder="如 规范, 代码" />
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="100000" show-word-limit placeholder="Rule 正文内容（纯文本，用于复制）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button native-type="button" @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>

    <div class="flex gap-3 mb-4">
      <el-button type="primary" @click="startNew">新建 Rule</el-button>
      <el-input v-model="keyword" placeholder="关键词" maxlength="200" clearable class="w-48" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table :data="items" stripe>
      <el-table-column prop="name" label="Rule">
        <template #default="{ row }">
          <router-link :to="'/rules/' + row.id" class="text-primary hover:underline">{{ row.name }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="visibility" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.visibility === 'VISIBLE' ? 'success' : 'info'" size="small">
            {{ row.visibility === 'VISIBLE' ? '可见' : '已隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="startEdit(row)">编辑</el-button>
          <el-button v-if="row.visibility === 'VISIBLE'" size="small" link type="primary" @click="hide(row.id)">隐藏</el-button>
          <el-button v-else size="small" link type="primary" @click="unhide(row.id)">恢复可见</el-button>
          <el-button size="small" link type="danger" @click="remove(row.id)">永久删除</el-button>
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
import { ref, onMounted } from 'vue'
import api from '../../services/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const editing = ref(false)
const editId = ref(null)
const form = ref({ name: '', tagsStr: '', content: '' })
const saving = ref(false)
const error = ref('')

onMounted(load)

function onSizeChange() {
  page.value = 1
  load()
}

function parseTags(str) {
  if (!str || !str.trim()) return []
  return str.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

function startNew() {
  editId.value = null
  form.value = { name: '', tagsStr: '', content: '' }
  editing.value = true
  error.value = ''
}

function startEdit(row) {
  editId.value = row.id
  form.value = {
    name: row.name,
    tagsStr: (row.tagNames || row.tags || []).map(t => typeof t === 'string' ? t : t?.name).filter(Boolean).join(', '),
    content: row.content || '',
  }
  editing.value = true
  error.value = ''
}

async function saveRule() {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  if (!form.value.content?.trim()) {
    ElMessage.warning('请输入正文')
    return
  }
  saving.value = true
  error.value = ''
  try {
    const body = {
      name: form.value.name.trim(),
      tags: parseTags(form.value.tagsStr),
      content: form.value.content.trim(),
    }
    if (editId.value) {
      await api.put('/admin/rules/' + editId.value, body)
      ElMessage.success('更新成功')
    } else {
      await api.post('/admin/rules', body)
      ElMessage.success('创建成功')
    }
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function load() {
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const data = await api.get('/admin/rules', { params })
    items.value = data.items || []
    total.value = data.total ?? 0
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

async function hide(id) {
  try {
    await api.post('/admin/rules/' + id + '/hide')
    ElMessage.success('已隐藏')
    await load()
  } catch (e) {
    error.value = e.message || '操作失败'
  }
}

async function unhide(id) {
  try {
    await api.post('/admin/rules/' + id + '/unhide')
    ElMessage.success('已恢复可见')
    await load()
  } catch (e) {
    error.value = e.message || '操作失败'
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定永久删除该 Rule？此操作不可恢复。', '提示', { type: 'warning' })
    await api.delete('/admin/rules/' + id)
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
