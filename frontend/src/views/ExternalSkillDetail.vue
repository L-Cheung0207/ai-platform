<template>
  <div class="pt-8">
    <template v-if="!item && !loading">
      <div class="max-w-[1400px] mx-auto px-6 py-8">
        <el-alert type="error" :title="error || '未找到'" show-icon />
      </div>
    </template>
    <template v-else-if="loading">
      <div class="max-w-[1400px] mx-auto px-6 py-12 text-center text-[#6b7280]">加载中…</div>
    </template>
    <template v-else>
      <!-- 两栏布局：主内容 + 右侧边栏 -->
      <div class="max-w-[1400px] mx-auto px-6 py-8 flex gap-8">
        <!-- 左侧主内容 -->
        <article class="flex-1 min-w-0">
          <!-- 标题行：技能名 + 右侧 GitHub/来源 按钮 -->
          <div class="flex items-center justify-between gap-4 mb-3">
            <h1 class="text-[28px] font-bold text-[#1f2937] tracking-tight min-w-0">{{ item.name }}</h1>
            <div v-if="item.sourceUrl" class="flex items-center flex-shrink-0">
              <a
                :href="item.sourceUrl"
                target="_blank"
                rel="noopener"
                class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#2563eb] text-white text-[14px] font-medium hover:bg-[#1d4ed8] transition-colors shadow-sm"
              >
                <svg v-if="isGitHubUrl" stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 496 512" class="w-4 h-4" aria-hidden="true">
                  <path d="M165.9 397.4c0 2-2.3 3.6-5.2 3.6-3.3.3-5.6-1.3-5.6-3.6 0-2 2.3-3.6 5.2-3.6 3-.3 5.6 1.3 5.6 3.6zm-31.1-4.5c-.7 2 1.3 4.3 4.3 4.9 2.6 1 5.6 0 6.2-2s-1.3-4.3-4.3-5.2c-2.6-.7-5.5.3-6.2 2.3zm44.2-1.7c-2.9.7-4.9 2.6-4.6 4.9.3 2 2.9 3.3 5.9 2.6 2.9-.7 4.9-2.6 4.6-4.6-.3-1.9-3-3.2-5.9-2.9zM244.8 8C106.1 8 0 113.3 0 252c0 110.9 69.8 205.8 169.5 239.2 12.8 2.3 17.3-5.6 17.3-12.1 0-6.2-.3-40.4-.3-61.4 0 0-70 15-84.7-29.8 0 0-11.4-29.1-27.8-36.6 0 0-22.9-15.7 1.6-15.4 0 0 24.9 2 38.6 25.8 21.9 38.6 58.6 27.5 72.9 20.9 2.3-16 8.8-27.1 16-33.7-55.9-6.2-112.3-14.3-112.3-110.5 0-27.5 7.6-41.3 23.6-58.9-2.6-6.5-11.1-33.3 2.6-67.9 20.9-6.5 69 27 69 27 20-5.6 41.5-8.5 62.8-8.5s42.8 2.9 62.8 8.5c0 0 48.1-33.6 69-27 13.7 34.7 5.2 61.4 2.6 67.9 16 17.7 25.8 31.5 25.8 58.9 0 96.5-58.9 104.2-114.8 110.5 9.2 7.9 17 22.9 17 46.4 0 33.7-.3 75.4-.3 83.6 0 6.5 4.6 14.4 17.3 12.1C428.2 457.8 496 362.9 496 252 496 113.3 383.5 8 244.8 8zM97.2 352.9c-1.3 1-1 3.3.7 5.2 1.6 1.6 3.9 2.3 5.2 1 1.3-1 1-3.3-.7-5.2-1.6-1.6-3.9-2.3-5.2-1zm-10.8-8.1c-.7 1.3.3 2.9 2.3 3.9 1.6 1 3.6.7 4.3-.7.7-1.3-.3-2.9-2.3-3.9-2-.6-3.6-.3-4.3.7zm32.4 35.6c-1.6 1.3-1 4.3 1.3 6.2 2.3 2.3 5.2 2.6 6.5 1 1.3-1.3.7-4.3-1.3-6.2-2.2-2.3-5.2-2.6-6.5-1zm-11.4-14.7c-1.6 1-1.6 3.6 0 5.9 1.6 2.3 4.3 3.3 5.6 2.3 1.6-1.3 1.6-3.9 0-6.2-1.4-2.3-4-3.3-5.6-2z" />
                </svg>
                <span>{{ sourceLinkText }}</span>
              </a>
            </div>
          </div>
          <p class="text-[16px] text-[#6b7280] leading-relaxed mb-4 line-clamp-3">
            {{ shortDescription }}
          </p>

          <!-- 标签 -->
          <div class="flex flex-wrap gap-2 mb-8">
            <span v-for="t in item.tagNames" :key="t" class="tag-chip">{{ t }}</span>
            <span class="tag-chip tag-chip--muted">外部</span>
          </div>

          <!-- 文档区：Skills.MD 风格，带复制 -->
          <section class="rounded-xl border border-[#e5e7eb] bg-white overflow-hidden shadow-sm">
            <div class="flex items-center justify-between px-5 py-3 border-b border-[#e5e7eb] bg-[#fafafa]">
              <h2 class="text-[16px] font-semibold text-[#1f2937] flex items-center gap-2">
                <span>Skills.MD</span>
              </h2>
            </div>
            <div
              class="markdown-body px-6 py-5 text-[15px] text-[#374151] leading-relaxed"
              v-html="renderedContent"
            />
          </section>
        </article>

        <!-- 右侧边栏 -->
        <aside class="w-[320px] flex-shrink-0 space-y-6">
          <!-- 快捷安装 -->
          <div class="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
            <h3 class="text-[16px] font-semibold text-[#1f2937] mb-2">快捷安装</h3>
            <p class="text-[13px] text-[#6b7280] mb-3">复制到终端去安装 Skill</p>
            <div class="flex gap-2 items-center">
              <code class="flex-1 min-w-0 text-[13px] text-[#374151] font-mono bg-[#f3f4f6] rounded-lg px-3 py-2.5 truncate" :title="item.installCommand">{{ item.installCommand }}</code>
              <button
                type="button"
                title="复制"
                class="flex-shrink-0 p-2 rounded text-gray-400 hover:text-gray-600 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300"
                @click="copy(item.installCommand)"
              >
                <span class="sr-only">复制</span>
                <svg viewBox="64 64 896 896" class="w-4 h-4" fill="currentColor" aria-hidden="true">
                  <path d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Skills 推荐 -->
          <div class="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
            <h3 class="text-[16px] font-semibold text-[#1f2937] mb-3">Skills 推荐</h3>
            <ul class="space-y-2">
              <li v-for="s in recommendedList" :key="s.id">
                <router-link
                  :to="'/external-skills/' + s.id"
                  class="block text-[14px] text-[#374151] hover:text-primary hover:underline"
                >
                  {{ s.name }}
                </router-link>
                <p class="text-[12px] text-[#9ca3af] mt-0.5 line-clamp-2">{{ s.description || '' }}</p>
              </li>
            </ul>
            <router-link
              to="/external-skills"
              class="mt-3 inline-block text-[13px] text-primary hover:underline"
            >
              查看更多 →
            </router-link>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const route = useRoute()
const item = ref(null)
const loading = ref(true)
const error = ref('')
const recommendedList = ref([])

/** 简短描述（首段或前 120 字） */
const shortDescription = computed(() => {
  const d = item.value?.description || ''
  const plain = d.replace(/#{1,6}\s/g, '').replace(/\*\*([^*]+)\*\*/g, '$1').replace(/\n+/g, ' ').trim()
  if (plain.length <= 120) return plain || '暂无描述'
  return plain.slice(0, 120) + '…'
})

/** 规范化爬取来的 Markdown：补全缺失表头、处理转义 */
function normalizeScrapedMarkdown(raw) {
  if (!raw || typeof raw !== 'string') return raw
  let s = raw.trim()
  const firstLine = s.split('\n')[0]?.trim() ?? ''
  if (/^\|[\s\-|]+\|$/.test(firstLine)) {
    s = '| 属性 | 值 |\n| --- | --- |\n' + s.slice(s.indexOf('\n') + 1)
  }
  s = s.replace(/\\&/g, '&')
  return s
}

/** 文档区渲染：优先 content（正文），无则用 description */
const renderedContent = computed(() => {
  const raw = item.value?.content || item.value?.description || '暂无描述'
  const normalized = normalizeScrapedMarkdown(raw)
  try {
    const html = marked.parse(normalized, { gfm: true })
    return DOMPurify.sanitize(html)
  } catch {
    return DOMPurify.sanitize(normalized)
  }
})

const isGitHubUrl = computed(() => (item.value?.sourceUrl || '').toLowerCase().includes('github'))
const sourceLinkText = computed(() => {
  const url = item.value?.sourceUrl || ''
  return url.toLowerCase().includes('github') ? '访问 GitHub' : '访问来源'
})

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (_) {
    ElMessage.error('复制失败')
  }
}

async function loadRecommended() {
  try {
    const data = await api.get('/external-skills', { params: { page: 1, size: 10 } })
    const list = (data.items || []).filter((s) => s.id !== item.value?.id).slice(0, 6)
    recommendedList.value = list
  } catch {
    recommendedList.value = []
  }
}

onMounted(async () => {
  try {
    item.value = await api.get('/external-skills/' + route.params.id)
    await loadRecommended()
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.markdown-body :deep(h1) { font-size: 1.35em; margin: 0.75em 0 0.5em; font-weight: 600; }
.markdown-body :deep(h2) { font-size: 1.2em; margin: 1em 0 0.5em; font-weight: 600; }
.markdown-body :deep(h3) { font-size: 1.05em; margin: 0.75em 0 0.5em; font-weight: 600; }
.markdown-body :deep(p) { margin: 0.5em 0; line-height: 1.6; color: #374151; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 0.5em 0; padding-left: 1.5em; }
.markdown-body :deep(li) { margin: 0.2em 0; }
.markdown-body :deep(code) { background: #f3f4f6; padding: 0.2em 0.4em; border-radius: 4px; font-size: 0.9em; color: #374151; }
.markdown-body :deep(pre) { background: #1f2937; color: #e5e7eb; padding: 1em; border-radius: 8px; overflow-x: auto; margin: 0.75em 0; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-body :deep(blockquote) { border-left: 4px solid #e5e7eb; margin: 0.75em 0; padding-left: 1em; color: #6b7280; }
.markdown-body :deep(a) { color: var(--el-color-primary); text-decoration: none; }
.markdown-body :deep(a:hover) { text-decoration: underline; }
.markdown-body :deep(table) { border-collapse: collapse; width: 100%; margin: 0.75em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #e5e7eb; padding: 0.5em 0.75em; text-align: left; }
.markdown-body :deep(th) { background: #f9fafb; font-weight: 600; }
</style>
