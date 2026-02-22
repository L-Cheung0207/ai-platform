<template>
  <div class="news-list">
    <PageHero
      variant="blue"
      title="每日 AI 资讯"
      subtitle="AI 行业动态与前沿资讯"
    />

    <div class="news-content">
      <div class="news-layout">
        <div class="news-main">
          <template v-for="(group, dateKey) in groupedByDate" :key="dateKey">
            <h3 class="news-date-header">{{ formatDateHeader(dateKey) }}</h3>
            <div class="news-date-group">
              <router-link
                v-for="(n, i) in group"
                :key="n.id"
                :to="'/news/' + n.id"
                class="news-card"
                :style="{ animationDelay: `${i * 30}ms` }"
              >
                <h4 class="news-card-title">{{ n.title }}</h4>
                <p v-if="n.summary || n.sourceName" class="news-card-summary">{{ (n.summary || n.sourceName || '').trim() }}</p>
              </router-link>
            </div>
          </template>
        </div>

        <aside class="news-sidebar">
          <section class="news-calendar-section">
            <h3 class="news-calendar-title">根据指定日期查询 AI 资讯</h3>
            <p v-if="selectedDate" class="news-recent-link">
              <a href="#" @click.prevent="showRecent10Days">查看最近 10 天</a>
            </p>
            <el-calendar v-model="calendarValue" class="news-calendar">
              <template #header="{ date }">
                <div class="news-calendar-inner-header">
                  <el-select v-model="selectedYear" size="default" class="news-year-select" @change="syncCalendarFromSelects">
                    <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
                  </el-select>
                  <el-select v-model="selectedMonth" size="default" class="news-month-select" @change="syncCalendarFromSelects">
                    <el-option v-for="m in 12" :key="m" :label="`${m}月`" :value="m" />
                  </el-select>
                </div>
              </template>
              <template #date-cell="{ data }">
                <div
                  class="news-calendar-cell"
                  :class="{
                    'is-prev-month': data.type === 'prev-month',
                    'is-next-month': data.type === 'next-month',
                    'is-future': isFutureDate(data.day),
                    'is-selected': data.day === selectedDate,
                    'is-current-month': data.type === 'current-month'
                  }"
                  @click="onDateCellClick(data)"
                >
                  {{ parseInt(data.day.split('-')[2], 10) }}
                </div>
              </template>
            </el-calendar>
          </section>
        </aside>
      </div>

      <div v-if="total > 0" class="news-pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>

      <p v-if="loading" class="news-loading">加载中…</p>
      <el-alert v-if="error" type="error" :title="error" show-icon class="news-alert" />
      <p v-if="!loading && !error && items.length === 0" class="news-empty">{{ selectedDate ? '该日期暂无资讯' : '最近 10 天暂无资讯' }}</p>
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
const size = ref(20)
const loading = ref(false)
const error = ref('')
const now = new Date()
const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
// null = 默认最近10天，有值 = 指定日期
const selectedDate = ref(null)
const calendarValue = ref(new Date(now.getFullYear(), now.getMonth(), 1))
const selectedYear = ref(now.getFullYear())
const selectedMonth = ref(now.getMonth() + 1)
const yearOptions = Array.from({ length: 7 }, (_, i) => now.getFullYear() - 2 + i)

function syncCalendarFromSelects() {
  calendarValue.value = new Date(selectedYear.value, selectedMonth.value - 1, 1)
}

function isFutureDate(dayStr) {
  if (!dayStr) return false
  return dayStr > todayStr
}

function onDateCellClick(data) {
  if (data.type !== 'current-month' || isFutureDate(data.day)) return
  selectedDate.value = data.day
  page.value = 1
  load()
}

function showRecent10Days() {
  selectedDate.value = null
  page.value = 1
  load()
}

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

function formatDateHeader(dateStr) {
  if (!dateStr) return '其他'
  const d = new Date(dateStr + 'T12:00:00')
  if (isNaN(d.getTime())) return dateStr
  const m = d.getMonth() + 1
  const day = d.getDate()
  const w = weekDays[d.getDay()]
  return `${m}月${day}日 · 周${w}`
}

const groupedByDate = computed(() => {
  const map = {}
  for (const n of items.value) {
    const key = n.publishDate || ''
    if (!map[key]) map[key] = []
    map[key].push(n)
  }
  return map
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (selectedDate.value) params.publishDate = selectedDate.value
    // 不传 publishDate 时后端默认返回最近 10 天，分页
    const data = await api.get('/news', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch(calendarValue, (val) => {
  if (val) {
    selectedYear.value = val.getFullYear()
    selectedMonth.value = val.getMonth() + 1
  }
})

onMounted(load)
</script>

<style scoped>
.news-list {
  min-height: 100vh;
  background: linear-gradient(180deg, #fafbfc 0%, #f1f3f6 100%);
}

.news-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem 1.5rem 3rem;
}

.news-layout {
  display: flex;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 1023px) {
  .news-layout {
    flex-direction: column;
  }
  .news-sidebar {
    order: -1;
  }
}

.news-main {
  flex: 1;
  min-width: 0;
}

.news-sidebar {
  flex-shrink: 0;
  width: 380px;
}

@media (max-width: 1023px) {
  .news-sidebar {
    width: 100%;
  }
}

.news-calendar-section {
  background: #fff;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 1rem;
}

.news-calendar-title {
  font-size: 1.0625rem;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 0.5rem;
}

.news-recent-link {
  font-size: 0.875rem;
  margin: 0 0 1rem;
}

.news-recent-link a {
  color: var(--color-primary, #2563eb);
  text-decoration: none;
}

.news-recent-link a:hover {
  text-decoration: underline;
}

.news-calendar-inner-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.news-year-select {
  width: 6rem;
}

.news-month-select {
  width: 5rem;
}

.news-calendar {
  --el-calendar-border: 1px solid #e2e8f0;
}

.news-calendar :deep(.el-calendar__header) {
  padding: 0.75rem 0 1rem;
}

.news-calendar :deep(.el-calendar__body) {
  padding: 0;
}

.news-calendar :deep(.el-calendar-table) {
  font-size: 1rem;
}

.news-calendar :deep(.el-calendar-table th) {
  font-size: 0.875rem;
  font-weight: 600;
  color: #64748b;
  padding: 0.5rem 0;
}

.news-calendar :deep(.el-calendar-table .el-calendar-day) {
  padding: 0.5rem;
  height: auto;
  border: none;
}

.news-calendar :deep(.el-calendar-table td.is-selected) {
  background: transparent !important;
}

.news-calendar :deep(.el-calendar-table td:hover) {
  background: transparent;
}

.news-calendar :deep(.el-calendar-table td) {
  background: transparent;
}

.news-calendar-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin: 0 auto;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  border-radius: 50%;
  transition: background 0.2s, color 0.2s;
}

.news-calendar-cell.is-prev-month,
.news-calendar-cell.is-next-month,
.news-calendar-cell.is-future {
  color: #cbd5e1;
  cursor: not-allowed;
  opacity: 0.6;
}

.news-calendar-cell.is-prev-month:hover,
.news-calendar-cell.is-next-month:hover,
.news-calendar-cell.is-future:hover {
  background: transparent;
}

.news-calendar-cell.is-current-month {
  color: #0f172a;
}

.news-calendar-cell.is-current-month:hover {
  background: #f1f5f9;
}

.news-calendar-cell.is-selected {
  background: var(--color-primary, #2563eb);
  color: #fff !important;
  font-weight: 600;
}

.news-calendar-cell.is-selected:hover {
  background: var(--color-primary-dark, #1e40af);
}

.news-date-header {
  font-size: 1rem;
  font-weight: 600;
  color: #64748b;
  margin: 1.5rem 0 0.75rem;
  padding-bottom: 0.25rem;
}

.news-date-header:first-child {
  margin-top: 0;
}

.news-date-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.news-card {
  display: block;
  padding: 1rem 1.25rem;
  background: #fff;
  border-radius: 10px;
  border-left: 3px solid transparent;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.25s ease, border-color 0.25s, transform 0.25s ease;
  animation: fade-up 0.4s ease-out both;
}

.news-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-left-color: var(--color-primary, #2563eb);
  transform: translateX(2px);
}

.news-card-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.45;
  margin-bottom: 0.25rem;
  transition: color 0.2s;
}

.news-card:hover .news-card-title {
  color: var(--color-primary, #2563eb);
}

.news-card-summary {
  font-size: 0.8125rem;
  color: #64748b;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.news-pagination {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

.news-loading,
.news-empty {
  text-align: center;
  color: #64748b;
  padding: 3rem 0;
}

.news-alert {
  margin-top: 1rem;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .news-card {
    animation: none;
  }
  .news-card:hover {
    transform: none;
  }
}
</style>
