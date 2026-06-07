<template>
  <div class="pt-8">
    <div class="max-w-[1400px] mx-auto px-6 py-8">
    <template v-if="skill">
      <!-- 标题区：参考 ai.codefather.cn -->
      <h1 class="text-[28px] font-bold text-[#1f2937] mb-3 tracking-tight">{{ skill.name }}</h1>
      <div
        class="markdown-body description-body text-[16px] text-[#6b7280] leading-relaxed mb-6"
        v-html="renderedDescription"
      />

      <!-- 标签 -->
      <div v-if="hasBadges" class="flex flex-wrap gap-2 mb-8">
        <span v-for="t in skill.tagNames" :key="t" class="tag-chip">{{ t }}</span>
        <span class="tag-chip tag-chip--muted">内部</span>
        <span class="tag-chip tag-chip--asset">{{ assetLevelLabel(skill.assetLevel) }}</span>
        <span class="tag-chip tag-chip--status">{{ lifecycleLabel(skill.lifecycleStatus) }}</span>
        <span class="tag-chip">{{ skillCategoryLabel(skill.skillCategory) }}</span>
        <span class="tag-chip tag-chip--priority">{{ buildPriorityLabel(skill.buildPriority) }}</span>
        <span class="tag-chip tag-chip--risk">{{ riskLabel(skill.riskLevel) }}</span>
        <span class="tag-chip tag-chip--validation">{{ validationStatusLabel(skill.templateValidationStatus) }}</span>
      </div>

      <section class="asset-overview mb-8">
        <div class="asset-overview-item">
          <span class="asset-overview-label">维护人</span>
          <strong>{{ skill.maintainer || skill.uploaderName || '—' }}</strong>
        </div>
        <div class="asset-overview-item">
          <span class="asset-overview-label">团队/业务线</span>
          <strong>{{ skill.teamName || '未分配' }}</strong>
        </div>
        <div class="asset-overview-item">
          <span class="asset-overview-label">版本</span>
          <strong>{{ skill.version || '1.0.0' }}</strong>
        </div>
        <div class="asset-overview-item">
          <span class="asset-overview-label">分类/优先级</span>
          <strong>{{ skillCategoryLabel(skill.skillCategory) }} / {{ buildPriorityLabel(skill.buildPriority) }}</strong>
        </div>
        <div class="asset-overview-item">
          <span class="asset-overview-label">下次复审</span>
          <strong>{{ skill.nextReviewAt || '未设置' }}</strong>
        </div>
        <div class="asset-overview-item">
          <span class="asset-overview-label">模板校验</span>
          <strong>{{ validationStatusLabel(skill.templateValidationStatus) }}</strong>
        </div>
      </section>

      <SkillGovernancePanel v-if="skill.id" :skill-id="skill.id" />

      <!-- 快捷安装 -->
      <section class="mb-8">
        <h2 class="text-[18px] font-semibold text-[#1f2937] mb-3">快捷安装</h2>
        <p class="text-[14px] text-[#6b7280] mb-2">复制到终端去安装 Skill</p>
        <div class="flex gap-3 items-center rounded-xl border border-[#e5e7eb] bg-white p-4">
          <code class="flex-1 min-w-0 text-[14px] text-[#374151] font-mono truncate" :title="skill.cloneCommand">{{ skill.cloneCommand }}</code>
          <button
            type="button"
            title="复制"
            class="flex-shrink-0 p-2 rounded text-gray-400 hover:text-gray-600 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300"
            @click="copy(skill.cloneCommand)"
          >
            <span class="sr-only">复制</span>
            <svg viewBox="64 64 896 896" class="w-4 h-4" fill="currentColor" aria-hidden="true">
              <path d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z" />
            </svg>
          </button>
        </div>
      </section>

      <section v-if="hasStructuredGuide" class="mb-8">
        <h2 class="text-[18px] font-semibold text-[#1f2937] mb-4">资产化说明</h2>
        <div class="asset-guide-grid">
          <article v-if="skill.applicableScenarios" class="asset-guide-item">
            <h3>适用场景</h3>
            <p>{{ skill.applicableScenarios }}</p>
          </article>
          <article v-if="skill.nonApplicableScenarios" class="asset-guide-item">
            <h3>不适用场景</h3>
            <p>{{ skill.nonApplicableScenarios }}</p>
          </article>
          <article v-if="skill.inputRequirements" class="asset-guide-item">
            <h3>输入要求</h3>
            <p>{{ skill.inputRequirements }}</p>
          </article>
          <article v-if="skill.outputFormat" class="asset-guide-item">
            <h3>输出格式</h3>
            <p>{{ skill.outputFormat }}</p>
          </article>
          <article v-if="skill.qualityStandard" class="asset-guide-item">
            <h3>质量标准</h3>
            <p>{{ skill.qualityStandard }}</p>
          </article>
          <article v-if="skill.referenceMaterials" class="asset-guide-item">
            <h3>参考资料</h3>
            <p>{{ skill.referenceMaterials }}</p>
          </article>
        </div>
        <article v-if="skill.executionSteps" class="asset-guide-item asset-guide-item--wide">
          <h3>执行步骤</h3>
          <p>{{ skill.executionSteps }}</p>
        </article>
        <article v-if="skill.validationMethod" class="asset-guide-item asset-guide-item--wide">
          <h3>验证方式</h3>
          <p>{{ skill.validationMethod }}</p>
        </article>
        <article v-if="skill.sourceRepositoryUrl || skill.skillDirectory || skill.templateValidationNotes" class="asset-guide-item asset-guide-item--wide">
          <h3>仓库与校验</h3>
          <p>{{ repositoryInfo }}</p>
        </article>
        <article v-if="skill.reviewNotes" class="asset-guide-item asset-guide-item--wide">
          <h3>评审备注</h3>
          <p>{{ skill.reviewNotes }}</p>
        </article>
      </section>

      <!-- Markdown 文档 -->
      <section v-if="skill.contentMd" class="mb-8">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-[18px] font-semibold text-[#1f2937]">文档</h2>
          <button
            type="button"
            title="复制"
            class="flex-shrink-0 p-2 rounded text-gray-400 hover:text-gray-600 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300"
            @click="copy(skill.contentMd)"
          >
            <span class="sr-only">复制</span>
            <svg viewBox="64 64 896 896" class="w-4 h-4" fill="currentColor" aria-hidden="true">
              <path d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z" />
            </svg>
          </button>
        </div>
        <div
          class="markdown-body rounded-xl border border-[#e5e7eb] bg-white p-6"
          v-html="renderedMd"
        />
      </section>

      <!-- 上传者 -->
      <div class="text-[14px] text-[#9ca3af]">
        <span v-if="skill.uploaderName">上传者：{{ skill.uploaderName }}</span>
      </div>
    </template>

    <template v-else>
      <div v-if="loading" class="py-12 text-center text-[#6b7280]">加载中…</div>
      <el-alert v-else type="error" :title="error || '未找到'" show-icon />
    </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import SkillGovernancePanel from '../components/skills/SkillGovernancePanel.vue'

const route = useRoute()
const skill = ref(null)
const loading = ref(true)
const error = ref('')

const assetLevelOptions = [
  { value: 'TEAM', label: '团队级' },
  { value: 'COMPANY', label: '公司级' },
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

const hasStructuredGuide = computed(() => {
  const s = skill.value
  return Boolean(s?.applicableScenarios || s?.nonApplicableScenarios || s?.inputRequirements || s?.executionSteps || s?.outputFormat || s?.validationMethod || s?.qualityStandard || s?.referenceMaterials || s?.sourceRepositoryUrl || s?.skillDirectory || s?.templateValidationNotes || s?.reviewNotes)
})

const hasBadges = computed(() => {
  const s = skill.value
  return Boolean((s?.tagNames || []).length || s?.assetLevel || s?.lifecycleStatus || s?.riskLevel)
})

const repositoryInfo = computed(() => {
  const s = skill.value || {}
  return [
    s.sourceRepositoryUrl ? `仓库地址：${s.sourceRepositoryUrl}` : '',
    s.skillDirectory ? `Skill 目录：${s.skillDirectory}` : '',
    s.templateValidationNotes ? `校验备注：${s.templateValidationNotes}` : '',
  ].filter(Boolean).join('\n')
})

const renderedDescription = computed(() => {
  const raw = skill.value?.description || '暂无描述'
  try {
    const html = marked.parse(raw, { gfm: true })
    return DOMPurify.sanitize(html)
  } catch {
    return DOMPurify.sanitize(raw)
  }
})

const renderedMd = computed(() => {
  const md = stripMarkdownFrontmatter(skill.value?.contentMd)
  if (!md) return ''
  try {
    const html = marked.parse(md, { gfm: true })
    return DOMPurify.sanitize(html)
  } catch {
    return ''
  }
})

function stripMarkdownFrontmatter(markdown) {
  if (!markdown) return ''
  return markdown.replace(/^---\s*\n[\s\S]*?\n---\s*\n?/, '')
}

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (_) {
    ElMessage.error('复制失败')
  }
}

function optionLabel(options, value, fallback = '—') {
  return options.find((item) => item.value === value)?.label || fallback
}

function assetLevelLabel(value) {
  return optionLabel(assetLevelOptions, value, '团队级')
}

function lifecycleLabel(value) {
  return optionLabel(lifecycleOptions, value, '候选')
}

function riskLabel(value) {
  return optionLabel(riskOptions, value, '低风险')
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

onMounted(async () => {
  try {
    skill.value = await api.get('/skills/' + route.params.id)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.markdown-body :deep(h1) { font-size: 1.5em; margin: 1em 0 0.5em; font-weight: 600; }
.markdown-body :deep(h2) { font-size: 1.25em; margin: 1.25em 0 0.5em; font-weight: 600; }
.markdown-body :deep(h3) { font-size: 1.1em; margin: 1em 0 0.5em; font-weight: 600; }
.markdown-body :deep(p) { margin: 0.75em 0; line-height: 1.6; color: #374151; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 0.75em 0; padding-left: 1.5em; }
.markdown-body :deep(li) { margin: 0.25em 0; }
.markdown-body :deep(code) { background: #f3f4f6; padding: 0.2em 0.4em; border-radius: 4px; font-size: 0.9em; }
.markdown-body :deep(pre) { background: #1f2937; color: #e5e7eb; padding: 1em; border-radius: 8px; overflow-x: auto; margin: 1em 0; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-body :deep(blockquote) { border-left: 4px solid #e5e7eb; margin: 1em 0; padding-left: 1em; color: #6b7280; }
.markdown-body :deep(a) { color: #2563eb; text-decoration: none; }
.markdown-body :deep(a:hover) { text-decoration: underline; }
.markdown-body :deep(table) { border-collapse: collapse; width: 100%; margin: 1em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #e5e7eb; padding: 0.5em 0.75em; text-align: left; }
.markdown-body :deep(th) { background: #f9fafb; font-weight: 600; }

.asset-overview {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.asset-overview-item {
  min-height: 76px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
}

.asset-overview-label {
  display: block;
  margin-bottom: 8px;
  color: #9ca3af;
  font-size: 12px;
}

.asset-overview-item strong {
  color: #1f2937;
  font-size: 15px;
}

.asset-guide-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.asset-guide-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
  margin-bottom: 12px;
}

.asset-guide-item--wide {
  margin-bottom: 12px;
}

.asset-guide-item h3 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.asset-guide-item p {
  margin: 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-line;
}

@media (max-width: 900px) {
  .asset-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .asset-guide-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .asset-overview {
    grid-template-columns: 1fr;
  }
}
</style>
