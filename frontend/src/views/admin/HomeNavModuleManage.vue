<template>
  <div class="admin-modules-page">
    <h1 class="text-xl font-bold text-gray-800 mb-2">模块管理</h1>
    <p class="text-sm text-gray-500 mb-5">控制各模块在顶部导航与首页的显示与隐藏。隐藏后用户仍可通过直接访问 URL 进入页面。</p>

    <el-table :data="items" stripe>
      <el-table-column prop="name" label="模块名称" min-width="180" />
      <el-table-column label="导航入口" min-width="160">
        <template #default="{ row }">
          <span v-if="row.navLabel">{{ row.navLabel }}</span>
          <span v-else class="text-gray-400">仅首页展示</span>
        </template>
      </el-table-column>
      <el-table-column label="路径" min-width="160">
        <template #default="{ row }">
          <span v-if="row.navPath">{{ row.navPath }}</span>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="显示" width="120">
        <template #default="{ row }">
          <el-switch
            :model-value="row.visible"
            :loading="savingId === row.id"
            active-text="显示"
            inactive-text="隐藏"
            inline-prompt
            @change="(value) => toggleVisible(row, value)"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'

const items = ref([])
const savingId = ref(null)

onMounted(load)

async function load() {
  items.value = await api.get('/admin/home/nav-modules')
}

async function toggleVisible(row, visible) {
  savingId.value = row.id
  try {
    const updated = await api.put(`/admin/home/nav-modules/${row.id}`, { visible })
    row.visible = updated.visible
    ElMessage.success(updated.visible ? '已显示' : '已隐藏')
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    savingId.value = null
  }
}
</script>
