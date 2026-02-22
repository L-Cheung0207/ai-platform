<template>
  <div>
    <h1 class="text-xl font-bold text-gray-800 mb-6">文章管理</h1>
    <el-dialog v-model="editing" :title="editId ? '编辑文章' : '新建文章'" width="500" draggable class="admin-dialog">
      <el-form :model="form" label-position="top" @submit.prevent="saveArticle">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="w-full">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button native-type="button" @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveArticle">保存</el-button>
      </template>
    </el-dialog>
    <el-button type="primary" class="mb-4" @click="startNew">新建文章</el-button>
    <el-table :data="items" stripe>
      <el-table-column prop="title" label="标题">
        <template #default="{ row }">
          <router-link :to="'/articles/' + row.id" class="text-primary hover:underline">{{ row.title }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
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
import { ref, onMounted } from 'vue'
import api from '../../services/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const editing = ref(false)
const editId = ref(null)
const form = ref({ title: '', summary: '', content: '', status: 'DRAFT' })
const saving = ref(false)
const error = ref('')

onMounted(load)

function onSizeChange() {
  page.value = 1
  load()
}

async function load() {
  try {
    const data = await api.get('/admin/articles', { params: { page: page.value, size: size.value } })
    items.value = data.items || []
    total.value = data.total ?? 0
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

async function loadOne(id) {
  try { return await api.get('/admin/articles/' + id) } catch (_) { return null }
}

function startNew() {
  editId.value = null
  form.value = { title: '', summary: '', content: '', status: 'DRAFT' }
  editing.value = true
  error.value = ''
}

async function startEdit(a) {
  const one = await loadOne(a.id)
  if (!one) { error.value = '无法加载文章'; return }
  editId.value = one.id
  form.value = { title: one.title, summary: one.summary || '', content: one.content || '', status: one.status || 'DRAFT' }
  editing.value = true
  error.value = ''
}

async function saveArticle() {
  saving.value = true
  error.value = ''
  try {
    if (editId.value) await api.put('/articles/' + editId.value, form.value)
    else await api.post('/articles', form.value)
    await load()
    editing.value = false
    ElMessage.success('保存成功')
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove(a) {
  try {
    await ElMessageBox.confirm('确定删除文章「' + a.title + '」？', '提示', { type: 'warning' })
    await api.delete('/articles/' + a.id)
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
