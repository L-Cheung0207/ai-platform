<template>
  <div class="pt-8">
    <div class="max-w-[1400px] mx-auto px-6 py-8">
    <template v-if="skill">
      <!-- 标题区：参考 ai.codefather.cn -->
      <h1 class="text-[28px] font-bold text-[#1f2937] mb-3 tracking-tight">{{ skill.name }}</h1>
      <div
        class="markdown-body description-body text-[16px] text-[#6b7280] leading-relaxed mb-6"
        v-html="renderedDescription"
      />

      <!-- 标签 -->
      <div v-if="skill.tagNames?.length" class="flex flex-wrap gap-2 mb-8">
        <span v-for="t in skill.tagNames" :key="t" class="tag-chip">{{ t }}</span>
        <span class="tag-chip tag-chip--muted">内部</span>
      </div>

      <!-- 快捷安装 -->
      <section class="mb-8">
        <h2 class="text-[18px] font-semibold text-[#1f2937] mb-3">快捷安装</h2>
        <p class="text-[14px] text-[#6b7280] mb-2">复制到终端去安装 Skill</p>
        <div class="flex gap-3 items-center rounded-xl border border-[#e5e7eb] bg-white p-4">
          <code class="flex-1 min-w-0 text-[14px] text-[#374151] font-mono truncate" :title="skill.cloneCommand">{{ skill.cloneCommand }}</code>
          <button
            type="button"
            title="复制"
            class="flex-shrink-0 p-2 rounded text-gray-400 hover:text-gray-600 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300"
            @click="copy(skill.cloneCommand)"
          >
            <span class="sr-only">复制</span>
            <svg viewBox="64 64 896 896" class="w-4 h-4" fill="currentColor" aria-hidden="true">
              <path d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z" />
            </svg>
          </button>
        </div>
      </section>

      <!-- Markdown 文档 -->
      <section v-if="skill.contentMd" class="mb-8">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-[18px] font-semibold text-[#1f2937]">文档</h2>
          <button
            type="button"
            title="复制"
            class="flex-shrink-0 p-2 rounded text-gray-400 hover:text-gray-600 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300"
            @click="copy(skill.contentMd)"
          >
            <span class="sr-only">复制</span>
            <svg viewBox="64 64 896 896" class="w-4 h-4" fill="currentColor" aria-hidden="true">
              <path d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z" />
            </svg>
          </button>
        </div>
        <div
          class="markdown-body rounded-xl border border-[#e5e7eb] bg-white p-6"
          v-html="renderedMd"
        />
      </section>

      <!-- 上传者 -->
      <div class="text-[14px] text-[#9ca3af]">
        <span v-if="skill.uploaderName">上传者：{{ skill.uploaderName }}</span>
      </div>
    </template>

    <template v-else>
      <div v-if="loading" class="py-12 text-center text-[#6b7280]">加载中…</div>
      <el-alert v-else type="error" :title="error || '未找到'" show-icon />
    </template>
    </div>
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
const skill = ref(null)
const loading = ref(true)
const error = ref('')

const renderedDescription = computed(() => {
  const raw = skill.value?.description || '暂无描述'
  try {
    const html = marked.parse(raw, { gfm: true })
    return DOMPurify.sanitize(html)
  } catch {
    return DOMPurify.sanitize(raw)
  }
})

const renderedMd = computed(() => {
  const md = skill.value?.contentMd
  if (!md) return ''
  try {
    const html = marked.parse(md, { gfm: true })
    return DOMPurify.sanitize(html)
  } catch {
    return ''
  }
})

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (_) {
    ElMessage.error('复制失败')
  }
}

onMounted(async () => {
  try {
    skill.value = await api.get('/skills/' + route.params.id)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.markdown-body :deep(h1) { font-size: 1.5em; margin: 1em 0 0.5em; font-weight: 600; }
.markdown-body :deep(h2) { font-size: 1.25em; margin: 1.25em 0 0.5em; font-weight: 600; }
.markdown-body :deep(h3) { font-size: 1.1em; margin: 1em 0 0.5em; font-weight: 600; }
.markdown-body :deep(p) { margin: 0.75em 0; line-height: 1.6; color: #374151; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 0.75em 0; padding-left: 1.5em; }
.markdown-body :deep(li) { margin: 0.25em 0; }
.markdown-body :deep(code) { background: #f3f4f6; padding: 0.2em 0.4em; border-radius: 4px; font-size: 0.9em; }
.markdown-body :deep(pre) { background: #1f2937; color: #e5e7eb; padding: 1em; border-radius: 8px; overflow-x: auto; margin: 1em 0; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-body :deep(blockquote) { border-left: 4px solid #e5e7eb; margin: 1em 0; padding-left: 1em; color: #6b7280; }
.markdown-body :deep(a) { color: #2563eb; text-decoration: none; }
.markdown-body :deep(a:hover) { text-decoration: underline; }
.markdown-body :deep(table) { border-collapse: collapse; width: 100%; margin: 1em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #e5e7eb; padding: 0.5em 0.75em; text-align: left; }
.markdown-body :deep(th) { background: #f9fafb; font-weight: 600; }
</style>
