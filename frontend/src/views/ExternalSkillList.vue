<template>
  <div>
    <PageHero
      variant="amber"
      title="外部 Skill 大全"
      subtitle="来自社区的外部 Agent Skills，一键安装"
    >
      <div class="global-search">
        <el-input
          v-model="keyword"
          placeholder="搜索外部 Skill（最多 200 字）"
          maxlength="200"
          show-word-limit
          clearable
          size="large"
          @keyup.enter="search"
        />
        <el-button type="primary" size="large" @click="search">搜索</el-button>
      </div>
    </PageHero>

    <div class="max-w-[1280px] mx-auto px-6 py-6">
      <!-- Skeleton -->
      <div v-if="loading" class="space-y-4" aria-busy="true" aria-label="加载中">
        <div v-for="i in 5" :key="i" class="rounded-xl border border-[#e5e7eb] bg-white p-5 animate-pulse">
          <div class="h-5 bg-[#e5e7eb] rounded w-2/3 mb-3" />
          <div class="flex gap-2 mb-3">
            <div class="h-6 w-16 bg-[#f3f4f6] rounded" />
            <div class="h-6 w-20 bg-[#f3f4f6] rounded" />
          </div>
          <div class="h-4 bg-[#f3f4f6] rounded w-full" />
          <div class="h-4 bg-[#f3f4f6] rounded w-[80%] mt-2" />
        </div>
      </div>

      <!-- 列表：item-card 风格 -->
      <div v-else class="flex flex-col gap-4">
        <router-link
          v-for="e in items"
          :key="e.id"
          :to="'/external-skills/' + e.id"
          class="block p-5 bg-white border border-[#e5e7eb] rounded-xl hover:border-primary hover:shadow-[0_4px_12px_rgba(37,99,235,0.1)] transition-all duration-200 cursor-pointer"
        >
          <div class="font-semibold text-[16px] text-[#1f2937] mb-2">{{ e.name }}</div>
          <div class="flex flex-wrap gap-2 mb-2">
            <span v-for="t in (e.tagNames || []).slice(0, 3)" :key="t" class="tag-chip tag-chip--sm">{{ t }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--muted">外部</span>
          </div>
          <p class="text-[14px] text-[#6b7280] leading-relaxed line-clamp-2">{{ e.description || '暂无描述' }}</p>
        </router-link>
      </div>

      <div class="flex justify-center mt-8">
        <el-pagination
          v-if="total > 0 && !loading"
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
      <p v-if="!loading && !error && items.length === 0" class="text-center text-[#6b7280] py-12">暂无外部 Skill</p>
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
const size = ref(10)
const keyword = ref('')
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const data = await api.get('/external-skills', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

onMounted(load)
</script>
