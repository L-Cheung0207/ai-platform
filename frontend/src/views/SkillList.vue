<template>
  <div>
    <PageHero
      variant="green"
      title="发现让 AI 更好用的 Skills"
      subtitle="内部与外部 Skill 一览，释放 AI 执行潜力"
    >
      <div class="global-search">
        <el-input
          v-model="keyword"
          placeholder="搜索 Skill（最多 200 字）"
          maxlength="200"
          show-word-limit
          clearable
          size="large"
          @keyup.enter="search"
        />
        <el-button type="primary" size="large" @click="search">搜索</el-button>
      </div>
    </PageHero>

    <div class="max-w-[1280px] mx-auto px-6 py-8">
      <!-- 类型筛选：全部 | 内部 | 外部 -->
      <div class="mb-6 flex flex-wrap gap-2 justify-center">
        <button
          v-for="opt in typeOptions"
          :key="opt.value"
          type="button"
          :class="[
            'px-4 py-2 rounded-full text-[14px] font-medium transition-all duration-200',
            typeFilter === opt.value ? 'bg-primary text-white' : 'bg-[#f3f4f6] text-[#4b5563] hover:bg-[#e5e7eb]'
          ]"
          @click="setTypeFilter(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>

      <!-- 分类标签 -->
      <div v-if="tagList.length > 0" class="mb-8 overflow-x-auto scrollbar-thin">
        <div class="flex flex-wrap gap-2 justify-center min-w-0">
          <button
            type="button"
            :class="[
              'px-4 py-2 rounded-full text-[14px] font-medium transition-all duration-200 whitespace-nowrap',
              selectedTagIds.length === 0
                ? 'bg-primary text-white'
                : 'bg-[#f3f4f6] text-[#4b5563] hover:bg-[#e5e7eb]'
            ]"
            @click="clearTagFilter"
          >
            全部
          </button>
          <button
            v-for="t in tagList"
            :key="t.id"
            type="button"
            :class="[
              'px-4 py-2 rounded-full text-[14px] font-medium transition-all duration-200 whitespace-nowrap',
              selectedTagIds.includes(t.id)
                ? 'bg-primary text-white'
                : 'bg-[#f3f4f6] text-[#4b5563] hover:bg-[#e5e7eb]'
            ]"
            @click="toggleTag(t.id)"
          >
            {{ tagEmoji(t.name) }} {{ t.name }}
          </button>
        </div>
      </div>

      <!-- 网格卡片 -->
      <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4" aria-busy="true">
        <div v-for="i in 6" :key="i" class="rounded-xl border border-[#e5e7eb] bg-white p-5 animate-pulse">
          <div class="h-5 bg-[#e5e7eb] rounded w-2/3 mb-3" />
          <div class="flex gap-2 mb-3 flex-wrap">
            <div class="h-6 w-16 bg-[#f3f4f6] rounded" />
            <div class="h-6 w-20 bg-[#f3f4f6] rounded" />
          </div>
          <div class="h-4 bg-[#f3f4f6] rounded w-full" />
          <div class="h-4 bg-[#f3f4f6] rounded w-[80%] mt-2" />
        </div>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <router-link
          v-for="s in displayItems"
          :key="s.type + '-' + s.id"
          :to="s.type === 'internal' ? '/skills/' + s.id : '/external-skills/' + s.id"
          class="block p-5 bg-white border border-[#e5e7eb] rounded-xl hover:border-primary hover:shadow-[0_4px_12px_rgba(37,99,235,0.1)] transition-all duration-200"
        >
          <div class="font-semibold text-[16px] text-[#1f2937] mb-2 truncate">{{ s.name }}</div>
          <div class="flex flex-wrap gap-2 mb-2">
            <span v-for="t in (s.tagNames || []).slice(0, 4)" :key="t" class="tag-chip tag-chip--sm">{{ t }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--muted">{{ s.type === 'internal' ? '内部' : '外部' }}</span>
          </div>
          <p class="text-[14px] text-[#6b7280] leading-relaxed line-clamp-2">{{ s.description || '暂无描述' }}</p>
        </router-link>
      </div>

      <div class="flex justify-center mt-8">
        <el-pagination
          v-if="total > 0 && !loading"
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="onPageChange"
        />
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
      <p v-if="!loading && !error && displayItems.length === 0" class="text-center text-[#6b7280] py-12">暂无 Skill</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'

const route = useRoute()
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref(route.query.keyword || '')
const typeFilter = ref('all') // 'all' | 'internal' | 'external'
const loading = ref(false)
const error = ref('')
const tagList = ref([])
const selectedTagIds = ref([])

const typeOptions = [
  { value: 'all', label: '全部' },
  { value: 'internal', label: '内部' },
  { value: 'external', label: '外部' },
]

const displayItems = computed(() => {
  if (typeFilter.value === 'all') {
    const start = (page.value - 1) * size.value
    return items.value.slice(start, start + size.value)
  }
  return items.value
})

const TAG_EMOJI = {
  效率工具: '⚡️',
  软件开发: '💻',
  文档处理: '📄',
  数据与分析: '📊',
  开发运维: '🚀',
  商业与营销: '💼',
  测试与安全: '🔒',
  内容与媒体: '🎨',
  研究: '🔬',
  数据库: '🗄️',
  区块链: '⛓️',
  生活方式: '🛋',
  协作与管理: '👥',
  Claude官方: '🌟',
}

function tagEmoji(name) {
  return TAG_EMOJI[name] || '🏷️'
}

function setTypeFilter(v) {
  typeFilter.value = v
  page.value = 1
  load()
}

async function loadTags() {
  try {
    const list = await api.get('/tags', { params: { forEntity: 'skills' } })
    tagList.value = list || []
  } catch {
    tagList.value = []
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const type = typeFilter.value
    const params = { page: 1, size: 500 }
    if (keyword.value) params.keyword = keyword.value
    if (selectedTagIds.value.length > 0) params.tags = selectedTagIds.value

    if (type === 'internal') {
      const data = await api.get('/skills', { params: { ...params, page: page.value, size: size.value } })
      items.value = (data.items || []).map((s) => ({ ...s, type: 'internal' }))
      total.value = data.total || 0
    } else if (type === 'external') {
      const data = await api.get('/external-skills', { params: { ...params, page: page.value, size: size.value } })
      items.value = (data.items || []).map((s) => ({ ...s, type: 'external' }))
      total.value = data.total || 0
    } else {
      const [skillsRes, extRes] = await Promise.all([
        api.get('/skills', { params }),
        api.get('/external-skills', { params }),
      ])
      const internal = (skillsRes?.items || []).map((s) => ({ ...s, type: 'internal' }))
      const external = (extRes?.items || []).map((s) => ({ ...s, type: 'external' }))
      const merged = [...internal, ...external].sort((a, b) => {
        const at = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
        const bt = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
        return bt - at
      })
      items.value = merged
      total.value = merged.length
    }
  } catch (e) {
    error.value = e.message || '加载失败'
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function toggleTag(id) {
  const idx = selectedTagIds.value.indexOf(id)
  if (idx >= 0) {
    selectedTagIds.value = selectedTagIds.value.filter((x) => x !== id)
  } else {
    selectedTagIds.value = [...selectedTagIds.value, id]
  }
  page.value = 1
  load()
}

function clearTagFilter() {
  selectedTagIds.value = []
  page.value = 1
  load()
}

watch(() => route.query.keyword, (v) => {
  if (v !== undefined) keyword.value = v || ''
})

function onPageChange() {
  if (typeFilter.value === 'all') return // 全部模式为前端分页，displayItems 自动更新
  load() // 内部/外部模式需重新请求
}

onMounted(() => {
  loadTags()
  load()
})
</script>

<style scoped>
.scrollbar-thin::-webkit-scrollbar {
  height: 4px;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}
</style>
