<template>
  <div>
    <PageHero
      variant="purple"
      title="Rule 规则大全"
      subtitle="平台内纯文本 Rule，一键复制使用"
    >
      <div class="global-search">
        <el-input
          v-model="keyword"
          placeholder="搜索 Rule（最多 200 字）"
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
      <!-- 分类标签：参考 ai.codefather.cn 横向 chips -->
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

      <!-- 网格卡片：参考 ai.codefather.cn 多列布局 -->
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

      <div
        v-else
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"
      >
        <router-link
          v-for="r in items"
          :key="r.id"
          :to="'/rules/' + r.id"
          class="block p-5 bg-white border border-[#e5e7eb] rounded-xl hover:border-primary hover:shadow-[0_4px_12px_rgba(37,99,235,0.1)] transition-all duration-200"
        >
          <div class="font-semibold text-[16px] text-[#1f2937] mb-2 truncate">{{ r.name }}</div>
          <div class="flex flex-wrap gap-2 mb-2">
            <span v-for="t in (r.tagNames || []).slice(0, 4)" :key="t" class="tag-chip tag-chip--sm">{{ t }}</span>
          </div>
          <p class="text-[14px] text-[#6b7280] leading-relaxed line-clamp-2">{{ (r.content || '').slice(0, 80) }}{{ (r.content || '').length > 80 ? '…' : '' }}</p>
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
      <p v-if="!loading && !error && items.length === 0" class="text-center text-[#6b7280] py-12">暂无 Rule</p>
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
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const tagList = ref([])
const selectedTagIds = ref([])

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

async function loadTags() {
  try {
    const list = await api.get('/tags', { params: { forEntity: 'rules' } })
    tagList.value = list || []
  } catch {
    tagList.value = []
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (selectedTagIds.value.length > 0) params.tags = selectedTagIds.value
    const data = await api.get('/rules', { params })
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
