<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  report: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'download'])

function close() {
  emit('update:modelValue', false)
}

function download() {
  emit('download')
}
</script>

<template>
  <el-dialog
    :model-value="props.modelValue"
    title="Skill 月度运营报告"
    width="860px"
    class="monthly-report-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="props.loading" class="monthly-report">
      <template v-if="props.report">
        <section class="monthly-report__summary">
          <article>
            <span>月份</span>
            <strong>{{ props.report.month }}</strong>
          </article>
          <article>
            <span>本月使用</span>
            <strong>{{ props.report.monthlyUsageCount || 0 }}</strong>
          </article>
          <article>
            <span>本月节省</span>
            <strong>{{ props.report.monthlySavedHours || 0 }}h</strong>
          </article>
          <article>
            <span>新人节省</span>
            <strong>{{ props.report.monthlyNewcomerOnboardingSavedHours || 0 }}h</strong>
          </article>
          <article>
            <span>Review 问题降低</span>
            <strong>{{ Number(props.report.monthlyReviewIssueReductionRate || 0).toFixed(1) }}%</strong>
          </article>
          <article>
            <span>覆盖率提升</span>
            <strong>{{ Number(props.report.monthlyTestCoverageIncreasePoints || 0).toFixed(1) }}pp</strong>
          </article>
          <article>
            <span>反馈闭环</span>
            <strong>{{ Number(props.report.monthlyFeedbackClosedRate || 0).toFixed(1) }}%</strong>
          </article>
        </section>

        <el-input
          :model-value="props.report.markdown"
          type="textarea"
          :rows="20"
          resize="vertical"
          readonly
          class="monthly-report__content"
        />
      </template>
      <el-empty v-else description="暂无月报" />
    </div>

    <template #footer>
      <el-button @click="close">关闭</el-button>
      <el-button type="primary" :disabled="!props.report" :loading="props.loading" @click="download">
        下载 Markdown
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.monthly-report {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.monthly-report__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.monthly-report__summary article {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  padding: 12px 14px;
  min-width: 0;
}

.monthly-report__summary span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 8px;
}

.monthly-report__summary strong {
  color: #111827;
  display: block;
  font-size: 20px;
  line-height: 1.2;
}

.monthly-report__content :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 720px) {
  .monthly-report__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .monthly-report__summary {
    grid-template-columns: 1fr;
  }
}
</style>
