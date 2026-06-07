<script setup>
defineProps({
  milestones: {
    type: Array,
    default: () => [],
  },
})

const statusMeta = {
  DONE: { label: '已完成', type: 'success' },
  IN_PROGRESS: { label: '推进中', type: 'warning' },
  PENDING: { label: '未开始', type: 'info' },
}

function statusLabel(status) {
  return statusMeta[status]?.label || status || '—'
}

function statusType(status) {
  return statusMeta[status]?.type || 'info'
}
</script>

<template>
  <article class="pilot-panel">
    <div class="pilot-panel__header">
      <h2>试点里程碑</h2>
    </div>

    <div v-if="milestones.length" class="pilot-list">
      <section v-for="item in milestones" :key="item.phase" class="pilot-row">
        <div class="pilot-row__top">
          <div class="pilot-row__title">
            <strong>{{ item.phase }}</strong>
            <span>{{ item.period }} · {{ item.deliverable }}</span>
          </div>
          <el-tag :type="statusType(item.status)" size="small" effect="light">
            {{ statusLabel(item.status) }}
          </el-tag>
        </div>
        <p class="pilot-row__action">{{ item.keyAction }}</p>
        <div class="pilot-row__progress">
          <el-progress :percentage="item.progressPercent || 0" :stroke-width="8" />
        </div>
        <div class="pilot-row__evidence">
          <span v-for="evidence in item.evidence || []" :key="evidence">{{ evidence }}</span>
        </div>
        <p v-if="item.nextActions?.length" class="pilot-row__next">
          下一步：{{ item.nextActions.join('；') }}
        </p>
      </section>
    </div>
    <el-empty v-else description="暂无试点进度数据" :image-size="80" />
  </article>
</template>

<style scoped>
.pilot-panel {
  grid-column: 1 / -1;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.pilot-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.pilot-panel__header h2 {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.pilot-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.pilot-row {
  min-width: 0;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfd;
  padding: 12px;
}

.pilot-row__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.pilot-row__title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.pilot-row__title strong {
  color: #111827;
  font-size: 13px;
  line-height: 1.25;
}

.pilot-row__title span,
.pilot-row__action,
.pilot-row__next {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}

.pilot-row__action {
  min-height: 34px;
  margin: 10px 0;
}

.pilot-row__progress {
  margin-bottom: 10px;
}

.pilot-row__evidence {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pilot-row__evidence span {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 1.2;
  padding: 3px 7px;
}

.pilot-row__next {
  margin: 10px 0 0;
}

@media (max-width: 1280px) {
  .pilot-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .pilot-list {
    grid-template-columns: 1fr;
  }
}
</style>
