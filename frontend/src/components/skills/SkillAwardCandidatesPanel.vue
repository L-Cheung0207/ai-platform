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

function assetLevelLabel(value) {
  return assetLevelLabels[value] || '—'
}

function lifecycleLabel(value) {
  return lifecycleLabels[value] || '—'
}
</script>

<template>
  <article class="award-panel">
    <div class="award-panel__header">
      <h2>本月高价值候选</h2>
    </div>
    <el-table :data="candidates" size="small" empty-text="暂无可推荐的月度贡献候选">
      <el-table-column label="Skill" min-width="190">
        <template #default="{ row }">
          <div class="award-skill">
            <strong>{{ row.skillName }}</strong>
            <span>{{ assetLevelLabel(row.assetLevel) }} / {{ lifecycleLabel(row.lifecycleStatus) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="分数" width="72" />
      <el-table-column prop="usageCount" label="使用" width="70" />
      <el-table-column prop="savedHours" label="节省h" width="78" />
      <el-table-column prop="successCaseCount" label="案例" width="70" />
      <el-table-column label="推荐理由" min-width="260">
        <template #default="{ row }">
          <div class="award-reasons">
            <span v-for="reason in row.reasons || []" :key="reason" class="award-reason">
              {{ reason }}
            </span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </article>
</template>

<style scoped>
.award-panel {
  grid-column: 1 / -1;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.award-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.award-panel__header h2 {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.award-skill {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.award-skill strong {
  color: #111827;
  font-size: 13px;
  line-height: 1.25;
}

.award-skill span {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.2;
}

.award-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.award-reason {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border: 1px solid #d1fae5;
  border-radius: 6px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  line-height: 1.2;
  padding: 3px 8px;
}
</style>
