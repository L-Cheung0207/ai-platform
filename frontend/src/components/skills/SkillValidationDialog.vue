<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  skill: { type: Object, default: null },
  report: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const statusLabel = computed(() => {
  if (!props.report) return '未校验'
  return props.report.passed ? '通过' : '未通过'
})

function itemTagType(item) {
  if (item.passed) return 'success'
  return item.required ? 'danger' : 'warning'
}
</script>

<template>
  <el-dialog v-model="visible" title="模板校验报告" width="760px" class="validation-dialog">
    <div v-if="skill" class="validation-heading">
      <div>
        <strong>{{ skill.name }}</strong>
        <span>{{ skill.skillDirectory || skill.sourceRepositoryUrl || '未登记来源' }}</span>
      </div>
      <el-tag
        :type="report?.passed ? 'success' : 'danger'"
        effect="light"
        class="validation-status-badge"
      >
        {{ statusLabel }}
      </el-tag>
    </div>

    <div v-if="report" class="validation-summary">
      <span>必填项 {{ report.passedCount }} / {{ report.totalCount }}</span>
      <span>{{ report.notes }}</span>
    </div>

    <el-table v-if="report" :data="report.items || []" size="small" max-height="420">
      <el-table-column label="项目" min-width="140">
        <template #default="{ row }">
          <div class="validation-item-title">
            <span>{{ row.label }}</span>
            <el-tag v-if="row.required" size="small" effect="plain">必填</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="itemTagType(row)" size="small" effect="light" class="validation-item-badge">
            {{ row.passed ? '通过' : '待补充' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
    </el-table>

    <template #footer>
      <el-button type="primary" @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.validation-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.validation-heading strong {
  display: block;
  color: #111827;
  font-size: 16px;
}

.validation-heading span {
  display: block;
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
  word-break: break-all;
}

.validation-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  padding: 12px 14px;
  margin-bottom: 14px;
  color: #374151;
  font-size: 14px;
}

.validation-item-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.validation-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}

.validation-status-badge {
  flex-shrink: 0;
  height: auto;
  padding: 6px 14px;
  border-radius: 6px;
  border-width: 1px;
  border-style: solid;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.25;
}

.validation-dialog :deep(.validation-status-badge.el-tag--success.is-light) {
  --el-tag-bg-color: #dff7ec;
  --el-tag-border-color: #6ee7b7;
  --el-tag-text-color: #065f46;
  background-color: #dff7ec;
  border-color: #6ee7b7;
  color: #065f46;
}

.validation-dialog :deep(.validation-status-badge.el-tag--danger.is-light) {
  --el-tag-bg-color: #fee2e2;
  --el-tag-border-color: #fca5a5;
  --el-tag-text-color: #991b1b;
  background-color: #fee2e2;
  border-color: #fca5a5;
  color: #991b1b;
}

.validation-dialog :deep(.validation-item-badge.el-tag--success.is-light) {
  --el-tag-bg-color: #ecfccb;
  --el-tag-border-color: #bef264;
  --el-tag-text-color: #365314;
  background-color: #ecfccb;
  border-color: #bef264;
  color: #365314;
  font-weight: 600;
}

.validation-dialog :deep(.validation-item-badge.el-tag--danger.is-light) {
  --el-tag-bg-color: #fee2e2;
  --el-tag-border-color: #fca5a5;
  --el-tag-text-color: #991b1b;
  background-color: #fee2e2;
  border-color: #fca5a5;
  color: #991b1b;
  font-weight: 600;
}

.validation-dialog :deep(.validation-item-badge.el-tag--warning.is-light) {
  --el-tag-bg-color: #fef3c7;
  --el-tag-border-color: #fcd34d;
  --el-tag-text-color: #92400e;
  background-color: #fef3c7;
  border-color: #fcd34d;
  color: #92400e;
  font-weight: 600;
}

@media (max-width: 640px) {
  .validation-heading,
  .validation-summary {
    flex-direction: column;
  }
}
</style>
