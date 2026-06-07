<script setup>
import { computed } from 'vue'
import { Download, Refresh } from '@element-plus/icons-vue'

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

const emit = defineEmits(['download', 'refresh'])

const findings = computed(() => props.report?.governanceFindings || [])
const risks = computed(() => props.report?.risks || [])
const recommendations = computed(() => props.report?.recommendations || [])
const topSkills = computed(() => props.report?.topSkills || [])

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

function percent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function assetLevelLabel(value) {
  return assetLevelLabels[value] || '—'
}

function lifecycleLabel(value) {
  return lifecycleLabels[value] || '—'
}
</script>

<template>
  <article v-loading="loading" class="quarterly-panel">
    <div class="quarterly-panel__header">
      <div>
        <h2>季度治理</h2>
        <p v-if="report">{{ report.quarter }} · {{ report.startMonth }} 至 {{ report.endMonth }}</p>
      </div>
      <div class="quarterly-panel__actions">
        <el-button :icon="Refresh" :loading="loading" @click="emit('refresh')">刷新</el-button>
        <el-button :icon="Download" type="primary" :disabled="!report?.markdown" @click="emit('download')">
          下载季报
        </el-button>
      </div>
    </div>

    <template v-if="report">
      <section class="quarterly-kpis">
        <div class="quarterly-kpi">
          <span>季度使用</span>
          <strong>{{ report.quarterlyUsageCount || 0 }}</strong>
        </div>
        <div class="quarterly-kpi">
          <span>工具链信号</span>
          <strong>{{ report.metricsSnapshot?.toolchainUsageCount || 0 }}</strong>
        </div>
        <div class="quarterly-kpi">
          <span>季度节省</span>
          <strong>{{ report.quarterlySavedHours || 0 }}h</strong>
        </div>
        <div class="quarterly-kpi">
          <span>反馈闭环</span>
          <strong>{{ percent(report.quarterlyFeedbackClosedRate) }}</strong>
        </div>
        <div class="quarterly-kpi">
          <span>评审通过</span>
          <strong>{{ percent(report.quarterlyReviewPassRate) }}</strong>
        </div>
        <div class="quarterly-kpi">
          <span>Review 问题降低</span>
          <strong>{{ percent(report.quarterlyReviewIssueReductionRate) }}</strong>
        </div>
        <div class="quarterly-kpi">
          <span>覆盖率提升</span>
          <strong>{{ Number(report.quarterlyTestCoverageIncreasePoints || 0).toFixed(1) }}pp</strong>
        </div>
      </section>

      <section class="quarterly-sections">
        <div class="quarterly-section">
          <h3>治理发现</h3>
          <ul>
            <li v-for="item in findings" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="quarterly-section quarterly-section--risk">
          <h3>季度风险</h3>
          <ul>
            <li v-for="item in risks" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="quarterly-section quarterly-section--next">
          <h3>下季度建议</h3>
          <ul>
            <li v-for="item in recommendations" :key="item">{{ item }}</li>
          </ul>
        </div>
      </section>

      <el-table :data="topSkills" size="small" empty-text="暂无季度高影响 Skill">
        <el-table-column prop="skillName" label="季度高影响 Skill" min-width="190" />
        <el-table-column label="层级" width="80">
          <template #default="{ row }">{{ assetLevelLabel(row.assetLevel) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ lifecycleLabel(row.lifecycleStatus) }}</template>
        </el-table-column>
        <el-table-column prop="usageCount" label="使用" width="70" />
        <el-table-column prop="feedbackCount" label="反馈" width="70" />
        <el-table-column prop="savedHours" label="节省h" width="78" />
      </el-table>
    </template>
    <el-empty v-else description="暂无季度治理数据" :image-size="80" />
  </article>
</template>

<style scoped>
.quarterly-panel {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.quarterly-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.quarterly-panel__header h2,
.quarterly-section h3 {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.quarterly-panel__header p {
  color: #6b7280;
  font-size: 12px;
  margin: 5px 0 0;
}

.quarterly-panel__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.quarterly-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.quarterly-kpi {
  min-width: 0;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfd;
  padding: 12px;
}

.quarterly-kpi span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.25;
  margin-bottom: 8px;
}

.quarterly-kpi strong {
  display: block;
  color: #111827;
  font-size: 20px;
  line-height: 1.15;
}

.quarterly-sections {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.quarterly-section {
  min-width: 0;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfd;
  padding: 12px;
}

.quarterly-section--risk {
  background: #fff7ed;
  border-color: #fed7aa;
}

.quarterly-section--next {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.quarterly-section ul {
  display: flex;
  flex-direction: column;
  gap: 7px;
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
}

.quarterly-section li {
  color: #374151;
  font-size: 12px;
  line-height: 1.45;
}

@media (max-width: 1280px) {
  .quarterly-kpis {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .quarterly-sections {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .quarterly-panel__header {
    flex-direction: column;
  }

  .quarterly-panel__actions {
    width: 100%;
  }

  .quarterly-panel__actions :deep(.el-button) {
    flex: 1;
  }

  .quarterly-kpis {
    grid-template-columns: 1fr;
  }
}
</style>
