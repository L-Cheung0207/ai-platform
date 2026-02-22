<template>
  <div class="pt-8">
    <div class="max-w-[1400px] mx-auto px-6 py-8">
    <template v-if="rule">
      <!-- 标题区：参考 ai.codefather.cn -->
      <h1 class="text-[28px] font-bold text-[#1f2937] mb-3 tracking-tight">{{ rule.name }}</h1>

      <!-- 标签 -->
      <div v-if="rule.tagNames?.length" class="flex flex-wrap gap-2 mb-8">
        <span v-for="t in rule.tagNames" :key="t" class="tag-chip">{{ t }}</span>
      </div>

      <!-- 正文（一键复制） -->
      <section class="mb-8">
        <h2 class="text-[18px] font-semibold text-[#1f2937] mb-3">Rule 正文</h2>
        <p class="text-[14px] text-[#6b7280] mb-2">复制以下内容到 Cursor Rules 等场景使用</p>
        <div class="flex gap-3 items-start rounded-xl border border-[#e5e7eb] bg-white p-4">
          <pre class="flex-1 min-w-0 text-[14px] text-[#374151] whitespace-pre-wrap break-words font-mono m-0">{{ rule.content }}</pre>
          <el-button type="primary" size="small" @click="copy(rule.content)">复制</el-button>
        </div>
      </section>

      <!-- 上传者 -->
      <div class="text-[14px] text-[#9ca3af]">
        <span v-if="rule.uploaderName">上传者：{{ rule.uploaderName }}</span>
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const rule = ref(null)
const loading = ref(true)
const error = ref('')

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
    rule.value = await api.get('/rules/' + route.params.id)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>
