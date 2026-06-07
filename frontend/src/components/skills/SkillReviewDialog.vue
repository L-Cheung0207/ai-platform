<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  skill: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue', 'saved'])
const authStore = useAuthStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const currentReviewerName = computed(() => authStore.user?.username || '')
const currentReviewerRole = computed(() => authStore.user?.skillGovernanceRole || 'CONTRIBUTOR')

const reviews = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const form = reactive(createForm())

const stageOptions = [
  { value: 'TEAM_REVIEW', label: '团队评审' },
  { value: 'COMPANY_REVIEW', label: '公司评审' },
  { value: 'PERIODIC_REVIEW', label: '周期复审' },
]

const reviewerRoleOptions = [
  { value: 'CONTRIBUTOR', label: '贡献者' },
  { value: 'TECH_LEAD', label: 'Tech Lead' },
  { value: 'PLATFORM_ENGINEERING', label: '平台/效能团队' },
  { value: 'TECHNICAL_COMMITTEE', label: '技术委员会' },
  { value: 'SECURITY_QUALITY', label: '安全/质量团队' },
]

const resultOptions = [
  { value: 'PASSED', label: '通过' },
  { value: 'NEEDS_CHANGES', label: '需调整' },
  { value: 'REJECTED', label: '未通过' },
]

const criteria = [
  { key: 'truthful', label: '真实性' },
  { key: 'accurate', label: '准确性' },
  { key: 'reusable', label: '可复用性' },
  { key: 'executable', label: '可执行性' },
  { key: 'secure', label: '安全性' },
  { key: 'verifiable', label: '可验证性' },
  { key: 'maintainable', label: '可维护性' },
]

const approvalGateMessage = computed(() => {
  if (!props.skill) return ''
  if (props.skill.templateValidationStatus !== 'PASSED') {
    return '评审通过前需要先完成模板校验'
  }
  if (currentReviewerRole.value === 'CONTRIBUTOR') {
    return '贡献者可提交 Skill，但不能直接评审入库'
  }
  if (props.skill.riskLevel === 'HIGH') {
    return '高风险 Skill 需由技术委员会或安全/质量团队评审通过'
  }
  if (props.skill.assetLevel === 'COMPANY') {
    return '公司级资产需由平台/效能团队、技术委员会或安全/质量团队评审通过'
  }
  return '评审通过后将自动进入已入库状态'
})

watch(() => props.modelValue, async (value) => {
  if (value && props.skill?.id) {
    await refreshReviewerIdentity()
    resetForm()
    await loadReviews()
  }
})

function createForm() {
  return {
    reviewerName: '',
    reviewerRole: 'CONTRIBUTOR',
    reviewStage: 'TEAM_REVIEW',
    result: 'PASSED',
    truthful: true,
    accurate: true,
    reusable: true,
    executable: true,
    secure: true,
    verifiable: true,
    maintainable: true,
    reviewedAt: new Date().toISOString().slice(0, 10),
    nextReviewAt: '',
    notes: '',
  }
}

function resetForm() {
  Object.assign(form, createForm())
  form.reviewerName = currentReviewerName.value
  form.reviewerRole = currentReviewerRole.value
}

async function refreshReviewerIdentity() {
  try {
    await authStore.fetchMe()
  } catch (e) {
    // 失效登录会由统一 API 处理跳转，这里只避免打断弹窗状态。
  }
}

async function loadReviews() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.get(`/admin/skills/${props.skill.id}/reviews`, {
      params: { page: 1, size: 20 },
    })
    reviews.value = data.items || []
  } catch (e) {
    error.value = e.message || '加载评审记录失败'
  } finally {
    loading.value = false
  }
}

async function submitReview() {
  if (!currentReviewerName.value) {
    ElMessage.warning('登录信息已过期，请重新登录')
    return
  }
  saving.value = true
  error.value = ''
  try {
    await api.post(`/admin/skills/${props.skill.id}/reviews`, {
      reviewerName: currentReviewerName.value,
      reviewerRole: currentReviewerRole.value,
      reviewStage: form.reviewStage,
      result: form.result,
      truthful: form.truthful,
      accurate: form.accurate,
      reusable: form.reusable,
      executable: form.executable,
      secure: form.secure,
      verifiable: form.verifiable,
      maintainable: form.maintainable,
      reviewedAt: form.reviewedAt || null,
      nextReviewAt: form.nextReviewAt || null,
      notes: form.notes || '',
    })
    ElMessage.success('评审已保存')
    emit('saved')
    resetForm()
    await loadReviews()
  } catch (e) {
    error.value = e.message || '保存评审失败'
  } finally {
    saving.value = false
  }
}

function optionLabel(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}
</script>

<template>
  <el-dialog v-model="visible" title="Skill 评审" width="820px" class="review-dialog">
    <div v-if="skill" class="review-heading">
      <strong>{{ skill.name }}</strong>
      <span>{{ skill.assetLevel }} / {{ skill.lifecycleStatus }}</span>
    </div>
    <el-alert
      v-if="approvalGateMessage"
      :title="approvalGateMessage"
      type="info"
      :closable="false"
      show-icon
      class="review-gate-alert"
    />

    <el-form label-position="top" class="review-form">
      <div class="review-grid">
        <el-form-item label="评审人">
          <el-input v-model="form.reviewerName" maxlength="100" disabled />
        </el-form-item>
        <el-form-item label="评审角色">
          <el-select v-model="form.reviewerRole" class="w-full" disabled>
            <el-option
              v-for="item in reviewerRoleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="form.reviewStage" class="w-full">
            <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="form.result" class="w-full">
            <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </div>

      <div class="criteria-grid">
        <el-checkbox v-for="item in criteria" :key="item.key" v-model="form[item.key]">
          {{ item.label }}
        </el-checkbox>
      </div>

      <div class="review-grid review-grid--dates">
        <el-form-item label="评审日期">
          <el-date-picker v-model="form.reviewedAt" value-format="YYYY-MM-DD" type="date" class="w-full" />
        </el-form-item>
        <el-form-item label="下次复审">
          <el-date-picker v-model="form.nextReviewAt" value-format="YYYY-MM-DD" type="date" class="w-full" clearable />
        </el-form-item>
      </div>

      <el-form-item label="评审备注">
        <el-input v-model="form.notes" type="textarea" :rows="3" maxlength="20000" show-word-limit />
      </el-form-item>
    </el-form>

    <div class="review-history">
      <div class="review-history__title">历史记录</div>
      <el-table v-loading="loading" :data="reviews" size="small" max-height="260">
        <el-table-column prop="reviewedAt" label="日期" width="110" />
        <el-table-column label="阶段" width="110">
          <template #default="{ row }">{{ optionLabel(stageOptions, row.reviewStage) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">{{ optionLabel(resultOptions, row.result) }}</template>
        </el-table-column>
        <el-table-column prop="reviewerName" label="评审人" width="120" />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">{{ optionLabel(reviewerRoleOptions, row.reviewerRole) }}</template>
        </el-table-column>
        <el-table-column prop="notes" label="备注" min-width="180" show-overflow-tooltip />
      </el-table>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon class="mt-3" />

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="saving" @click="submitReview">保存评审</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.review-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  color: #374151;
}

.review-heading span {
  color: #9ca3af;
  font-size: 13px;
}

.review-gate-alert {
  margin-bottom: 14px;
}

.review-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr;
  gap: 12px;
}

.review-grid--dates {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.criteria-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px 12px;
  margin-bottom: 16px;
}

.review-history {
  margin-top: 10px;
}

.review-history__title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #374151;
}

.review-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}

@media (max-width: 720px) {
  .review-grid,
  .review-grid--dates,
  .criteria-grid {
    grid-template-columns: 1fr;
  }
}
</style>
