<template>
  <div>
    <PageHero
      variant="rose"
      title="AI 学习文章"
      subtitle="探索 AI 相关知识与实践"
    />

    <div class="max-w-4xl mx-auto px-6 py-8">
      <div class="space-y-3">
        <router-link v-for="a in items" :key="a.id" :to="'/articles/' + a.id">
          <el-card shadow="hover" class="transition-shadow">
            <h3 class="font-medium text-gray-800">{{ a.title }}</h3>
            <p class="text-sm text-gray-500 mt-1 line-clamp-2">{{ a.summary || '暂无摘要' }}</p>
          </el-card>
        </router-link>
      </div>

      <div class="flex justify-center mt-8">
        <el-pagination
          v-if="total > 0"
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>

      <p v-if="loading" class="text-center text-gray-500 py-8">加载中…</p>
      <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
      <p v-if="!loading && !error && items.length === 0" class="text-center text-gray-500 py-8">暂无文章</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    const data = await api.get('/articles', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
