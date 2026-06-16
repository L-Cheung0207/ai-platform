<script setup>
const props = defineProps({
  hideReviewFields: {
    type: Boolean,
    default: false,
  },
})
const form = defineModel({ type: Object, required: true })
const emit = defineEmits(['submit'])

const assetLevelOptions = [
  { value: 'TEAM', label: '团队级' },
  { value: 'COMPANY', label: '公司级' },
]

const lifecycleOptions = [
  { value: 'CANDIDATE', label: '候选' },
  { value: 'TRIAL', label: '试用中' },
  { value: 'REVIEWING', label: '评审中' },
  { value: 'APPROVED', label: '已入库（评审通过）', disabled: true },
  { value: 'NEEDS_REVIEW', label: '待复审' },
  { value: 'ARCHIVED', label: '已归档' },
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
  { value: 'P0', label: 'P0 样板优先' },
  { value: 'P1', label: 'P1 推广优先' },
  { value: 'P2', label: 'P2 储备建设' },
]

const riskOptions = [
  { value: 'LOW', label: '低风险' },
  { value: 'MEDIUM', label: '中风险' },
  { value: 'HIGH', label: '高风险' },
]

const creationSourceLabels = {
  MANUAL: '手工录入',
  SKILL_CREATOR_PACKAGE: 'skill-creator 包',
  REPOSITORY_SYNC: '仓库同步',
  SEED: '试点样板',
}

function creationSourceLabel(value) {
  return creationSourceLabels[value] || '手工录入'
}
</script>

<template>
  <el-form :model="form" label-position="top" class="skill-asset-form" @submit.prevent="emit('submit')">
    <section class="skill-form-section">
      <h3 class="skill-form-section__title">基础登记</h3>
      <div class="skill-form-grid">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.version" maxlength="50" placeholder="1.0.0" />
        </el-form-item>
      </div>
      <el-form-item label="创建来源">
        <el-input :model-value="creationSourceLabel(form.creationSource)" disabled />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="5000" show-word-limit />
      </el-form-item>
      <div class="skill-form-grid">
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="form.tagsStr" placeholder="如 Java, Review, 测试" />
        </el-form-item>
        <el-form-item label="Clone 命令" required>
          <el-input v-model="form.cloneCommand" placeholder="git clone ..." maxlength="2000" show-word-limit />
        </el-form-item>
      </div>
      <div class="skill-form-grid">
        <el-form-item label="仓库地址">
          <el-input v-model="form.sourceRepositoryUrl" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="Skill 目录">
          <el-input v-model="form.skillDirectory" maxlength="255" placeholder="java-code-review" />
        </el-form-item>
      </div>
    </section>

    <section class="skill-form-section">
      <h3 class="skill-form-section__title">资产治理</h3>
      <el-alert
        v-if="!props.hideReviewFields"
        title="已入库状态由模板校验通过后的评审自动产生"
        type="info"
        :closable="false"
        show-icon
        class="skill-form-alert"
      />
      <div class="skill-form-grid skill-form-grid--three">
        <el-form-item label="资产层级">
          <el-select v-model="form.assetLevel" class="w-full">
            <el-option v-for="item in assetLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!props.hideReviewFields" label="生命周期">
          <el-select v-model="form.lifecycleStatus" class="w-full">
            <el-option
              v-for="item in lifecycleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
              :disabled="item.disabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!props.hideReviewFields" label="风险等级">
          <el-select v-model="form.riskLevel" class="w-full">
            <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </div>
      <div class="skill-form-grid">
        <el-form-item label="Skill 分类">
          <el-select v-model="form.skillCategory" class="w-full">
            <el-option v-for="item in skillCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!props.hideReviewFields" label="建设优先级">
          <el-select v-model="form.buildPriority" class="w-full">
            <el-option v-for="item in buildPriorityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </div>
      <div class="skill-form-grid">
        <el-form-item label="维护人">
          <el-input v-model="form.maintainer" maxlength="100" />
        </el-form-item>
        <el-form-item v-if="!props.hideReviewFields" label="团队/业务线">
          <el-input v-model="form.teamName" maxlength="120" />
        </el-form-item>
      </div>
      <div v-if="!props.hideReviewFields" class="skill-form-grid">
        <el-form-item label="试用开始日期">
          <el-date-picker v-model="form.trialStartedAt" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
        <el-form-item label="试用结束日期">
          <el-date-picker v-model="form.trialEndsAt" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
      </div>
      <div v-if="!props.hideReviewFields" class="skill-form-grid">
        <el-form-item label="最近复审日期">
          <el-date-picker v-model="form.lastReviewedAt" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
        <el-form-item label="下次复审日期">
          <el-date-picker v-model="form.nextReviewAt" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
      </div>
    </section>

    <section class="skill-form-section">
      <h3 class="skill-form-section__title">Skill 模板</h3>
      <div class="skill-form-grid">
        <el-form-item label="适用场景">
          <el-input v-model="form.applicableScenarios" type="textarea" :rows="3" maxlength="20000" show-word-limit />
        </el-form-item>
        <el-form-item label="不适用场景">
          <el-input v-model="form.nonApplicableScenarios" type="textarea" :rows="3" maxlength="20000" show-word-limit />
        </el-form-item>
      </div>
      <div class="skill-form-grid">
        <el-form-item label="输入要求">
          <el-input v-model="form.inputRequirements" type="textarea" :rows="4" maxlength="20000" show-word-limit />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="form.outputFormat" type="textarea" :rows="4" maxlength="20000" show-word-limit />
        </el-form-item>
      </div>
      <el-form-item label="执行步骤">
        <el-input v-model="form.executionSteps" type="textarea" :rows="6" maxlength="100000" show-word-limit />
      </el-form-item>
      <el-form-item v-if="!props.hideReviewFields" label="验证方式">
        <el-input v-model="form.validationMethod" type="textarea" :rows="4" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-form-item v-if="!props.hideReviewFields" label="质量标准">
        <el-input v-model="form.qualityStandard" type="textarea" :rows="4" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-form-item label="参考资料">
        <el-input v-model="form.referenceMaterials" type="textarea" :rows="4" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-form-item v-if="!props.hideReviewFields" label="评审备注">
        <el-input v-model="form.reviewNotes" type="textarea" :rows="3" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-form-item label="Markdown 文档">
        <el-input v-model="form.contentMd" type="textarea" :rows="8" placeholder="SKILL.md 等 Markdown 文档内容，详情页将渲染展示" />
      </el-form-item>
    </section>
  </el-form>
</template>

<style scoped>
.skill-asset-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.skill-form-section {
  border-top: 1px solid #e5e7eb;
  padding-top: 16px;
}

.skill-form-section:first-child {
  border-top: 0;
  padding-top: 0;
}

.skill-form-section__title {
  margin: 0 0 12px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.skill-form-alert {
  margin-bottom: 14px;
}

.skill-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.skill-form-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 720px) {
  .skill-form-grid,
  .skill-form-grid--three {
    grid-template-columns: 1fr;
  }
}

.skill-asset-form :deep(textarea) {
  font-family: inherit;
  font-size: inherit;
  line-height: 1.5;
}
</style>
