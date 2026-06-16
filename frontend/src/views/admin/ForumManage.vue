<template>
  <div class="admin-forum-page">
    <h1 class="text-xl font-bold text-gray-800 mb-5">论坛管理</h1>

    <div class="admin-forum-toolbar">
      <el-input v-model="keyword" class="admin-forum-toolbar__search" placeholder="搜索帖子" clearable @keyup.enter="load" />
      <el-select v-model="status" class="admin-forum-toolbar__select" clearable placeholder="状态">
        <el-option label="全部" value="" />
        <el-option label="公开" value="NORMAL" />
        <el-option label="隐藏" value="HIDDEN" />
        <el-option label="锁定" value="LOCKED" />
        <el-option label="删除" value="DELETED" />
      </el-select>
      <el-select v-model="categoryId" class="admin-forum-toolbar__select" clearable placeholder="分类">
        <el-option label="全部分类" value="" />
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
      <el-button type="primary" @click="load">筛选</el-button>
    </div>

    <el-table :data="items" stripe>
      <el-table-column label="标题" min-width="360">
        <template #default="{ row }">
          <router-link :to="'/forum/' + row.id" class="text-primary hover:underline">{{ row.title }}</router-link>
          <div class="text-xs text-gray-500 mt-1">{{ row.categoryName }} · {{ row.authorUsername }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="92">
        <template #default="{ row }">{{ postTypeLabel(row.postType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="统计" width="190">
        <template #default="{ row }">
          <div class="admin-forum-stats">
            <span>回复 {{ row.replyCount || 0 }}</span>
            <span>赞 {{ row.likeCount || 0 }}</span>
            <span>收藏 {{ row.favoriteCount || 0 }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-space :size="8" wrap>
            <el-button size="small" @click="pin(row)">{{ row.pinned ? '取消置顶' : '置顶' }}</el-button>
            <el-button size="small" @click="toggleLock(row)">{{ row.status === 'LOCKED' ? '解锁' : '锁定' }}</el-button>
            <el-button size="small" @click="toggleHidden(row)">{{ row.status === 'HIDDEN' ? '恢复' : '隐藏' }}</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'
import { forumPostTypeLabels, forumStatusLabels } from '../../utils/forum'

const items = ref([])
const categories = ref([])
const keyword = ref('')
const status = ref('')
const categoryId = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

onMounted(async () => {
  await loadCategories()
  await load()
})

async function loadCategories() {
  categories.value = await api.get('/admin/forum/categories')
}

async function load() {
  const params = { page: page.value, size: size.value }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (status.value) params.status = status.value
  if (categoryId.value !== '') params.categoryId = categoryId.value
  const data = await api.get('/admin/forum/posts', { params })
  items.value = data.items || []
  total.value = data.total || 0
}

function onSizeChange() {
  page.value = 1
  load()
}

async function pin(row) {
  await api.post(`/admin/forum/posts/${row.id}/${row.pinned ? 'unpin' : 'pin'}`)
  ElMessage.success(row.pinned ? '已取消置顶' : '已置顶')
  await load()
}

async function toggleLock(row) {
  await api.post(`/admin/forum/posts/${row.id}/${row.status === 'LOCKED' ? 'unlock' : 'lock'}`)
  ElMessage.success(row.status === 'LOCKED' ? '已解锁' : '已锁定')
  await load()
}

async function toggleHidden(row) {
  await api.post(`/admin/forum/posts/${row.id}/${row.status === 'HIDDEN' ? 'show' : 'hide'}`)
  ElMessage.success(row.status === 'HIDDEN' ? '已恢复' : '已隐藏')
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定永久删除帖子「${row.title}」？此操作不可恢复。`, '提示', { type: 'warning' })
    await api.delete(`/admin/forum/posts/${row.id}`)
    ElMessage.success('已永久删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function statusTagType(value) {
  if (value === 'NORMAL') return 'success'
  if (value === 'LOCKED') return 'warning'
  if (value === 'HIDDEN') return 'info'
  return 'danger'
}

function postTypeLabel(value) {
  return forumPostTypeLabels[value] || value || '-'
}

function statusLabel(value) {
  return forumStatusLabels[value] || value || '-'
}
</script>

<style scoped>
.admin-forum-toolbar {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}
.admin-forum-toolbar__search {
  flex: 1 1 280px;
  min-width: 220px;
}
.admin-forum-toolbar__select {
  width: 180px;
}
.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.admin-forum-stats {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: nowrap;
  white-space: nowrap;
  font-size: 12px;
  line-height: 1.2;
  color: #4b5563;
}

:deep(.el-table__cell) {
  vertical-align: top;
}
</style>
