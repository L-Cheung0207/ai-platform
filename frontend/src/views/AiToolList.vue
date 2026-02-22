<template>
  <div>
    <PageHero
      variant="indigo"
      title="AI 工具大全"
      subtitle="精选 AI 写作、图像、视频、办公、开发等工具"
    >
      <div class="global-search mx-auto mb-4">
        <el-input
          v-model="keyword"
          placeholder="搜索 AI 工具"
          maxlength="200"
          size="large"
          clearable
          @keyup.enter="search"
        />
        <el-button type="primary" size="large" @click="search">搜索</el-button>
      </div>
      <div class="flex flex-wrap justify-center gap-2">
        <button
          v-for="c in allCategoryOptions"
          :key="c.value"
          :class="[
            'px-4 py-2 rounded-full text-sm font-medium transition-all duration-200',
            selectedCategory === c.value
              ? 'bg-primary text-white shadow-md'
              : 'bg-white text-[#6b7280] border border-[#e5e7eb] hover:border-primary hover:text-primary'
          ]"
          @click="selectCategory(c.value)"
        >
          {{ c.label }}
        </button>
      </div>
    </PageHero>

    <div class="max-w-[1280px] mx-auto px-6 py-6">
      <div v-if="loading" class="space-y-10">
        <div v-for="i in 3" :key="i">
          <div class="h-6 w-32 bg-[#e5e7eb] rounded mb-4 animate-pulse" />
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            <div v-for="j in 8" :key="j" class="rounded-xl border border-[#e5e7eb] bg-white p-5 animate-pulse">
              <div class="h-12 w-12 rounded-lg bg-[#e5e7eb] mb-3" />
              <div class="h-5 bg-[#e5e7eb] rounded w-2/3 mb-2" />
              <div class="h-4 bg-[#f3f4f6] rounded w-full" />
              <div class="h-4 bg-[#f3f4f6] rounded w-4/5 mt-2" />
            </div>
          </div>
        </div>
      </div>

      <!-- 分组模式：全部分类时按分类展示 -->
      <template v-else-if="isGroupedMode">
        <div v-if="groupedData.length === 0" class="text-center text-[#6b7280] py-12">
          {{ keyword ? '未找到匹配的工具' : '暂无 AI 工具' }}
        </div>
        <div v-else class="space-y-12">
          <section v-for="group in groupedData" :key="group.category" class="category-group">
            <h2 class="text-lg font-semibold text-[#1f2937] mb-4 flex items-center gap-2">
              <span class="w-1 h-5 bg-primary rounded-full" />
              {{ stripAiPrefix(group.category) }}
            </h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              <router-link
                v-for="t in group.items"
                :key="t.id"
                :to="'/ai-tools/' + t.id"
                class="flex gap-4 p-5 bg-white border border-[#e5e7eb] rounded-xl hover:border-primary hover:shadow-[0_4px_12px_rgba(37,99,235,0.1)] transition-all duration-200"
              >
                <img
                  v-if="t.logoUrl"
                  :src="t.logoUrl"
                  :alt="t.name"
                  class="w-12 h-12 rounded-lg object-contain shrink-0 bg-[#f9fafb]"
                />
                <div v-else class="w-12 h-12 rounded-lg bg-[#f3f4f6] shrink-0 flex items-center justify-center text-[#9ca3af] text-xl font-bold">
                  {{ (t.name || '?')[0] }}
                </div>
                <div class="min-w-0 flex-1">
                  <div class="font-semibold text-[#1f2937] truncate">{{ t.name }}</div>
                  <span v-if="toolDisplayTag(t)" class="tag-chip tag-chip--sm mt-1">{{ toolDisplayTag(t) }}</span>
                  <p class="text-[14px] text-[#6b7280] leading-relaxed line-clamp-2 mt-1">{{ t.summary || t.description || '暂无描述' }}</p>
                </div>
              </router-link>
            </div>
          </section>
        </div>
      </template>

      <!-- 单分类模式：分页列表 -->
      <template v-else>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          <router-link
            v-for="t in items"
            :key="t.id"
            :to="'/ai-tools/' + t.id"
            class="flex gap-4 p-5 bg-white border border-[#e5e7eb] rounded-xl hover:border-primary hover:shadow-[0_4px_12px_rgba(37,99,235,0.1)] transition-all duration-200"
          >
            <img
              v-if="t.logoUrl"
              :src="t.logoUrl"
              :alt="t.name"
              class="w-12 h-12 rounded-lg object-contain shrink-0 bg-[#f9fafb]"
            />
            <div v-else class="w-12 h-12 rounded-lg bg-[#f3f4f6] shrink-0 flex items-center justify-center text-[#9ca3af] text-xl font-bold">
              {{ (t.name || '?')[0] }}
            </div>
            <div class="min-w-0 flex-1">
              <div class="font-semibold text-[#1f2937] truncate">{{ t.name }}</div>
              <span v-if="toolDisplayTag(t)" class="tag-chip tag-chip--sm mt-1">{{ toolDisplayTag(t) }}</span>
              <p class="text-[14px] text-[#6b7280] leading-relaxed line-clamp-2 mt-1">{{ t.summary || t.description || '暂无描述' }}</p>
            </div>
          </router-link>
        </div>

        <div class="admin-pagination mt-8">
          <el-pagination
            v-if="total > 0 && !loading"
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="loadList"
            @size-change="onSizeChange"
          />
        </div>
      </template>

      <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
      <p v-if="!loading && !error && !isGroupedMode && items.length === 0" class="text-center text-[#6b7280] py-12">暂无 AI 工具</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(24)
const keyword = ref('')
const selectedCategory = ref('')
const tags = ref([])
const groupedData = ref([])
const loading = ref(false)
const error = ref('')

const isGroupedMode = computed(() => !selectedCategory.value)

function stripAiPrefix(name) {
  if (!name || typeof name !== 'string') return name
  return name.startsWith('AI') ? name.slice(2) : name
}

function toolDisplayTag(t) {
  const s = (t.tagNames && t.tagNames.split(',')[0]?.trim()) || t.categoryName
  return s ? stripAiPrefix(s) : null
}

const allCategoryOptions = computed(() => {
  const opts = [{ label: '全部', value: '' }]
  for (const t of tags.value.slice(0, 10)) {
    opts.push({ label: stripAiPrefix(t), value: t })
  }
  return opts
})

async function loadGrouped() {
  loading.value = true
  error.value = ''
  try {
    const params = keyword.value ? { keyword: keyword.value } : {}
    groupedData.value = await api.get('/ai-tools/grouped', { params })
  } catch (e) {
    error.value = e.message || '加载失败'
    groupedData.value = []
  } finally {
    loading.value = false
  }
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (selectedCategory.value) params.category = selectedCategory.value
    const data = await api.get('/ai-tools', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    tags.value = await api.get('/ai-tools/tags')
  } catch (_) {
    tags.value = []
  }
}

function selectCategory(val) {
  selectedCategory.value = val
}

function search() {
  page.value = 1
  if (isGroupedMode.value) {
    loadGrouped()
  } else {
    loadList()
  }
}

function onSizeChange() {
  page.value = 1
  loadList()
}

watch(selectedCategory, (val) => {
  page.value = 1
  if (!val) {
    loadGrouped()
  } else {
    loadList()
  }
})

onMounted(() => {
  loadTags()
  if (isGroupedMode.value) {
    loadGrouped()
  } else {
    loadList()
  }
})
</script>

<style scoped>
.admin-pagination {
  display: flex;
  justify-content: flex-end;
}
.admin-pagination :deep(.el-pagination) {
  font-weight: 500;
}
.category-group {
  scroll-margin-top: 1rem;
}
</style>
