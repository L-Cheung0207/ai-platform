<template>
  <div>
    <h1 class="text-xl font-bold text-gray-800 mb-2">Skill 资产管理</h1>
    <p class="text-gray-500 text-sm mb-4">管理员可维护 Skill 的层级、状态、复审和验证信息。隐藏后公开列表不可见。</p>

    <section class="metrics-grid" v-loading="metricsLoading">
      <article class="metric-item">
        <span>资产总数</span>
        <strong>{{ metrics?.totalSkills || 0 }}</strong>
      </article>
      <article class="metric-item">
        <span>团队级/公司级资产</span>
        <strong>{{ (metrics?.teamAssetCount || 0) + (metrics?.companyAssetCount || 0) }}</strong>
      </article>
      <article class="metric-item">
        <span>已入库</span>
        <strong>{{ metrics?.approvedCount || 0 }}</strong>
      </article>
      <article class="metric-item">
        <span>模板通过</span>
        <strong>{{ metrics?.templateValidatedCount || 0 }}</strong>
      </article>
      <article class="metric-item">
        <span>模板未通过</span>
        <strong>{{ metrics?.templateValidationFailedCount || 0 }}</strong>
      </article>
      <article class="metric-item">
        <span>待复审</span>
        <strong>{{ metrics?.needsReviewCount || 0 }}</strong>
      </article>
      <article class="metric-item">
        <span>过期 Skill 比例</span>
        <strong>{{ formatPercent(metrics?.overdueSkillRate) }}</strong>
      </article>
      <article class="metric-item">
        <span>近 30 天使用</span>
        <strong>{{ metrics?.monthlyUsageCount || 0 }}</strong>
      </article>
      <article class="metric-item">
        <span>质量信号</span>
        <strong>{{ metrics?.qualitySignalCount || 0 }}</strong>
      </article>
      <article class="metric-item">
        <span>反馈待处理</span>
        <strong>{{ metrics?.openFeedbackCount || 0 }}</strong>
      </article>
    </section>

    <el-dialog v-model="editing" :title="editId ? '编辑 Skill 资产' : '新建 Skill 资产'" width="920" draggable class="admin-dialog">
      <SkillAssetForm v-model="form" @submit="saveSkill" />
      <template #footer>
        <el-button native-type="button" @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSkill">保存</el-button>
      </template>
    </el-dialog>

    <SkillReviewDialog v-model="reviewDialogVisible" :skill="selectedSkill" @saved="onGovernanceChanged" />
    <SkillFeedbackDrawer v-model="feedbackDrawerVisible" :skill="selectedSkill" @updated="loadMetrics" />
    <SkillValidationDialog v-model="validationDialogVisible" :skill="selectedSkill" :report="validationReport" />
    <SkillPackageImportDialog v-model="importDialogVisible" @imported="onPackageImported" />

    <div class="admin-skills-toolbar">
      <div class="admin-skills-actions">
        <el-button type="primary" @click="startNew">新建 Skill 资产</el-button>
        <el-button @click="importDialogVisible = true">导入 Skill 包</el-button>
      </div>
      <div class="skill-filter-grid" role="search" aria-label="Skill 筛选">
        <el-input v-model="keyword" placeholder="关键词" maxlength="200" clearable class="skill-filter-keyword" @keyup.enter="load" />
        <el-select v-model="assetLevelFilter" clearable placeholder="资产层级" class="skill-filter-control" @change="onFilterChange">
          <el-option v-for="item in assetLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="lifecycleFilter" clearable placeholder="生命周期" class="skill-filter-control" @change="onFilterChange">
          <el-option v-for="item in lifecycleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="skillCategoryFilter" clearable placeholder="Skill 分类" class="skill-filter-control" @change="onFilterChange">
          <el-option v-for="item in skillCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="buildPriorityFilter" clearable placeholder="优先级" class="skill-filter-control" @change="onFilterChange">
          <el-option v-for="item in buildPriorityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" class="skill-filter-submit" @click="load">搜索</el-button>
      </div>
    </div>
    <el-table :data="items" stripe>
      <el-table-column prop="name" label="名称">
        <template #default="{ row }">
          <router-link :to="'/skills/' + row.id" class="text-primary hover:underline">{{ row.name }}</router-link>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="160">
        <template #default="{ row }">
          <template v-if="(row.tagNames || row.tags || []).length">
            <el-tag
              v-for="t in (row.tagNames || row.tags || [])"
              :key="typeof t === 'string' ? t : t?.name"
              size="small"
              effect="plain"
              class="mr-1 mb-1"
            >
              {{ typeof t === 'string' ? t : t?.name }}
            </el-tag>
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column label="治理" min-width="210">
        <template #default="{ row }">
          <div class="asset-cell">
            <el-tag size="small" effect="plain">{{ assetLevelLabel(row.assetLevel) }}</el-tag>
            <el-tag size="small" :type="lifecycleTagType(row.lifecycleStatus)" effect="light">
              {{ lifecycleLabel(row.lifecycleStatus) }}
            </el-tag>
            <el-tag size="small" :type="riskTagType(row.riskLevel)" effect="plain">
              {{ riskLabel(row.riskLevel) }}
            </el-tag>
            <el-tag size="small" effect="plain">{{ skillCategoryLabel(row.skillCategory) }}</el-tag>
            <el-tag size="small" :type="priorityTagType(row.buildPriority)" effect="light">
              {{ buildPriorityLabel(row.buildPriority) }}
            </el-tag>
            <el-tag size="small" :type="validationTagType(row.templateValidationStatus)" effect="plain">
              {{ validationStatusLabel(row.templateValidationStatus) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="维护" min-width="150">
        <template #default="{ row }">
          <div class="text-sm text-gray-700">{{ row.maintainer || row.uploaderName || '—' }}</div>
          <div class="text-xs text-gray-400">{{ row.teamName || '未分配团队' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="132">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ creationSourceLabel(row.creationSource) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="复审" width="120">
        <template #default="{ row }">
          <span class="text-sm text-gray-600">{{ row.nextReviewAt || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="visibility" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.visibility === 'VISIBLE' ? 'success' : 'info'" size="small">
            {{ row.visibility === 'VISIBLE' ? '可见' : '已隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="startEdit(row)">编辑</el-button>
          <el-button size="small" link type="primary" :loading="validatingId === row.id" @click="validateTemplate(row)">校验</el-button>
          <el-button size="small" link type="primary" @click="openReview(row)">评审</el-button>
          <el-button size="small" link type="primary" @click="openFeedback(row)">反馈</el-button>
          <el-button v-if="row.visibility === 'VISIBLE'" size="small" link type="primary" @click="hide(row.id)">隐藏</el-button>
          <el-button v-else size="small" link type="primary" @click="unhide(row.id)">恢复可见</el-button>
          <el-button size="small" link type="danger" @click="remove(row.id)">永久删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="admin-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../services/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import SkillAssetForm from '../../components/skills/SkillAssetForm.vue'
import SkillReviewDialog from '../../components/skills/SkillReviewDialog.vue'
import SkillFeedbackDrawer from '../../components/skills/SkillFeedbackDrawer.vue'
import SkillValidationDialog from '../../components/skills/SkillValidationDialog.vue'
import SkillPackageImportDialog from '../../components/skills/SkillPackageImportDialog.vue'

const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const assetLevelFilter = ref('')
const lifecycleFilter = ref('')
const skillCategoryFilter = ref('')
const buildPriorityFilter = ref('')
const editing = ref(false)
const editId = ref(null)
const form = ref(createEmptyForm())
const saving = ref(false)
const error = ref('')
const metrics = ref(null)
const metricsLoading = ref(false)
const selectedSkill = ref(null)
const reviewDialogVisible = ref(false)
const feedbackDrawerVisible = ref(false)
const validationDialogVisible = ref(false)
const importDialogVisible = ref(false)
const validationReport = ref(null)
const validatingId = ref(null)

const assetLevelOptions = [
  { value: 'TEAM', label: '团队' },
  { value: 'COMPANY', label: '公司' },
]

const lifecycleOptions = [
  { value: 'CANDIDATE', label: '候选' },
  { value: 'TRIAL', label: '试用中' },
  { value: 'REVIEWING', label: '评审中' },
  { value: 'APPROVED', label: '已入库' },
  { value: 'NEEDS_REVIEW', label: '待复审' },
  { value: 'ARCHIVED', label: '已归档' },
]

const riskOptions = [
  { value: 'LOW', label: '低风险' },
  { value: 'MEDIUM', label: '中风险' },
  { value: 'HIGH', label: '高风险' },
]

const skillCategoryOptions = [
  { value: 'REQUIREMENT_ANALYSIS', label: '需求分析' },
  { value: 'ARCHITECTURE_DESIGN', label: '架构设计' },
  { value: 'CODING_IMPLEMENTATION', label: '编码实现' },
  { value: 'TESTING_VALIDATION', label: '测试验证' },
  { value: 'CODE_REVIEW', label: '代码 Review' },
  { value: 'OPS_TROUBLESHOOTING', label: '排障运维' },
  { value: 'DOCUMENTATION_KNOWLEDGE', label: '文档知识' },
]

const buildPriorityOptions = [
  { value: 'P0', label: 'P0' },
  { value: 'P1', label: 'P1' },
  { value: 'P2', label: 'P2' },
]

const validationOptions = [
  { value: 'UNVALIDATED', label: '未校验' },
  { value: 'PASSED', label: '模板通过' },
  { value: 'FAILED', label: '模板未通过' },
]

const creationSourceOptions = [
  { value: 'MANUAL', label: '手工录入' },
  { value: 'SKILL_CREATOR_PACKAGE', label: 'skill-creator 包' },
  { value: 'REPOSITORY_SYNC', label: '仓库同步' },
  { value: 'SEED', label: '试点样板' },
]

onMounted(() => {
  load()
  loadMetrics()
})

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
    maintainer: '',
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

function onSizeChange() {
  page.value = 1
  load()
}

function onFilterChange() {
  page.value = 1
  load()
}

function parseTags(str) {
  if (!str || !str.trim()) return []
  return str.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

function startNew() {
  editId.value = null
  form.value = createEmptyForm()
  editing.value = true
  error.value = ''
}

function startEdit(row) {
  editId.value = row.id
  form.value = {
    name: row.name,
    description: row.description || '',
    tagsStr: (row.tagNames || row.tags || []).map(t => typeof t === 'string' ? t : t?.name).filter(Boolean).join(', '),
    cloneCommand: row.cloneCommand || '',
    contentMd: row.contentMd || '',
    sourceRepositoryUrl: row.sourceRepositoryUrl || '',
    skillDirectory: row.skillDirectory || '',
    assetLevel: row.assetLevel || 'TEAM',
    lifecycleStatus: row.lifecycleStatus || 'CANDIDATE',
    skillCategory: row.skillCategory || 'CODING_IMPLEMENTATION',
    buildPriority: row.buildPriority || 'P2',
    creationSource: row.creationSource || 'MANUAL',
    maintainer: row.maintainer || '',
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
    const body = {
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
      maintainer: form.value.maintainer || '',
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
    if (editId.value) {
      await api.put('/admin/skills/' + editId.value, body)
      ElMessage.success('更新成功')
    } else {
      await api.post('/admin/skills', body)
      ElMessage.success('创建成功')
    }
    editing.value = false
    await load()
    await loadMetrics()
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function load() {
  try {
    const params = { page: Number(page.value) || 1, size: size.value }
    if (keyword.value) params.keyword = String(keyword.value).trim()
    if (assetLevelFilter.value) params.assetLevel = assetLevelFilter.value
    if (lifecycleFilter.value) params.lifecycleStatus = lifecycleFilter.value
    if (skillCategoryFilter.value) params.skillCategory = skillCategoryFilter.value
    if (buildPriorityFilter.value) params.buildPriority = buildPriorityFilter.value
    const data = await api.get('/admin/skills', { params })
    items.value = data.items || []
    total.value = data.total ?? 0
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

async function loadMetrics() {
  metricsLoading.value = true
  try {
    metrics.value = await api.get('/admin/skill-metrics')
  } catch (e) {
    error.value = e.message || '加载指标失败'
  } finally {
    metricsLoading.value = false
  }
}

function openReview(row) {
  selectedSkill.value = row
  reviewDialogVisible.value = true
}

function openFeedback(row) {
  selectedSkill.value = row
  feedbackDrawerVisible.value = true
}

async function onGovernanceChanged() {
  await load()
  await loadMetrics()
}

async function onPackageImported() {
  await load()
  await loadMetrics()
}

async function validateTemplate(row) {
  validatingId.value = row.id
  selectedSkill.value = row
  error.value = ''
  try {
    validationReport.value = await api.post('/admin/skills/' + row.id + '/validate-template')
    validationDialogVisible.value = true
    ElMessage.success(validationReport.value.passed ? '模板校验通过' : '模板校验未通过')
    await load()
    await loadMetrics()
  } catch (e) {
    error.value = e.message || '模板校验失败'
  } finally {
    validatingId.value = null
  }
}

async function hide(id) {
  try {
    await api.post('/admin/skills/' + id + '/hide')
    ElMessage.success('已隐藏')
    await load()
    await loadMetrics()
  } catch (e) {
    error.value = e.message || '操作失败'
  }
}

async function unhide(id) {
  try {
    await api.post('/admin/skills/' + id + '/unhide')
    ElMessage.success('已恢复可见')
    await load()
    await loadMetrics()
  } catch (e) {
    error.value = e.message || '操作失败'
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定永久删除该 Skill？此操作不可恢复。', '提示', { type: 'warning' })
    await api.delete('/admin/skills/' + id)
    ElMessage.success('删除成功')
    await load()
    await loadMetrics()
  } catch (e) {
    if (e !== 'cancel') error.value = e.message || '删除失败'
  }
}

function optionLabel(options, value, fallback = '—') {
  return options.find((item) => item.value === value)?.label || fallback
}

function assetLevelLabel(value) {
  return optionLabel(assetLevelOptions, value)
}

function lifecycleLabel(value) {
  return optionLabel(lifecycleOptions, value)
}

function riskLabel(value) {
  return optionLabel(riskOptions, value)
}

function skillCategoryLabel(value) {
  return optionLabel(skillCategoryOptions, value, '编码实现')
}

function buildPriorityLabel(value) {
  return optionLabel(buildPriorityOptions, value, 'P2')
}

function validationStatusLabel(value) {
  return optionLabel(validationOptions, value, '未校验')
}

function creationSourceLabel(value) {
  return optionLabel(creationSourceOptions, value, '手工录入')
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function lifecycleTagType(value) {
  if (value === 'APPROVED') return 'success'
  if (value === 'NEEDS_REVIEW') return 'warning'
  if (value === 'ARCHIVED') return 'info'
  return 'primary'
}

function riskTagType(value) {
  if (value === 'HIGH') return 'danger'
  if (value === 'MEDIUM') return 'warning'
  return 'success'
}

function priorityTagType(value) {
  if (value === 'P0') return 'danger'
  if (value === 'P1') return 'warning'
  return 'info'
}

function validationTagType(value) {
  if (value === 'PASSED') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'info'
}
</script>

<style scoped>
.admin-skills-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.admin-skills-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.admin-skills-toolbar .admin-skills-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.skill-filter-grid {
  display: flex;
  flex: 1 1 640px;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  justify-content: end;
  min-width: 0;
}

.skill-filter-keyword {
  flex: 1 1 200px;
  min-width: 180px;
  max-width: 280px;
}

.skill-filter-control {
  flex: 0 1 136px;
  min-width: 128px;
}

.skill-filter-submit {
  flex: 0 0 auto;
  min-width: 76px;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
  padding: 1rem 0;
}
.admin-pagination :deep(.el-pagination) {
  font-weight: 500;
}
.admin-pagination :deep(.el-pagination .el-pager li) {
  min-width: 32px;
  height: 32px;
  line-height: 32px;
}

.asset-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.metric-item {
  min-height: 86px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.metric-item span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 10px;
}

.metric-item strong {
  display: block;
  color: #111827;
  font-size: 24px;
  line-height: 1.1;
}

@media (max-width: 1280px) {
  .metrics-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .skill-filter-submit {
    flex: 1 1 128px;
  }

  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .admin-skills-toolbar,
  .admin-skills-actions {
    flex-direction: column;
  }

  .admin-skills-actions,
  .skill-filter-grid {
    width: 100%;
  }

  .admin-skills-actions :deep(.el-button),
  .skill-filter-keyword,
  .skill-filter-control,
  .skill-filter-submit {
    flex: 1 1 100%;
    max-width: none;
    width: 100%;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
