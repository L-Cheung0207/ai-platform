<script setup>
import { onMounted, ref, shallowRef } from 'vue'
import api from '../../services/api'
import SkillOpsDashboard from '../../components/skills/SkillOpsDashboard.vue'
import SkillFeedbackInbox from '../../components/skills/SkillFeedbackInbox.vue'
import SkillMonthlyReportDialog from '../../components/skills/SkillMonthlyReportDialog.vue'
import SkillMonthlyReportHistory from '../../components/skills/SkillMonthlyReportHistory.vue'
import SkillQuarterlyReportPanel from '../../components/skills/SkillQuarterlyReportPanel.vue'

const report = ref(null)
const monthlyReport = ref(null)
const quarterlyReport = ref(null)
const reportHistory = ref([])
const loading = shallowRef(false)
const historyLoading = shallowRef(false)
const reportLoading = shallowRef(false)
const quarterlyLoading = shallowRef(false)
const reportDialogVisible = shallowRef(false)
const error = shallowRef('')

onMounted(refreshOperations)

function refreshOperations() {
  loadReport()
  loadQuarterlyReport()
  loadReportHistory()
}

async function loadReport() {
  loading.value = true
  error.value = ''
  try {
    report.value = await api.get('/admin/skill-operations')
  } catch (e) {
    error.value = e.message || '运营数据加载失败'
  } finally {
    loading.value = false
  }
}

async function loadReportHistory() {
  historyLoading.value = true
  error.value = ''
  try {
    const data = await api.get('/admin/skill-operations/monthly-reports', {
      params: { page: 1, size: 6 },
    })
    reportHistory.value = data.items || []
  } catch (e) {
    error.value = e.message || '月报归档加载失败'
  } finally {
    historyLoading.value = false
  }
}

async function loadQuarterlyReport() {
  quarterlyLoading.value = true
  error.value = ''
  try {
    quarterlyReport.value = await api.get('/admin/skill-operations/quarterly-report')
  } catch (e) {
    error.value = e.message || '季度治理数据加载失败'
  } finally {
    quarterlyLoading.value = false
  }
}

async function generateMonthlyReport() {
  reportLoading.value = true
  error.value = ''
  try {
    monthlyReport.value = await api.get('/admin/skill-operations/monthly-report')
    reportDialogVisible.value = true
    await loadReportHistory()
  } catch (e) {
    error.value = e.message || '月报生成失败'
  } finally {
    reportLoading.value = false
  }
}

function openMonthlyReport(reportItem) {
  monthlyReport.value = reportItem
  reportDialogVisible.value = true
}

function downloadMonthlyReport(reportItem = monthlyReport.value) {
  if (!reportItem?.markdown) return
  const blob = new Blob([reportItem.markdown], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `ai-skill-monthly-report-${reportItem.month || 'current'}.md`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function downloadQuarterlyReport() {
  if (!quarterlyReport.value?.markdown) return
  const blob = new Blob([quarterlyReport.value.markdown], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `ai-skill-quarterly-report-${quarterlyReport.value.quarter || 'current'}.md`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="skill-operations">
    <div class="skill-operations__header">
      <div>
        <h1>Skill 运营</h1>
        <p>月度使用、收益、评审和反馈闭环</p>
      </div>
      <div class="skill-operations__actions">
        <el-button :loading="reportLoading" @click="generateMonthlyReport">生成月报</el-button>
        <el-button :loading="loading || quarterlyLoading || historyLoading" @click="refreshOperations">刷新</el-button>
      </div>
    </div>

    <SkillOpsDashboard :report="report" :loading="loading" />
    <SkillQuarterlyReportPanel
      :report="quarterlyReport"
      :loading="quarterlyLoading"
      @refresh="loadQuarterlyReport"
      @download="downloadQuarterlyReport"
    />
    <SkillMonthlyReportHistory
      :reports="reportHistory"
      :loading="historyLoading"
      @view="openMonthlyReport"
      @download="downloadMonthlyReport"
    />
    <SkillFeedbackInbox @changed="loadReport" />
    <SkillMonthlyReportDialog
      v-model="reportDialogVisible"
      :report="monthlyReport"
      :loading="reportLoading"
      @download="downloadMonthlyReport"
    />

    <el-alert v-if="error" type="error" :title="error" show-icon />
  </div>
</template>

<style scoped>
.skill-operations {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.skill-operations__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.skill-operations__header h1 {
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 6px;
}

.skill-operations__header p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.skill-operations__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 680px) {
  .skill-operations__header {
    flex-direction: column;
  }

  .skill-operations__actions {
    width: 100%;
  }

  .skill-operations__actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
