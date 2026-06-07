<script setup>
defineProps({
  candidates: {
    type: Array,
    default: () => [],
  },
})

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

const riskLevelLabels = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
}

function assetLevelLabel(value) {
  return assetLevelLabels[value] || '—'
}

function lifecycleLabel(value) {
  return lifecycleLabels[value] || '—'
}

function riskLevelLabel(value) {
  return riskLevelLabels[value] || '—'
}

function scoreType(score) {
  if (Number(score || 0) >= 70) return 'danger'
  if (Number(score || 0) >= 40) return 'warning'
  return 'info'
}

function lastUsageLabel(row) {
  if (!row.lastUsageAt) return '从未使用'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(row.lastUsageAt))
}
</script>

<template>
  <article class="archive-panel">
    <div class="archive-panel__header">
      <h2>下架候选</h2>
    </div>
    <el-table :data="candidates" size="small" empty-text="暂无需要下架评估的 Skill">
      <el-table-column label="Skill" min-width="190">
        <template #default="{ row }">
          <div class="archive-skill">
            <strong>{{ row.skillName }}</strong>
            <span>{{ assetLevelLabel(row.assetLevel) }} / {{ lifecycleLabel(row.lifecycleStatus) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="风险" width="96">
        <template #default="{ row }">
          <el-tag :type="scoreType(row.riskScore)" size="small" effect="light">
            {{ row.riskScore }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="84">
        <template #default="{ row }">{{ riskLevelLabel(row.riskLevel) }}</template>
      </el-table-column>
      <el-table-column label="最近使用" width="124">
        <template #default="{ row }">
          <div class="archive-usage">
            <span>{{ lastUsageLabel(row) }}</span>
            <small v-if="row.daysSinceLastUsage">{{ row.daysSinceLastUsage }} 天</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="usageCount" label="使用" width="70" />
      <el-table-column prop="openFeedbackCount" label="待处理" width="78" />
      <el-table-column prop="failureCaseCount" label="失败" width="70" />
      <el-table-column prop="recommendedAction" label="建议动作" min-width="156" />
      <el-table-column label="触发原因" min-width="280">
        <template #default="{ row }">
          <div class="archive-reasons">
            <span v-for="reason in row.reasons || []" :key="reason" class="archive-reason">
              {{ reason }}
            </span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </article>
</template>

<style scoped>
.archive-panel {
  grid-column: 1 / -1;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.archive-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.archive-panel__header h2 {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.archive-skill,
.archive-usage {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.archive-skill strong {
  color: #111827;
  font-size: 13px;
  line-height: 1.25;
}

.archive-skill span,
.archive-usage span,
.archive-usage small {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.2;
}

.archive-usage span {
  color: #374151;
  font-weight: 600;
}

.archive-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.archive-reason {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border: 1px solid #fee2e2;
  border-radius: 6px;
  background: #fff7ed;
  color: #b45309;
  font-size: 12px;
  line-height: 1.2;
  padding: 3px 8px;
}
</style>
