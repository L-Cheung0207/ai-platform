<template>
  <div class="pt-8">
    <div class="max-w-[1400px] mx-auto px-6 py-8">
      <template v-if="item">
        <!-- 标题行：Logo + 名称 + 右侧访问官网（参考 Skill 详情） -->
        <div class="flex items-center justify-between gap-4 mb-3">
          <div class="flex items-center gap-4 min-w-0 flex-1">
            <img
              v-if="item.logoUrl"
              :src="item.logoUrl"
              :alt="item.name"
              class="w-14 h-14 rounded-xl object-contain bg-[#f9fafb] shrink-0 border border-[#e5e7eb]"
            />
            <div
              v-else
              class="w-14 h-14 rounded-xl bg-[#f3f4f6] shrink-0 flex items-center justify-center text-2xl font-bold text-[#9ca3af] border border-[#e5e7eb]"
            >
              {{ (item.name || '?')[0] }}
            </div>
            <h1 class="text-[28px] font-bold text-[#1f2937] tracking-tight min-w-0 truncate">{{ item.name }}</h1>
          </div>
          <a
            v-if="item.url"
            :href="item.url"
            target="_blank"
            rel="noopener nofollow"
            class="flex-shrink-0 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#2563eb] text-white text-[14px] font-medium hover:bg-[#1d4ed8] transition-colors shadow-sm"
          >
            访问官网
            <span class="opacity-80">→</span>
          </a>
        </div>

        <!-- 描述（与 Skill 详情同风格） -->
        <p class="text-[16px] text-[#6b7280] leading-relaxed mb-6">
          {{ item.summary || item.description || '暂无描述' }}
        </p>

        <!-- 标签 -->
        <div v-if="item.tagNames" class="flex flex-wrap gap-2 mb-8">
          <span
            v-for="tag in (item.tagNames || '').split(',').filter(Boolean)"
            :key="tag"
            class="tag-chip"
          >
            {{ tag.trim() }}
          </span>
        </div>

        <!-- 详细介绍（卡片样式，与 Skill 文档区一致） -->
        <section v-if="item.content" class="mb-8">
          <h2 class="text-[18px] font-semibold text-[#1f2937] mb-3">详细介绍</h2>
          <div class="rounded-xl border border-[#e5e7eb] bg-white p-6">
            <div class="text-[15px] text-[#374151] leading-relaxed whitespace-pre-wrap">{{ item.content }}</div>
          </div>
        </section>

        <!-- 工具推荐 -->
        <section v-if="recommendedTools.length" class="mb-8">
          <h2 class="text-[18px] font-semibold text-[#1f2937] mb-3">工具推荐</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <router-link
              v-for="t in recommendedTools"
              :key="t.id"
              :to="'/ai-tools/' + t.id"
              class="flex gap-3 p-4 rounded-xl border border-[#e5e7eb] bg-white hover:border-primary hover:shadow-[0_4px_12px_rgba(37,99,235,0.08)] transition-all"
            >
              <img
                v-if="t.logoUrl"
                :src="t.logoUrl"
                :alt="t.name"
                class="w-12 h-12 rounded-lg object-contain shrink-0 bg-[#f9fafb]"
              />
              <div
                v-else
                class="w-12 h-12 rounded-lg bg-[#f3f4f6] shrink-0 flex items-center justify-center text-lg font-bold text-[#9ca3af]"
              >
                {{ (t.name || '?')[0] }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="font-semibold text-[#1f2937] truncate">{{ t.name }}</div>
                <p class="text-[13px] text-[#6b7280] line-clamp-2 mt-1">{{ t.summary || t.description || '' }}</p>
              </div>
            </router-link>
          </div>
        </section>
      </template>

      <template v-else>
        <div v-if="loading" class="py-12 text-center text-[#6b7280]">加载中…</div>
        <el-alert v-else type="error" :title="error || '未找到'" show-icon />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'

const route = useRoute()
const item = ref(null)
const recommendedTools = ref([])
const loading = ref(true)
const error = ref('')

async function loadDetail() {
  try {
    item.value = await api.get('/ai-tools/' + route.params.id)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadRecommended() {
  if (!item.value?.id) return
  try {
    const params = { page: 1, size: 12 }
    if (item.value.categoryName) params.category = item.value.categoryName
    const data = await api.get('/ai-tools', { params })
    const list = (data.items || []).filter((t) => t.id !== item.value.id)
    recommendedTools.value = list.slice(0, 6)
  } catch {
    recommendedTools.value = []
  }
}

onMounted(async () => {
  await loadDetail()
  if (item.value) loadRecommended()
})

watch(() => route.params.id, async () => {
  loading.value = true
  error.value = ''
  item.value = null
  recommendedTools.value = []
  await loadDetail()
  if (item.value) loadRecommended()
})
</script>
