<script setup>
import { computed, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, FolderOpened, Plus, Upload } from '@element-plus/icons-vue'
import api from '../../services/api'
import { useAuthStore } from '../../stores/auth'
import SkillAssetForm from './SkillAssetForm.vue'
import SkillPackageImportDialog from './SkillPackageImportDialog.vue'

const visible = defineModel({ type: Boolean, required: true })
const emit = defineEmits(['changed'])

const router = useRouter()
const auth = useAuthStore()
const items = ref([])
const total = shallowRef(0)
const page = shallowRef(1)
const size = shallowRef(10)
const loading = shallowRef(false)
const saving = shallowRef(false)
const deletingId = shallowRef(null)
const error = shallowRef('')
const editing = shallowRef(false)
const editId = shallowRef(null)
const importDialogVisible = shallowRef(false)
const form = ref(createEmptyForm())

const userLabel = computed(() => auth.user?.username || '贡献者')
const roleLabel = computed(() => {
  if (auth.user?.skillGovernanceRole === 'CONTRIBUTOR') return '贡献者'
  return auth.user?.skillGovernanceRole || '未分配'
})

async function loadMine() {
  if (!auth.isAuthenticated) return
  loading.value = true
  error.value = ''
  try {
    const data = await api.get('/skills/me', {
      params: { page: page.value, size: size.value },
    })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '我的 Skill 加载失败'
  } finally {
    loading.value = false
  }
}

function createEmptyForm() {
  return {
    name: '',
    description: '',
    tagsStr: '',
    cloneCommand: '',
    contentMd: '',
    sourceRepositoryUrl: '',
    skillDirectory: '',
    assetLevel: 'TEAM',
    lifecycleStatus: 'CANDIDATE',
    skillCategory: 'CODING_IMPLEMENTATION',
    buildPriority: 'P2',
    creationSource: 'MANUAL',
    maintainer: auth.user?.username || '',
    teamName: '',
    version: '1.0.0',
    applicableScenarios: '',
    nonApplicableScenarios: '',
    inputRequirements: '',
    executionSteps: '',
    outputFormat: '',
    validationMethod: '',
    qualityStandard: '',
    referenceMaterials: '',
    riskLevel: 'LOW',
    reviewNotes: '',
    trialStartedAt: '',
    trialEndsAt: '',
    lastReviewedAt: '',
    nextReviewAt: '',
  }
}

function parseTags(value) {
  if (!value || !value.trim()) return []
  return value.split(/[,，]/).map((item) => item.trim()).filter(Boolean)
}

function skillToForm(row) {
  return {
    name: row.name || '',
    description: row.description || '',
    tagsStr: (row.tagNames || row.tags || []).map((item) => typeof item === 'string' ? item : item?.name).filter(Boolean).join(', '),
    cloneCommand: row.cloneCommand || '',
    contentMd: row.contentMd || '',
    sourceRepositoryUrl: row.sourceRepositoryUrl || '',
    skillDirectory: row.skillDirectory || '',
    assetLevel: row.assetLevel || 'TEAM',
    lifecycleStatus: row.lifecycleStatus || 'CANDIDATE',
    skillCategory: row.skillCategory || 'CODING_IMPLEMENTATION',
    buildPriority: row.buildPriority || 'P2',
    creationSource: row.creationSource || 'MANUAL',
    maintainer: row.maintainer || auth.user?.username || '',
    teamName: row.teamName || '',
    version: row.version || '1.0.0',
    applicableScenarios: row.applicableScenarios || '',
    nonApplicableScenarios: row.nonApplicableScenarios || '',
    inputRequirements: row.inputRequirements || '',
    executionSteps: row.executionSteps || '',
    outputFormat: row.outputFormat || '',
    validationMethod: row.validationMethod || '',
    qualityStandard: row.qualityStandard || '',
    referenceMaterials: row.referenceMaterials || '',
    riskLevel: row.riskLevel || 'LOW',
    reviewNotes: row.reviewNotes || '',
    trialStartedAt: row.trialStartedAt || '',
    trialEndsAt: row.trialEndsAt || '',
    lastReviewedAt: row.lastReviewedAt || '',
    nextReviewAt: row.nextReviewAt || '',
  }
}

function formToPayload() {
  return {
    name: form.value.name.trim(),
    description: form.value.description || '',
    tags: parseTags(form.value.tagsStr),
    cloneCommand: form.value.cloneCommand.trim(),
    contentMd: form.value.contentMd || '',
    sourceRepositoryUrl: form.value.sourceRepositoryUrl || '',
    skillDirectory: form.value.skillDirectory || '',
    assetLevel: form.value.assetLevel || 'TEAM',
    lifecycleStatus: form.value.lifecycleStatus || 'CANDIDATE',
    skillCategory: form.value.skillCategory || 'CODING_IMPLEMENTATION',
    buildPriority: form.value.buildPriority || 'P2',
    maintainer: form.value.maintainer || auth.user?.username || '',
    teamName: form.value.teamName || '',
    version: form.value.version || '1.0.0',
    applicableScenarios: form.value.applicableScenarios || '',
    nonApplicableScenarios: form.value.nonApplicableScenarios || '',
    inputRequirements: form.value.inputRequirements || '',
    executionSteps: form.value.executionSteps || '',
    outputFormat: form.value.outputFormat || '',
    validationMethod: form.value.validationMethod || '',
    qualityStandard: form.value.qualityStandard || '',
    referenceMaterials: form.value.referenceMaterials || '',
    riskLevel: form.value.riskLevel || 'LOW',
    reviewNotes: form.value.reviewNotes || '',
    trialStartedAt: form.value.trialStartedAt || null,
    trialEndsAt: form.value.trialEndsAt || null,
    lastReviewedAt: form.value.lastReviewedAt || null,
    nextReviewAt: form.value.nextReviewAt || null,
  }
}

function openCreate() {
  editId.value = null
  form.value = createEmptyForm()
  editing.value = true
  error.value = ''
}

function openEdit(row) {
  editId.value = row.id
  form.value = skillToForm(row)
  editing.value = true
  error.value = ''
}

async function saveSkill() {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  if (!form.value.cloneCommand?.trim()) {
    ElMessage.warning('请输入 Clone 命令')
    return
  }
  saving.value = true
  error.value = ''
  try {
    if (editId.value) {
      await api.put(`/skills/me/${editId.value}`, formToPayload())
      ElMessage.success('Skill 已更新')
    } else {
      await api.post('/skills/me', formToPayload())
      ElMessage.success('Skill 已提交')
    }
    editing.value = false
    await refreshAfterChange()
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function deleteSkill(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.name}」？`, '删除 Skill', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
    deletingId.value = row.id
    await api.delete(`/skills/me/${row.id}`)
    ElMessage.success('Skill 已删除')
    await refreshAfterChange()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    error.value = e.message || '删除失败'
  } finally {
    deletingId.value = null
  }
}

async function onImported() {
  await refreshAfterChange()
}

async function refreshAfterChange() {
  await loadMine()
  emit('changed')
}

function onPageChange(nextPage) {
  page.value = nextPage
  loadMine()
}

function openDetail(row) {
  visible.value = false
  router.push(`/skills/${row.id}`)
}

function lifecycleLabel(value) {
  const labels = {
    CANDIDATE: '候选',
    TRIAL: '试用中',
    REVIEWING: '评审中',
    APPROVED: '已入库',
    NEEDS_REVIEW: '待复审',
    ARCHIVED: '已归档',
  }
  return labels[value] || '候选'
}

function validationLabel(value) {
  const labels = {
    UNVALIDATED: '未校验',
    PASSED: '模板通过',
    FAILED: '模板未通过',
  }
  return labels[value] || '未校验'
}

function validationType(value) {
  if (value === 'PASSED') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'info'
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}
</script>

<template>
  <el-drawer
    v-model="visible"
    size="min(1040px, 96vw)"
    class="contributor-workspace"
    destroy-on-close
    @opened="loadMine"
  >
    <template #header>
      <div class="workspace-header">
        <div>
          <h2 class="workspace-header__title">我的 Skill</h2>
          <p class="workspace-header__meta">{{ userLabel }} / {{ roleLabel }}</p>
        </div>
        <div class="workspace-header__actions">
          <el-button :icon="Upload" @click="importDialogVisible = true">上传包</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">手工登记</el-button>
        </div>
      </div>
    </template>

    <div class="workspace-body">
      <el-alert
        v-if="error"
        type="error"
        :title="error"
        show-icon
        class="workspace-alert"
      />

      <div class="workspace-table-wrap">
        <el-table
          v-loading="loading"
          :data="items"
          empty-text="暂无提交"
          height="100%"
          class="workspace-table"
        >
          <el-table-column prop="name" label="名称" min-width="170" show-overflow-tooltip />
          <el-table-column label="治理状态" width="96">
            <template #default="{ row }">{{ lifecycleLabel(row.lifecycleStatus) }}</template>
          </el-table-column>
          <el-table-column label="模板" width="112">
            <template #default="{ row }">
              <el-tag :type="validationType(row.templateValidationStatus)" size="small" effect="plain">
                {{ validationLabel(row.templateValidationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="维护" min-width="132">
            <template #default="{ row }">
              <div class="workspace-maintainer">{{ row.maintainer || row.uploaderName || '-' }}</div>
              <div class="workspace-team">{{ row.teamName || '未分配团队' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="150">
            <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="132" align="right">
            <template #default="{ row }">
              <div class="workspace-row-actions">
                <el-tooltip content="查看" placement="top">
                  <el-button size="small" :icon="FolderOpened" circle aria-label="查看" @click="openDetail(row)" />
                </el-tooltip>
                <el-tooltip content="编辑" placement="top">
                  <el-button size="small" :icon="Edit" circle aria-label="编辑" @click="openEdit(row)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button
                    size="small"
                    type="danger"
                    :icon="Delete"
                    :loading="deletingId === row.id"
                    circle
                    aria-label="删除"
                    @click="deleteSkill(row)"
                  />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="workspace-footer">
        <el-pagination
          v-if="total > 0"
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="editing"
      :title="editId ? '编辑我的 Skill' : '手工登记 Skill'"
      width="920px"
      class="contributor-edit-dialog"
      destroy-on-close
    >
      <SkillAssetForm v-model="form" @submit="saveSkill" />
      <template #footer>
        <el-button @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSkill">保存</el-button>
      </template>
    </el-dialog>

    <SkillPackageImportDialog
      v-model="importDialogVisible"
      endpoint="/skills/me/import-package"
      title="上传 Skill 包"
      success-message="Skill 包已上传"
      @imported="onImported"
    />
  </el-drawer>
</template>

<style scoped>
.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  min-width: 0;
}

.workspace-header__title {
  margin: 0 0 4px;
  color: #111827;
  font-size: 20px;
  font-weight: 700;
}

.workspace-header__meta {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.workspace-header__actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  justify-content: flex-end;
  flex-shrink: 0;
}

.workspace-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.workspace-alert {
  margin-bottom: 2px;
}

.workspace-maintainer {
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

.workspace-team {
  color: #9ca3af;
  font-size: 12px;
}

.workspace-row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  min-width: 0;
}

.workspace-row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.workspace-footer {
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  padding-top: 2px;
}

:global(.contributor-workspace .el-drawer__header) {
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 0;
  padding: 16px 20px;
}

:global(.contributor-workspace .el-drawer__close-btn) {
  align-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin: 0 0 0 12px;
}

:global(.contributor-workspace .el-drawer__body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: 16px 20px 14px;
}

.workspace-table-wrap {
  flex: 1;
  min-height: 280px;
  overflow: hidden;
}

.workspace-table {
  width: 100%;
}

.workspace-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.workspace-table :deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}

.workspace-table :deep(.el-button.is-circle) {
  width: 28px;
  height: 28px;
  min-height: 28px;
  padding: 0;
}

:global(.contributor-edit-dialog .el-dialog),
:global(.el-dialog.contributor-edit-dialog) {
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 32px);
  max-height: calc(100dvh - 32px);
  margin-top: 16px !important;
  display: flex;
  flex-direction: column;
}

:global(.el-overlay-dialog:has(.contributor-edit-dialog)) {
  overflow: hidden;
}

:global(.contributor-edit-dialog .el-dialog__header),
:global(.el-dialog.contributor-edit-dialog .el-dialog__header),
:global(.contributor-edit-dialog .el-dialog__footer),
:global(.el-dialog.contributor-edit-dialog .el-dialog__footer) {
  flex-shrink: 0;
}

:global(.contributor-edit-dialog .el-dialog__body),
:global(.el-dialog.contributor-edit-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
}

@media (max-height: 680px) {
  :global(.contributor-edit-dialog .el-dialog),
  :global(.el-dialog.contributor-edit-dialog) {
    max-height: calc(100vh - 24px);
    max-height: calc(100dvh - 24px);
    margin-top: 12px !important;
    margin-bottom: 12px !important;
  }
}

@media (max-width: 760px) {
  .workspace-header {
    flex-direction: column;
    align-items: stretch;
  }

  .workspace-header__actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
