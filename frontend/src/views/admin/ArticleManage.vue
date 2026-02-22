<template>
  <div>
    <h1 class="text-xl font-bold text-gray-800 mb-6">AI知识库管理</h1>
    <el-dialog v-model="editing" :title="editId ? '编辑知识' : '新建知识'" width="800" draggable class="admin-dialog article-edit-dialog">
      <el-form :model="form" label-position="top" @submit.prevent="saveArticle">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="标题" />
        </el-form-item>
        <el-form-item label="内容类型" required>
          <el-radio-group v-model="form.contentType">
            <el-radio value="RICH_TEXT">富文本（所见即所得）</el-radio>
            <el-radio value="MARKDOWN">Markdown（纯文本语法）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文">
          <p class="text-gray-500 text-sm mb-2">
            <template v-if="form.contentType === 'MARKDOWN'">直接写 Markdown 语法（如 **粗体**、# 标题、列表等），保存后详情页按 Markdown 渲染。</template>
            <template v-else>可切换下方「Markdown」标签录入，保存为富文本 HTML。</template>
          </p>
          <el-input
            v-if="form.contentType === 'MARKDOWN'"
            v-model="form.content"
            type="textarea"
            :rows="14"
            placeholder="支持 Markdown：**粗体**、*斜体*、[链接](url)、![图片](url)、# 标题、列表等…"
            class="markdown-plain-textarea"
          />
          <RichTextEditor v-else v-model="form.content" :content-type="form.contentType" />
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
    <el-button type="primary" class="mb-4" @click="startNew">新建知识</el-button>
    <el-table :data="items" stripe>
      <el-table-column prop="title" label="标题">
        <template #default="{ row }">
          <router-link :to="'/articles/' + row.id" class="text-primary hover:underline">{{ row.title }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="contentType" label="内容类型" width="110">
        <template #default="{ row }">
          {{ row.contentType === 'MARKDOWN' ? 'Markdown' : '富文本' }}
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
import { ref, watch, onMounted } from 'vue'
import api from '../../services/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import RichTextEditor from '../../components/RichTextEditor.vue'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const editing = ref(false)
const editId = ref(null)
const form = ref({ title: '', content: '', contentType: 'RICH_TEXT', status: 'DRAFT' })
const saving = ref(false)
const error = ref('')
/** 取消切换时恢复单选，避免再次触发 watch 弹窗 */
let skipContentTypeConfirm = false

onMounted(load)

// 切换「富文本」↔「Markdown」时，若已有正文则确认并清空，避免 content 与 contentType 不一致
watch(
  () => form.value.contentType,
  async (nextType, prevType) => {
    if (skipContentTypeConfirm) {
      skipContentTypeConfirm = false
      return
    }
    if (prevType === undefined || nextType === prevType) return
    const hasContent = (form.value.content || '').trim().length > 0
    if (!hasContent) return
    try {
      await ElMessageBox.confirm(
        '切换内容类型后，当前正文格式可能与新类型不符。建议清空后重新输入，是否清空正文并切换？',
        '切换内容类型',
        { type: 'warning', confirmButtonText: '清空并切换', cancelButtonText: '取消' }
      )
      form.value.content = ''
    } catch {
      skipContentTypeConfirm = true
      form.value.contentType = prevType
    }
  }
)

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
  skipContentTypeConfirm = true
  form.value = { title: '', content: '', contentType: 'RICH_TEXT', status: 'DRAFT' }
  editing.value = true
  error.value = ''
}

async function startEdit(a) {
  const one = await loadOne(a.id)
  if (!one) { error.value = '无法加载该知识'; return }
  editId.value = one.id
  skipContentTypeConfirm = true
  form.value = { title: one.title, content: one.content || '', contentType: one.contentType || 'RICH_TEXT', status: one.status || 'DRAFT' }
  editing.value = true
  error.value = ''
}

async function saveArticle() {
  const title = (form.value.title || '').trim()
  if (!title) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  error.value = ''
  try {
    const payload = { ...form.value, title, content: form.value.content ?? '' }
    if (editId.value) await api.put('/articles/' + editId.value, payload)
    else await api.post('/articles', payload)
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
    await ElMessageBox.confirm('确定删除该知识「' + a.title + '」？', '提示', { type: 'warning' })
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
.article-edit-dialog :deep(.el-dialog__body) {
  max-height: 75vh;
  overflow-y: auto;
}
.article-edit-dialog :deep(.el-form-item) {
  width: 100%;
}
.article-edit-dialog :deep(.el-form-item__content) {
  width: 100%;
  max-width: 100%;
}
.markdown-plain-textarea :deep(textarea) {
  font-family: ui-monospace, 'SF Mono', Monaco, 'Cascadia Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
}
</style>
