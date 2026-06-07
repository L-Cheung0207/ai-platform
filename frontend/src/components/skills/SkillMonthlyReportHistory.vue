<script setup>
const props = defineProps({
  reports: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['view', 'download'])

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function percent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}
</script>

<template>
  <section class="monthly-history">
    <div class="monthly-history__header">
      <h2>月报归档</h2>
    </div>

    <el-table v-loading="props.loading" :data="props.reports" size="small" empty-text="暂无归档月报">
      <el-table-column prop="month" label="月份" width="110" />
      <el-table-column label="生成时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.generatedAt) }}</template>
      </el-table-column>
      <el-table-column prop="monthlyUsageCount" label="使用" width="78" />
      <el-table-column prop="monthlySavedHours" label="节省h" width="86" />
      <el-table-column label="反馈闭环" width="100">
        <template #default="{ row }">{{ percent(row.monthlyFeedbackClosedRate) }}</template>
      </el-table-column>
      <el-table-column label="评审通过" width="100">
        <template #default="{ row }">{{ percent(row.monthlyReviewPassRate) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="emit('view', row)">查看</el-button>
          <el-button link type="primary" @click="emit('download', row)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.monthly-history {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
  min-width: 0;
}

.monthly-history__header {
  margin-bottom: 14px;
}

.monthly-history__header h2 {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}
</style>
