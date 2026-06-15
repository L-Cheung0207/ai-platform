<template>
  <div class="admin-forum-categories-page">
    <h1 class="text-xl font-bold text-gray-800 mb-5">论坛分类管理</h1>

    <div class="mb-4">
      <el-button type="primary" @click="openNew">新建分类</el-button>
    </div>

    <el-table :data="items" stripe>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="slug" label="Slug" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="240" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-space wrap>
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑分类' : '新建分类'" width="560px" draggable>
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例如：提问求助" />
        </el-form-item>
        <el-form-item label="Slug">
          <el-input v-model="form.slug" placeholder="可留空，自动生成" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="给这个分类一个简短说明" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'

const items = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const saving = ref(false)
const form = ref({ name: '', slug: '', description: '', sortOrder: 0, enabled: true })

onMounted(load)

async function load() {
  items.value = await api.get('/admin/forum/categories')
}

function openNew() {
  editId.value = null
  form.value = { name: '', slug: '', description: '', sortOrder: 0, enabled: true }
  dialogVisible.value = true
}

function edit(row) {
  editId.value = row.id
  form.value = { name: row.name || '', slug: row.slug || '', description: row.description || '', sortOrder: row.sortOrder || 0, enabled: row.enabled !== false }
  dialogVisible.value = true
}

async function save() {
  const name = (form.value.name || '').trim()
  if (!name) {
    ElMessage.warning('请填写名称')
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value, name, slug: (form.value.slug || '').trim() || null }
    if (editId.value) {
      await api.put(`/admin/forum/categories/${editId.value}`, payload)
    } else {
      await api.post('/admin/forum/categories', payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '提示', { type: 'warning' })
    await api.delete(`/admin/forum/categories/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}
</script>
