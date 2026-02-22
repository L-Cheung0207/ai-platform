<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-semibold text-gray-800">LLM 排行榜</h1>
      <router-link to="/llm-leaderboard" class="el-button el-button--default">查看公开排行榜</router-link>
    </div>

    <el-card shadow="never" class="max-w-2xl">
      <template #header>
        <span>数据来源：OpenLM Chatbot Arena</span>
      </template>
      <p class="text-sm text-gray-600 mb-4">
        从 <a href="https://openlm.ai/chatbot-arena/" target="_blank" rel="noopener" class="text-primary hover:underline">openlm.ai/chatbot-arena</a> 爬取大语言模型排行榜（Arena Elo、Coding、Vision、AAII、MMLU-Pro、ARC-AGI 等）。爬取为全量替换，会先清空再写入新数据。
      </p>
      <div class="flex items-center gap-4">
        <el-button
          type="primary"
          :loading="scraping"
          :disabled="statusData?.status === 'running'"
          @click="startScrape"
        >
          {{ statusData?.status === 'running' ? '爬取中…' : '一键爬取' }}
        </el-button>
        <span v-if="statusData?.status" class="text-sm text-gray-500">
          状态：{{ statusText }}
          <span v-if="statusData?.finishedAt" class="ml-1">（{{ statusData.finishedAt }}）</span>
        </span>
      </div>
      <el-alert v-if="statusData?.error" type="error" :title="statusData.error" show-icon class="mt-4" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../services/api'

const statusData = ref(null)
const scraping = ref(false)

const statusText = computed(() => {
  const s = statusData.value
  if (!s) return ''
  if (s.status === 'running') return '运行中'
  if (s.status === 'completed') return `已完成，共 ${s.added ?? 0} 条`
  if (s.status === 'failed') return '失败'
  return s.status
})

async function fetchStatus() {
  try {
    statusData.value = await api.get('/admin/scrape-status/llm-leaderboard')
  } catch {
    statusData.value = null
  }
}

async function startScrape() {
  scraping.value = true
  try {
    await api.post('/admin/scrape/llm-leaderboard')
    statusData.value = { status: 'running' }
    await fetchStatus()
    let timerId = setInterval(async () => {
      await fetchStatus()
      if (statusData.value?.status === 'completed' || statusData.value?.status === 'failed') {
        clearInterval(timerId)
        scraping.value = false
      }
    }, 2500)
    setTimeout(() => {
      clearInterval(timerId)
      scraping.value = false
    }, 180000)
  } catch (e) {
    scraping.value = false
  }
}

onMounted(fetchStatus)
</script>
