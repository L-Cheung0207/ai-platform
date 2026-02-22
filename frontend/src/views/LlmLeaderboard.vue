<template>
  <div>
    <PageHero
      variant="teal"
      title="大语言模型排行榜"
      subtitle="数据来源 OpenLM Chatbot Arena，基于 Arena Elo、Coding、Vision、AAII、MMLU-Pro、ARC-AGI 等指标"
    >
      <div class="global-search">
        <el-input
          v-model="keyword"
          placeholder="搜索模型名称或机构"
          maxlength="100"
          clearable
          size="large"
          @keyup.enter="search"
        />
        <el-button type="primary" size="large" @click="search">搜索</el-button>
      </div>
    </PageHero>

    <div class="max-w-[1400px] mx-auto px-6 py-8 pb-10">
      <div v-if="loading" class="overflow-x-auto">
        <div class="h-10 bg-[#e5e7eb] rounded w-full mb-2 animate-pulse" />
        <div v-for="i in 15" :key="i" class="h-12 bg-[#f3f4f6] rounded w-full mb-1 animate-pulse" />
      </div>

      <div v-else class="overflow-x-auto rounded-xl border border-[#e5e7eb] bg-white shadow-sm">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-[#e5e7eb] bg-[#f9fafb]">
              <th class="px-4 py-3 text-xs font-semibold text-[#6b7280] tracking-wider">排名</th>
              <th class="px-4 py-3 text-xs font-semibold text-[#6b7280] tracking-wider">模型</th>
              <th
                v-for="col in sortableColumns"
                :key="col.field"
                class="px-4 py-3 text-xs font-semibold text-[#6b7280] tracking-wider  text-center cursor-pointer select-none hover:text-primary hover:bg-[#f3f4f6] transition-colors"
                :class="{ 'text-primary': sortBy === col.field }"
                @click="toggleSort(col.field)"
              >
                {{ col.label }}
                <span v-if="sortBy === col.field" class="ml-0.5">{{ sortOrder === 'desc' ? '↓' : '↑' }}</span>
              </th>
              <th class="px-4 py-3 text-xs font-semibold text-[#6b7280] tracking-wider min-w-[100px]">机构</th>
              <th class="px-4 py-3 text-xs font-semibold text-[#6b7280] tracking-wider min-w-[80px]">许可证</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, idx) in items"
              :key="row.id"
              class="border-b border-[#e5e7eb] hover:bg-[#f9fafb] transition-colors"
            >
              <td class="px-4 py-3 text-[#6b7280] text-sm">
                {{ (page - 1) * size + idx + 1 }}
              </td>
              <td class="px-4 py-3">
                <span v-if="row.rankBadge" class="mr-1" :title="row.rankBadge">{{ row.rankBadge }}</span>
                <a
                  v-if="row.modelUrl"
                  :href="row.modelUrl"
                  target="_blank"
                  rel="noopener"
                  class="font-medium text-primary hover:underline"
                >
                  {{ row.modelName }}
                </a>
                <span v-else class="font-medium text-[#1f2937]">{{ row.modelName }}</span>
              </td>
              <td class="px-4 py-3 text-center font-mono text-sm">{{ formatNum(row.arenaElo) }}</td>
              <td class="px-4 py-3 text-center font-mono text-sm">{{ formatNum(row.coding) }}</td>
              <td class="px-4 py-3 text-center font-mono text-sm">{{ formatNum(row.vision) }}</td>
              <td class="px-4 py-3 text-center font-mono text-sm">{{ formatNum(row.aaii) }}</td>
              <td class="px-4 py-3 text-center font-mono text-sm">{{ formatNum(row.mmluPro) }}</td>
              <td class="px-4 py-3 text-center font-mono text-sm">{{ formatNum(row.arcAgi) }}</td>
              <td class="px-4 py-3 text-sm text-[#4b5563]">{{ row.organization || '—' }}</td>
              <td class="px-4 py-3 text-sm text-[#6b7280]">{{ row.licenseName || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex justify-center mt-6">
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
      <p v-if="!loading && !error && items.length === 0" class="text-center text-[#6b7280] py-12">
        暂无排行榜数据，请由管理员在后台执行「LLM 排行榜」爬取。
      </p>
    </div>

    <!-- 指标说明：排版与 MCP 页底部常见问题一致 -->
    <section class="mcp-faq">
      <div class="mcp-faq-inner">
        <h2 class="mcp-faq-title">
          <span class="mcp-faq-title-icon" aria-hidden="true">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </span>
          指标说明
        </h2>
        <dl class="mcp-faq-grid">
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">竞技场 Elo（Arena Elo）</dt>
            <dd class="mcp-faq-a">来自 Chatbot Arena 的众包对战评分。用户在随机双盲对比中为不同模型的回答投票，平台基于数百万次投票用 Elo 算法计算各模型相对实力，分数越高表示在用户偏好中表现越好。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">AAII</dt>
            <dd class="mcp-faq-a">Artificial Analysis Intelligence Index（人工分析智能指数）v3，聚合 MMLU-Pro、Humanity’s Last Exam、AA-LCR、GPQA Diamond、AIME、IFBench、SciCode、LiveCodeBench、Terminal-Bench Hard、τ²-Bench Telecom 等 10 项挑战性评估的综合指数。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">MMLU-Pro</dt>
            <dd class="mcp-faq-a">Massive Multitask Language Understanding 的进阶版，面向多任务语言理解与推理的基准测试，涵盖多学科与高阶推理能力。</dd>
          </div>
          <div class="mcp-faq-card">
            <dt class="mcp-faq-q">ARC-AGI</dt>
            <dd class="mcp-faq-a">AI2 Reasoning Challenge — Artificial General Intelligence（通用人工智能基准）v2，用于衡量模型的流体智力与推理能力，题目来自科学考试等需要强推理的题型。</dd>
          </div>
        </dl>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHero from '../components/PageHero.vue'
import api from '../services/api'

const sortableColumns = [
  { field: 'arenaElo', label: '竞技场 Elo' },
  { field: 'coding', label: '编程' },
  { field: 'vision', label: '视觉' },
  { field: 'aaii', label: 'AAII' },
  { field: 'mmluPro', label: 'MMLU-Pro' },
  { field: 'arcAgi', label: 'ARC-AGI' },
]
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(50)
const keyword = ref('')
const sortBy = ref('coding')
const sortOrder = ref('desc')
const loading = ref(false)
const error = ref('')

function formatNum(v) {
  if (v == null) return '—'
  return Number(v)
}

function toggleSort(field) {
  if (sortBy.value === field) {
    sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    sortBy.value = field
    sortOrder.value = 'desc'
  }
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value, sortBy: sortBy.value, sortOrder: sortOrder.value }
    if (keyword.value) params.keyword = keyword.value
    const data = await api.get('/llm-leaderboard', { params })
    items.value = data.items || []
    total.value = data.total ?? 0
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

<style scoped>
/* 与 MCP 页底部常见问题一致 */
.mcp-faq {
  background: #fff;
  border-top: 1px solid #e5e7eb;
  margin-top: 2.5rem;
  padding: 3.5rem 1.5rem;
}
.mcp-faq-inner {
  max-width: 960px;
  margin: 0 auto;
}
.mcp-faq-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-size: 1.375rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2rem;
  text-align: center;
}
.mcp-faq-title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.1);
  color: rgb(37, 99, 235);
}
.mcp-faq-title-icon svg {
  width: 18px;
  height: 18px;
}
.mcp-faq-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}
@media (min-width: 640px) {
  .mcp-faq-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1.25rem;
  }
}
.mcp-faq-card {
  padding: 1.25rem 1.5rem;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}
.mcp-faq-card:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.mcp-faq-q {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #334155;
  margin: 0 0 0.5rem;
  line-height: 1.4;
}
.mcp-faq-a {
  margin: 0;
  font-size: 0.875rem;
  color: #64748b;
  line-height: 1.65;
}
</style>
