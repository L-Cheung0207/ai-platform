<script setup>
import { computed } from 'vue'
import SkillArchiveCandidatesPanel from './SkillArchiveCandidatesPanel.vue'
import SkillAwardCandidatesPanel from './SkillAwardCandidatesPanel.vue'
import SkillPilotMilestonesPanel from './SkillPilotMilestonesPanel.vue'

const props = defineProps({
  report: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const metrics = computed(() => props.report?.metrics || {})
const monthlyTrends = computed(() => props.report?.monthlyTrends || [])
const topSkills = computed(() => props.report?.topSkills || [])
const reviewQueue = computed(() => props.report?.reviewQueue || [])
const monthlyAwardCandidates = computed(() => props.report?.monthlyAwardCandidates || [])
const pilotMilestones = computed(() => props.report?.pilotMilestones || [])
const archiveCandidates = computed(() => props.report?.archiveCandidates || [])
const maxUsage = computed(() => Math.max(1, ...monthlyTrends.value.map((item) => item.usageCount || 0)))

const assetLevelLabels = {
  TEAM: '团队',
  COMPANY: '公司',
}

const lifecycleLabels = {
  CANDIDATE: '候选',
  TRIAL: '试用中',
  REVIEWING: '评审中',
  APPROVED: '已入库',
  NEEDS_REVIEW: '待复审',
  ARCHIVED: '已归档',
}

function barStyle(item) {
  return { width: `${Math.max(6, ((item.usageCount || 0) / maxUsage.value) * 100)}%` }
}

function percent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function assetLevelLabel(value) {
  return assetLevelLabels[value] || '—'
}

function lifecycleLabel(value) {
  return lifecycleLabels[value] || '—'
}

function reviewDueLabel(row) {
  const days = Number(row.daysUntilReview || 0)
  if (days < 0) return `已过期 ${Math.abs(days)} 天`
  if (days === 0) return '今天到期'
  return `${days} 天后`
}

function dueTypeLabel(row) {
  if (row.dueType === 'TRIAL') return '试用到期'
  return '复审到期'
}
</script>

<template>
  <div v-loading="loading" class="ops-dashboard">
    <section class="ops-kpis">
      <article class="ops-kpi">
        <span>资产总数</span>
        <strong>{{ metrics.totalSkills || 0 }}</strong>
      </article>
      <article class="ops-kpi">
        <span>近 30 天使用</span>
        <strong>{{ metrics.monthlyUsageCount || 0 }}</strong>
      </article>
      <article class="ops-kpi">
        <span>工具链信号</span>
        <strong>{{ metrics.toolchainUsageCount || 0 }}</strong>
      </article>
      <article class="ops-kpi">
        <span>累计节省工时</span>
        <strong>{{ metrics.estimatedSavedHours || 0 }}</strong>
      </article>
      <article class="ops-kpi">
        <span>新人上手节省</span>
        <strong>{{ metrics.newcomerOnboardingSavedHours || 0 }}h</strong>
      </article>
      <article class="ops-kpi">
        <span>Review 问题降低</span>
        <strong>{{ percent(metrics.reviewIssueReductionRate) }}</strong>
      </article>
      <article class="ops-kpi">
        <span>测试覆盖提升</span>
        <strong>{{ Number(metrics.testCoverageIncreasePoints || 0).toFixed(1) }}pp</strong>
      </article>
      <article class="ops-kpi">
        <span>质量信号</span>
        <strong>{{ metrics.qualitySignalCount || 0 }}</strong>
      </article>
      <article class="ops-kpi">
        <span>反馈闭环率</span>
        <strong>{{ percent(metrics.feedbackClosedRate) }}</strong>
      </article>
      <article class="ops-kpi">
        <span>反馈待处理</span>
        <strong>{{ metrics.openFeedbackCount || 0 }}</strong>
      </article>
      <article class="ops-kpi">
        <span>待复审</span>
        <strong>{{ metrics.needsReviewCount || 0 }}</strong>
      </article>
      <article class="ops-kpi">
        <span>过期 Skill 比例</span>
        <strong>{{ percent(metrics.overdueSkillRate) }}</strong>
      </article>
      <article class="ops-kpi">
        <span>评审通过率</span>
        <strong>{{ percent(metrics.reviewPassRate) }}</strong>
      </article>
    </section>

    <section class="ops-grid">
      <article class="ops-panel">
        <div class="ops-panel__header">
          <h2>月度趋势</h2>
        </div>
        <div class="trend-list">
          <div v-for="item in monthlyTrends" :key="item.month" class="trend-row">
            <span class="trend-row__month">{{ item.month }}</span>
            <div class="trend-row__track">
              <span class="trend-row__bar" :style="barStyle(item)" />
            </div>
            <span class="trend-row__meta">{{ item.usageCount }} 次 / {{ item.savedHours }}h / {{ item.feedbackCount }} 反馈</span>
          </div>
        </div>
      </article>

      <article class="ops-panel">
        <div class="ops-panel__header">
          <h2>高影响 Skill</h2>
        </div>
        <el-table :data="topSkills" size="small" empty-text="暂无使用数据">
          <el-table-column prop="skillName" label="Skill" min-width="170" />
          <el-table-column label="层级" width="80">
            <template #default="{ row }">{{ assetLevelLabel(row.assetLevel) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">{{ lifecycleLabel(row.lifecycleStatus) }}</template>
          </el-table-column>
          <el-table-column prop="usageCount" label="使用" width="70" />
          <el-table-column prop="savedHours" label="节省h" width="78" />
          <el-table-column prop="openFeedbackCount" label="待处理" width="78" />
        </el-table>
      </article>

      <SkillPilotMilestonesPanel :milestones="pilotMilestones" />

      <SkillAwardCandidatesPanel :candidates="monthlyAwardCandidates" />

      <SkillArchiveCandidatesPanel :candidates="archiveCandidates" />

      <article class="ops-panel ops-panel--wide">
        <div class="ops-panel__header">
          <h2>复审提醒</h2>
        </div>
        <el-table :data="reviewQueue" size="small" empty-text="暂无 14 天内到期的 Skill">
          <el-table-column prop="skillName" label="Skill" min-width="180" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">{{ lifecycleLabel(row.lifecycleStatus) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="96">
            <template #default="{ row }">{{ dueTypeLabel(row) }}</template>
          </el-table-column>
          <el-table-column prop="dueAt" label="到期日期" width="112" />
          <el-table-column label="提醒" width="112">
            <template #default="{ row }">
              <span :class="['review-due', { 'review-due--overdue': row.overdue }]">
                {{ reviewDueLabel(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="maintainer" label="维护人" width="110" />
          <el-table-column prop="teamName" label="团队" width="110" />
        </el-table>
      </article>
    </section>
  </div>
</template>

<style scoped>
.ops-dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ops-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ops-kpi {
  min-height: 86px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.ops-kpi span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 10px;
}

.ops-kpi strong {
  color: #111827;
  display: block;
  font-size: 24px;
  line-height: 1.1;
}

.ops-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
}

.ops-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
  min-width: 0;
}

.ops-panel--wide {
  grid-column: 1 / -1;
}

.ops-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.ops-panel__header h2 {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trend-row {
  display: grid;
  grid-template-columns: 72px minmax(90px, 1fr) 148px;
  align-items: center;
  gap: 10px;
}

.trend-row__month {
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.trend-row__track {
  height: 10px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.trend-row__bar {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #16a34a;
}

.trend-row__meta {
  color: #6b7280;
  font-size: 12px;
  text-align: right;
}

.review-due {
  color: #2563eb;
  font-weight: 700;
}

.review-due--overdue {
  color: #dc2626;
}

@media (max-width: 1280px) {
  .ops-kpis {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .ops-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .ops-kpis {
    grid-template-columns: 1fr;
  }

  .trend-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .trend-row__meta {
    text-align: left;
  }
}
</style>
