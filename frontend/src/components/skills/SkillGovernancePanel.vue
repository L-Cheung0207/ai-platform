<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'

const props = defineProps({
  skillId: { type: [Number, String], required: true },
})

const summary = ref(null)
const loading = ref(false)
const usageDialogVisible = ref(false)
const feedbackDialogVisible = ref(false)
const usageSubmitting = ref(false)
const feedbackSubmitting = ref(false)

const usageForm = reactive({
  userName: '',
  scenario: '',
  savedMinutes: 0,
  newcomerOnboardingSavedMinutes: 0,
  reviewIssuesBefore: null,
  reviewIssuesAfter: null,
  testCoverageBefore: null,
  testCoverageAfter: null,
})

const feedbackForm = reactive({
  submitterName: '',
  feedbackType: 'SUCCESS_CASE',
  rating: 5,
  estimatedSavedMinutes: 0,
  content: '',
})

const feedbackTypeOptions = [
  { value: 'SUCCESS_CASE', label: '成功案例' },
  { value: 'FAILURE_CASE', label: '失败案例' },
  { value: 'IMPROVEMENT', label: '改进建议' },
  { value: 'SCOPE_NOTE', label: '适用范围补充' },
]

const latestReviewLabel = computed(() => {
  const result = summary.value?.latestReviewResult
  if (result === 'PASSED') return '通过'
  if (result === 'NEEDS_CHANGES') return '需调整'
  if (result === 'REJECTED') return '未通过'
  return '暂无'
})

watch(() => props.skillId, () => {
  if (props.skillId) loadSummary()
}, { immediate: true })

async function loadSummary() {
  loading.value = true
  try {
    summary.value = await api.get(`/skills/${props.skillId}/governance-summary`)
  } finally {
    loading.value = false
  }
}

async function submitUsage() {
  usageSubmitting.value = true
  try {
    summary.value = await api.post(`/skills/${props.skillId}/usage`, {
      userName: usageForm.userName || '',
      scenario: usageForm.scenario || '',
      savedMinutes: usageForm.savedMinutes || 0,
      newcomerOnboardingSavedMinutes: usageForm.newcomerOnboardingSavedMinutes || 0,
      reviewIssuesBefore: usageForm.reviewIssuesBefore ?? null,
      reviewIssuesAfter: usageForm.reviewIssuesAfter ?? null,
      testCoverageBefore: usageForm.testCoverageBefore ?? null,
      testCoverageAfter: usageForm.testCoverageAfter ?? null,
    })
    usageForm.userName = ''
    usageForm.scenario = ''
    usageForm.savedMinutes = 0
    usageForm.newcomerOnboardingSavedMinutes = 0
    usageForm.reviewIssuesBefore = null
    usageForm.reviewIssuesAfter = null
    usageForm.testCoverageBefore = null
    usageForm.testCoverageAfter = null
    usageDialogVisible.value = false
    ElMessage.success('已记录')
  } catch (e) {
    ElMessage.error(e.message || '记录失败')
  } finally {
    usageSubmitting.value = false
  }
}

async function submitFeedback() {
  if (!feedbackForm.content.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }
  feedbackSubmitting.value = true
  try {
    await api.post(`/skills/${props.skillId}/feedback`, {
      submitterName: feedbackForm.submitterName || '',
      feedbackType: feedbackForm.feedbackType,
      rating: feedbackForm.rating,
      estimatedSavedMinutes: feedbackForm.estimatedSavedMinutes || 0,
      content: feedbackForm.content.trim(),
    })
    feedbackForm.submitterName = ''
    feedbackForm.feedbackType = 'SUCCESS_CASE'
    feedbackForm.rating = 5
    feedbackForm.estimatedSavedMinutes = 0
    feedbackForm.content = ''
    feedbackDialogVisible.value = false
    ElMessage.success('已提交')
    await loadSummary()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    feedbackSubmitting.value = false
  }
}
</script>

<template>
  <section class="governance-panel" v-loading="loading">
    <div class="governance-stats">
      <div class="governance-stat">
        <span>使用次数</span>
        <strong>{{ summary?.usageCount || 0 }}</strong>
      </div>
      <div class="governance-stat">
        <span>节省工时</span>
        <strong>{{ summary?.estimatedSavedHours || 0 }}h</strong>
      </div>
      <div class="governance-stat">
        <span>反馈</span>
        <strong>{{ summary?.feedbackCount || 0 }}</strong>
      </div>
      <div class="governance-stat">
        <span>新人节省</span>
        <strong>{{ summary?.newcomerOnboardingSavedHours || 0 }}h</strong>
      </div>
      <div class="governance-stat">
        <span>Review 问题降低</span>
        <strong>{{ Number(summary?.reviewIssueReductionRate || 0).toFixed(1) }}%</strong>
      </div>
      <div class="governance-stat">
        <span>覆盖率提升</span>
        <strong>{{ Number(summary?.testCoverageIncreasePoints || 0).toFixed(1) }}pp</strong>
      </div>
      <div class="governance-stat">
        <span>最近评审</span>
        <strong>{{ latestReviewLabel }}</strong>
      </div>
    </div>
    <div class="governance-actions">
      <el-button type="primary" @click="usageDialogVisible = true">记录使用</el-button>
      <el-button @click="feedbackDialogVisible = true">提交反馈</el-button>
    </div>
  </section>

  <el-dialog v-model="usageDialogVisible" title="记录使用" width="520px" class="governance-dialog">
    <el-form label-position="top">
      <el-form-item label="使用人">
        <el-input v-model="usageForm.userName" maxlength="100" clearable />
      </el-form-item>
      <el-form-item label="使用场景">
        <el-input v-model="usageForm.scenario" type="textarea" :rows="3" maxlength="20000" />
      </el-form-item>
      <el-form-item label="节省分钟">
        <el-input-number v-model="usageForm.savedMinutes" :min="0" :max="100000" class="w-full" />
      </el-form-item>
      <el-form-item label="新人上手节省分钟">
        <el-input-number v-model="usageForm.newcomerOnboardingSavedMinutes" :min="0" :max="100000" class="w-full" />
      </el-form-item>
      <div class="quality-row">
        <el-form-item label="Review 问题数（使用前）">
          <el-input-number v-model="usageForm.reviewIssuesBefore" :min="0" :max="100000" class="w-full" />
        </el-form-item>
        <el-form-item label="Review 问题数（使用后）">
          <el-input-number v-model="usageForm.reviewIssuesAfter" :min="0" :max="100000" class="w-full" />
        </el-form-item>
      </div>
      <div class="quality-row">
        <el-form-item label="测试覆盖率（使用前 %）">
          <el-input-number v-model="usageForm.testCoverageBefore" :min="0" :max="100" :precision="1" class="w-full" />
        </el-form-item>
        <el-form-item label="测试覆盖率（使用后 %）">
          <el-input-number v-model="usageForm.testCoverageAfter" :min="0" :max="100" :precision="1" class="w-full" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="usageDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="usageSubmitting" @click="submitUsage">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="feedbackDialogVisible" title="提交反馈" width="560px" class="governance-dialog">
    <el-form label-position="top">
      <el-form-item label="提交人">
        <el-input v-model="feedbackForm.submitterName" maxlength="100" clearable />
      </el-form-item>
      <div class="feedback-row">
        <el-form-item label="类型">
          <el-select v-model="feedbackForm.feedbackType" class="w-full">
            <el-option v-for="item in feedbackTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="feedbackForm.rating" />
        </el-form-item>
      </div>
      <el-form-item label="节省分钟">
        <el-input-number v-model="feedbackForm.estimatedSavedMinutes" :min="0" :max="100000" class="w-full" />
      </el-form-item>
      <el-form-item label="反馈内容">
        <el-input v-model="feedbackForm.content" type="textarea" :rows="4" maxlength="20000" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="feedbackDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="feedbackSubmitting" @click="submitFeedback">提交</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.governance-panel {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 32px;
}

.governance-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  flex: 1;
}

.governance-stat {
  min-height: 76px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.governance-stat span {
  display: block;
  color: #9ca3af;
  font-size: 12px;
  margin-bottom: 8px;
}

.governance-stat strong {
  color: #1f2937;
  font-size: 20px;
  line-height: 1.2;
}

.governance-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.feedback-row {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 12px;
}

.quality-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.governance-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}

@media (max-width: 900px) {
  .governance-panel {
    flex-direction: column;
  }

  .governance-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .governance-stats,
  .feedback-row,
  .quality-row {
    grid-template-columns: 1fr;
  }
}
</style>
