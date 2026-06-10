<script setup>
import { computed, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'

const props = defineProps({
  endpoint: {
    type: String,
    default: '/admin/skills/import-package',
  },
  title: {
    type: String,
    default: '导入 Skill 包',
  },
  successMessage: {
    type: String,
    default: 'Skill 包已导入',
  },
})

const visible = defineModel({ type: Boolean, required: true })
const emit = defineEmits(['imported'])

const fileList = ref([])
const selectedFile = shallowRef(null)
const importing = shallowRef(false)
const result = shallowRef(null)
const error = shallowRef('')

const canImport = computed(() => Boolean(selectedFile.value) && !importing.value)

function onFileChange(file, files) {
  selectedFile.value = file.raw || null
  fileList.value = files.slice(-1)
  result.value = null
  error.value = ''
}

function onFileRemove() {
  selectedFile.value = null
  fileList.value = []
}

function reset() {
  selectedFile.value = null
  fileList.value = []
  result.value = null
  error.value = ''
}

async function submitImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择 Skill 包')
    return
  }
  importing.value = true
  error.value = ''
  try {
    const form = new FormData()
    form.append('file', selectedFile.value)
    result.value = await api.post(props.endpoint, form)
    if (result.value?.skill) {
      ElMessage.success(props.successMessage)
      emit('imported', result.value)
    } else {
      ElMessage.warning('Skill 包校验未通过')
    }
  } catch (e) {
    error.value = e.message || '导入失败'
  } finally {
    importing.value = false
  }
}

function closeDialog() {
  visible.value = false
}

function statusType(report) {
  if (!report) return 'info'
  return report.passed ? 'success' : 'danger'
}

function statusText(report) {
  if (!report) return '未校验'
  return report.passed ? '通过' : '未通过'
}

function gitLabStatusType(publication) {
  if (!publication) return 'info'
  if (publication.status === 'PUBLISHED') return 'success'
  if (publication.status === 'DISABLED') return 'info'
  return 'warning'
}

function gitLabStatusText(publication) {
  const labels = {
    PUBLISHED: '已发布',
    DISABLED: '未启用',
  }
  return labels[publication?.status] || '待处理'
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="props.title"
    width="820px"
    class="skill-import-dialog"
    @closed="reset"
  >
    <el-upload
      drag
      accept=".zip,application/zip"
      :auto-upload="false"
      :limit="1"
      :file-list="fileList"
      :on-change="onFileChange"
      :on-remove="onFileRemove"
    >
      <div class="upload-body">
        <div class="upload-body__title">skill-creator zip</div>
        <div class="upload-body__meta">
          SKILL.md / agents/openai.yaml / sourceRepositoryUrl / quick_validate.py 通过证据
        </div>
      </div>
    </el-upload>

    <div v-if="result" class="import-result">
      <section class="validation-block">
        <div class="validation-block__header">
          <strong>包结构校验</strong>
          <el-tag :type="statusType(result.packageValidation)" effect="plain">
            {{ statusText(result.packageValidation) }}
          </el-tag>
        </div>
        <p class="validation-block__notes">{{ result.packageValidation?.notes }}</p>
        <el-table :data="result.packageValidation?.items || []" size="small">
          <el-table-column prop="label" label="检查项" min-width="180" />
          <el-table-column label="要求" width="90">
            <template #default="{ row }">{{ row.required ? '必填' : '建议' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag :type="row.passed ? 'success' : row.required ? 'danger' : 'info'" size="small" effect="plain">
                {{ row.passed ? '通过' : '缺失' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="220" />
        </el-table>
      </section>

      <section v-if="result.assetValidation" class="validation-block">
        <div class="validation-block__header">
          <strong>资产字段校验</strong>
          <el-tag :type="statusType(result.assetValidation)" effect="plain">
            {{ statusText(result.assetValidation) }}
          </el-tag>
        </div>
        <p class="validation-block__notes">{{ result.assetValidation?.notes }}</p>
        <el-table :data="result.assetValidation?.items || []" size="small" max-height="280">
          <el-table-column prop="label" label="检查项" min-width="180" />
          <el-table-column label="要求" width="90">
            <template #default="{ row }">{{ row.required ? '必填' : '建议' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag :type="row.passed ? 'success' : row.required ? 'danger' : 'info'" size="small" effect="plain">
                {{ row.passed ? '通过' : '缺失' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="说明" min-width="220" />
        </el-table>
      </section>

      <section v-if="result.gitLabPublication" class="validation-block">
        <div class="validation-block__header">
          <strong>GitLab 发布</strong>
          <el-tag :type="gitLabStatusType(result.gitLabPublication)" effect="plain">
            {{ gitLabStatusText(result.gitLabPublication) }}
          </el-tag>
        </div>
        <p class="validation-block__notes">{{ result.gitLabPublication.message }}</p>
        <div class="publish-meta">
          <div v-if="result.gitLabPublication.repositoryUrl" class="publish-meta__row">
            <span>仓库</span>
            <el-link :href="result.gitLabPublication.repositoryUrl" target="_blank" type="primary">
              {{ result.gitLabPublication.repositoryUrl }}
            </el-link>
          </div>
          <div v-if="result.gitLabPublication.mergeRequestUrl" class="publish-meta__row">
            <span>MR</span>
            <el-link :href="result.gitLabPublication.mergeRequestUrl" target="_blank" type="primary">
              {{ result.gitLabPublication.mergeRequestUrl }}
            </el-link>
          </div>
          <div v-if="result.gitLabPublication.branchName" class="publish-meta__row">
            <span>分支</span>
            <code>{{ result.gitLabPublication.branchName }}</code>
          </div>
          <div v-if="result.gitLabPublication.skillPath" class="publish-meta__row">
            <span>路径</span>
            <code>{{ result.gitLabPublication.skillPath }}</code>
          </div>
        </div>
      </section>
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon class="import-error" />

    <template #footer>
      <el-button @click="closeDialog">关闭</el-button>
      <el-button type="primary" :loading="importing" :disabled="!canImport" @click="submitImport">导入</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.upload-body {
  padding: 10px 0;
}

.upload-body__title {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.upload-body__meta {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.import-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 18px;
}

.validation-block {
  border-top: 1px solid #e5e7eb;
  padding-top: 14px;
}

.validation-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.validation-block__notes {
  margin: 0 0 10px;
  color: #6b7280;
  font-size: 13px;
}

.import-error {
  margin-top: 14px;
}

.publish-meta {
  display: grid;
  gap: 8px;
}

.publish-meta__row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: #374151;
  font-size: 13px;
}

.publish-meta__row span {
  color: #6b7280;
}

.publish-meta__row code {
  overflow-wrap: anywhere;
  color: #111827;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

</style>
