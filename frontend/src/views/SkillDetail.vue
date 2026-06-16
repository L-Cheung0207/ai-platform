<template>
  <div class="skill-detail-page">
    <template v-if="skill">
      <header class="skill-hero" :class="'skill-hero--' + heroVariant">
        <div class="skill-hero__inner">
          <router-link to="/skills" class="skill-back">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
            Skill 资产库
          </router-link>
          <div class="skill-hero__head">
            <div class="skill-hero__badge" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9.75 3.104v5.714a2.25 2.25 0 01-.659 1.591L5 14.5M9.75 3.104c-.251.023-.501.05-.75.082m.75-.082a24.301 24.301 0 014.5 0m0 0v5.714c0 .597.237 1.17.659 1.591L19.8 15.3M14.25 3.104c.251.023.501.05.75.082M19.8 15.3l-1.57.393A9.065 9.065 0 0112 15a9.065 9.065 0 00-6.23.693L5 14.5m14.8.8l1.402 1.402c1.232 1.232.65 3.318-1.067 3.611A48.309 48.309 0 0112 21c-2.773 0-5.491-.235-8.135-.687-1.718-.293-2.3-2.379-1.067-3.61L5 14.5" />
              </svg>
            </div>
            <div class="skill-hero__titles">
              <h1 class="skill-hero__title">{{ skill.name }}</h1>
              <div
                class="skill-hero__desc markdown-body"
                v-html="renderedDescription"
              />
            </div>
          </div>
          <div v-if="hasBadges" class="skill-hero__tags">
            <span v-for="t in skill.tagNames" :key="t" class="tag-chip tag-chip--sm">{{ t }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--muted">内部</span>
            <span class="tag-chip tag-chip--sm tag-chip--asset">{{ assetLevelLabel(skill.assetLevel) }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--status">{{ lifecycleLabel(skill.lifecycleStatus) }}</span>
            <span class="tag-chip tag-chip--sm">{{ skillCategoryLabel(skill.skillCategory) }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--priority">{{ buildPriorityLabel(skill.buildPriority) }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--risk">{{ riskLabel(skill.riskLevel) }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--validation">{{ validationStatusLabel(skill.templateValidationStatus) }}</span>
          </div>
        </div>
      </header>

      <div class="skill-body">
        <div class="skill-body__grid">
          <main class="skill-main">
            <section v-if="hasStructuredGuide" class="skill-section skill-animate" style="--delay: 0.05s">
              <div class="skill-section__head">
                <h2>资产化说明</h2>
                <p>适用边界、输入输出与质量标准</p>
              </div>
              <div class="guide-grid">
                <article v-if="skill.applicableScenarios" class="guide-card guide-card--ok">
                  <div class="guide-card__icon" aria-hidden="true">✓</div>
                  <div>
                    <h3>适用场景</h3>
                    <p>{{ skill.applicableScenarios }}</p>
                  </div>
                </article>
                <article v-if="skill.nonApplicableScenarios" class="guide-card guide-card--warn">
                  <div class="guide-card__icon" aria-hidden="true">✕</div>
                  <div>
                    <h3>不适用场景</h3>
                    <p>{{ skill.nonApplicableScenarios }}</p>
                  </div>
                </article>
                <article v-if="skill.inputRequirements" class="guide-card guide-card--in">
                  <div class="guide-card__icon" aria-hidden="true">→</div>
                  <div>
                    <h3>输入要求</h3>
                    <p>{{ skill.inputRequirements }}</p>
                  </div>
                </article>
                <article v-if="skill.outputFormat" class="guide-card guide-card--out">
                  <div class="guide-card__icon" aria-hidden="true">←</div>
                  <div>
                    <h3>输出格式</h3>
                    <p>{{ skill.outputFormat }}</p>
                  </div>
                </article>
                <article v-if="skill.qualityStandard" class="guide-card guide-card--quality">
                  <div class="guide-card__icon" aria-hidden="true">◎</div>
                  <div>
                    <h3>质量标准</h3>
                    <p>{{ skill.qualityStandard }}</p>
                  </div>
                </article>
                <article v-if="skill.referenceMaterials" class="guide-card guide-card--ref">
                  <div class="guide-card__icon" aria-hidden="true">◇</div>
                  <div>
                    <h3>参考资料</h3>
                    <p>{{ skill.referenceMaterials }}</p>
                  </div>
                </article>
              </div>
              <article v-if="skill.executionSteps" class="guide-card guide-card--wide guide-card--steps">
                <div class="guide-card__icon" aria-hidden="true">①</div>
                <div>
                  <h3>执行步骤</h3>
                  <p>{{ skill.executionSteps }}</p>
                </div>
              </article>
              <article v-if="skill.validationMethod" class="guide-card guide-card--wide guide-card--validate">
                <div class="guide-card__icon" aria-hidden="true">⚡</div>
                <div>
                  <h3>验证方式</h3>
                  <p>{{ skill.validationMethod }}</p>
                </div>
              </article>
              <article v-if="skill.sourceRepositoryUrl || skill.skillDirectory || skill.templateValidationNotes" class="guide-card guide-card--wide guide-card--repo">
                <div class="guide-card__icon" aria-hidden="true">⌂</div>
                <div>
                  <h3>仓库与校验</h3>
                  <p>{{ repositoryInfo }}</p>
                </div>
              </article>
              <article v-if="skill.reviewNotes" class="guide-card guide-card--wide guide-card--note">
                <div class="guide-card__icon" aria-hidden="true">✎</div>
                <div>
                  <h3>评审备注</h3>
                  <p>{{ skill.reviewNotes }}</p>
                </div>
              </article>
            </section>

            <section v-if="skill.contentMd" class="skill-section skill-animate" style="--delay: 0.12s">
              <div class="skill-section__head skill-section__head--row">
                <div>
                  <h2>文档</h2>
                  <p>完整 Skill 说明与使用指引</p>
                </div>
                <button type="button" class="skill-copy-btn" title="复制文档" @click="copy(skill.contentMd)">
                  <svg viewBox="64 64 896 896" class="w-4 h-4" fill="currentColor" aria-hidden="true">
                    <path d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z" />
                  </svg>
                  复制
                </button>
              </div>
              <div class="doc-panel">
                <div class="markdown-body doc-panel__body" v-html="renderedMd" />
              </div>
            </section>
          </main>

          <aside class="skill-aside">
            <div class="aside-card aside-card--install skill-animate" style="--delay: 0.08s">
              <h3>快捷安装</h3>
              <p>复制到终端即可获取 Skill</p>
              <div class="terminal">
                <div class="terminal__bar">
                  <span class="terminal__dot terminal__dot--red" />
                  <span class="terminal__dot terminal__dot--yellow" />
                  <span class="terminal__dot terminal__dot--green" />
                  <span class="terminal__label">bash</span>
                </div>
                <div class="terminal__body">
                  <code ref="cloneCommandRef" class="terminal__code" :title="skill.cloneCommand">{{ skill.cloneCommand }}</code>
                  <button type="button" class="terminal__copy" title="复制命令" @click="copy(skill.cloneCommand, cloneCommandRef)">
                    <svg viewBox="64 64 896 896" class="w-4 h-4" fill="currentColor" aria-hidden="true">
                      <path d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h496v688c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32zM704 192H192c-17.7 0-32 14.3-32 32v530.7c0 8.5 3.4 16.6 9.4 22.6l173.3 173.3c2.2 2.2 4.7 4 7.4 5.5v1.9h4.2c3.5 1.3 7.2 2 11 2H704c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zM350 856.2L263.9 770H350v86.2zM664 888H414V746c0-22.1-17.9-40-40-40H232V264h432v624z" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <div class="aside-card skill-animate" style="--delay: 0.14s">
              <h3>资产信息</h3>
              <dl class="meta-list">
                <div class="meta-list__item">
                  <dt>维护人</dt>
                  <dd>{{ skill.maintainer || skill.uploaderName || '—' }}</dd>
                </div>
                <div class="meta-list__item">
                  <dt>团队/业务线</dt>
                  <dd>{{ skill.teamName || '未分配' }}</dd>
                </div>
                <div class="meta-list__item">
                  <dt>版本</dt>
                  <dd><span class="meta-version">{{ skill.version || '1.0.0' }}</span></dd>
                </div>
                <div class="meta-list__item meta-list__item--highlight">
                  <dt>使用次数</dt>
                  <dd><span class="meta-usage">{{ usageCount }}</span></dd>
                </div>
              </dl>
            </div>

            <div v-if="skill.uploaderName" class="aside-footer skill-animate" style="--delay: 0.18s">
              <span>上传者</span>
              <strong>{{ skill.uploaderName }}</strong>
            </div>
          </aside>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="skill-state">
        <div v-if="loading" class="skill-skeleton">
          <div class="skill-skeleton__hero" />
          <div class="skill-skeleton__body">
            <div class="skill-skeleton__main" />
            <div class="skill-skeleton__aside" />
          </div>
        </div>
        <el-alert v-else type="error" :title="error || '未找到'" show-icon />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { copyText, selectElementText } from '../utils/copyText'

const route = useRoute()
const skill = ref(null)
const loading = ref(true)
const error = ref('')
const usageCount = ref(0)
const cloneCommandRef = ref(null)

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

const heroVariantMap = {
  REQUIREMENT_ANALYSIS: 'indigo',
  ARCHITECTURE_DESIGN: 'purple',
  CODING_IMPLEMENTATION: 'blue',
  TESTING_VALIDATION: 'teal',
  CODE_REVIEW: 'amber',
  OPS_TROUBLESHOOTING: 'rose',
  DOCUMENTATION_KNOWLEDGE: 'green',
}

const hasStructuredGuide = computed(() => {
  const s = skill.value
  return Boolean(s?.applicableScenarios || s?.nonApplicableScenarios || s?.inputRequirements || s?.executionSteps || s?.outputFormat || s?.validationMethod || s?.qualityStandard || s?.referenceMaterials || s?.sourceRepositoryUrl || s?.skillDirectory || s?.templateValidationNotes || s?.reviewNotes)
})

const hasBadges = computed(() => {
  const s = skill.value
  return Boolean((s?.tagNames || []).length || s?.assetLevel || s?.lifecycleStatus || s?.riskLevel)
})

const heroVariant = computed(() => heroVariantMap[skill.value?.skillCategory] || 'blue')

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

async function copy(text, fallbackEl = null) {
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  if (await copyText(text)) {
    ElMessage.success('已复制到剪贴板')
    return
  }
  if (fallbackEl && selectElementText(fallbackEl)) {
    ElMessage.warning('无法自动复制，内容已选中，请按 Ctrl+C')
    return
  }
  ElMessage.error('复制失败，请手动选中后复制')
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
    if (skill.value?.id) {
      try {
        const summary = await api.get(`/skills/${skill.value.id}/governance-summary`)
        usageCount.value = summary?.usageCount || 0
      } catch {
        usageCount.value = 0
      }
    }
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.skill-detail-page {
  min-height: 100%;
}

/* Hero */
.skill-hero {
  position: relative;
  overflow: hidden;
  padding: 2rem 1.5rem 2.25rem;
  background-size: 28px 28px, 28px 28px, 100% 100%;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

.skill-hero::before,
.skill-hero::after {
  content: '';
  position: absolute;
  pointer-events: none;
  border-radius: 50%;
}

.skill-hero--blue {
  background-image:
    linear-gradient(rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(165deg, #f0f7ff 0%, #e8f4f8 45%, #f8fafc 100%);
}
.skill-hero--blue::before { top: -30%; right: -10%; width: 55%; height: 110%; background: radial-gradient(ellipse, rgba(46, 201, 215, 0.1) 0%, transparent 70%); }
.skill-hero--blue::after { bottom: -40%; left: -8%; width: 40%; height: 90%; background: radial-gradient(ellipse, rgba(37, 99, 235, 0.07) 0%, transparent 65%); }

.skill-hero--indigo {
  background-image:
    linear-gradient(rgba(79, 70, 229, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 70, 229, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #eef2ff 0%, #e0e7ff 45%, #f8fafc 100%);
}
.skill-hero--indigo::before { top: -30%; right: -10%; width: 55%; height: 110%; background: radial-gradient(ellipse, rgba(129, 140, 248, 0.12) 0%, transparent 70%); }

.skill-hero--purple {
  background-image:
    linear-gradient(rgba(124, 58, 237, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(124, 58, 237, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #f5f3ff 0%, #ede9fe 45%, #fafafa 100%);
}
.skill-hero--purple::before { top: -30%; right: -10%; width: 55%; height: 110%; background: radial-gradient(ellipse, rgba(167, 139, 250, 0.12) 0%, transparent 70%); }

.skill-hero--green {
  background-image:
    linear-gradient(rgba(5, 150, 105, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(5, 150, 105, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #f0fdf4 0%, #ecfdf5 45%, #f8fafc 100%);
}
.skill-hero--green::before { top: -30%; right: -10%; width: 55%; height: 110%; background: radial-gradient(ellipse, rgba(52, 211, 153, 0.12) 0%, transparent 70%); }

.skill-hero--teal {
  background-image:
    linear-gradient(rgba(13, 148, 136, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(13, 148, 136, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #f0fdfa 0%, #ccfbf1 45%, #f8fafc 100%);
}
.skill-hero--teal::before { top: -30%; right: -10%; width: 55%; height: 110%; background: radial-gradient(ellipse, rgba(45, 212, 191, 0.12) 0%, transparent 70%); }

.skill-hero--amber {
  background-image:
    linear-gradient(rgba(217, 119, 6, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(217, 119, 6, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #fffbeb 0%, #fef3c7 45%, #fafafa 100%);
}
.skill-hero--amber::before { top: -30%; right: -10%; width: 55%; height: 110%; background: radial-gradient(ellipse, rgba(251, 191, 36, 0.14) 0%, transparent 70%); }

.skill-hero--rose {
  background-image:
    linear-gradient(rgba(225, 29, 72, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(225, 29, 72, 0.035) 1px, transparent 1px),
    linear-gradient(165deg, #fff1f2 0%, #ffe4e6 45%, #fafafa 100%);
}
.skill-hero--rose::before { top: -30%; right: -10%; width: 55%; height: 110%; background: radial-gradient(ellipse, rgba(251, 113, 133, 0.12) 0%, transparent 70%); }

.skill-hero__inner {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto;
}

.skill-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 1.25rem;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  text-decoration: none;
  transition: color 0.2s;
}

.skill-back svg {
  width: 16px;
  height: 16px;
}

.skill-back:hover {
  color: #2563eb;
}

.skill-hero__head {
  display: flex;
  gap: 1.25rem;
  align-items: flex-start;
}

.skill-hero__badge {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

.skill-hero__badge svg {
  width: 28px;
  height: 28px;
}

.skill-hero__title {
  margin: 0 0 0.5rem;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.025em;
  color: #0f172a;
  line-height: 1.2;
}

.skill-hero__desc {
  margin: 0;
  font-size: 1rem;
  line-height: 1.7;
  color: #64748b;
}

.skill-hero__desc :deep(p) {
  margin: 0;
}

.skill-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 1.25rem;
}

/* Body layout */
.skill-body {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem 1.5rem 3rem;
}

.skill-body__grid {
  display: flex;
  gap: 2rem;
  align-items: flex-start;
}

.skill-main {
  flex: 1;
  min-width: 0;
}

.skill-aside {
  width: 340px;
  flex-shrink: 0;
  position: sticky;
  top: 1.5rem;
}

/* Sections */
.skill-section {
  margin-bottom: 2rem;
}

.skill-section__head {
  margin-bottom: 1.25rem;
}

.skill-section__head--row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.skill-section__head h2 {
  margin: 0 0 4px;
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
}

.skill-section__head p {
  margin: 0;
  font-size: 13px;
  color: #94a3b8;
}

.skill-copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.skill-copy-btn:hover {
  color: #2563eb;
  border-color: #bfdbfe;
  background: #eff6ff;
}

/* Guide cards */
.guide-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.guide-card {
  display: flex;
  gap: 12px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.guide-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
}

.guide-card--wide {
  margin-bottom: 12px;
}

.guide-card__icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
}

.guide-card--ok { border-left: 3px solid #10b981; }
.guide-card--ok .guide-card__icon { background: #ecfdf5; color: #059669; }

.guide-card--warn { border-left: 3px solid #f59e0b; }
.guide-card--warn .guide-card__icon { background: #fffbeb; color: #d97706; }

.guide-card--in { border-left: 3px solid #3b82f6; }
.guide-card--in .guide-card__icon { background: #eff6ff; color: #2563eb; }

.guide-card--out { border-left: 3px solid #8b5cf6; }
.guide-card--out .guide-card__icon { background: #f5f3ff; color: #7c3aed; }

.guide-card--quality { border-left: 3px solid #06b6d4; }
.guide-card--quality .guide-card__icon { background: #ecfeff; color: #0891b2; }

.guide-card--ref { border-left: 3px solid #64748b; }
.guide-card--ref .guide-card__icon { background: #f1f5f9; color: #475569; }

.guide-card--steps { border-left: 3px solid #2563eb; }
.guide-card--steps .guide-card__icon { background: #dbeafe; color: #1d4ed8; }

.guide-card--validate { border-left: 3px solid #f97316; }
.guide-card--validate .guide-card__icon { background: #fff7ed; color: #ea580c; }

.guide-card--repo { border-left: 3px solid #6366f1; }
.guide-card--repo .guide-card__icon { background: #eef2ff; color: #4f46e5; }

.guide-card--note { border-left: 3px solid #94a3b8; }
.guide-card--note .guide-card__icon { background: #f8fafc; color: #64748b; }

.guide-card h3 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.guide-card p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  white-space: pre-line;
}

/* Doc panel */
.doc-panel {
  border: 1px solid #e8edf3;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.doc-panel__body {
  padding: 1.5rem;
}

/* Sidebar */
.aside-card {
  padding: 1.25rem;
  margin-bottom: 1rem;
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}

.aside-card--install {
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
}

.aside-card h3 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.aside-card > p {
  margin: 0 0 1rem;
  font-size: 12px;
  color: #94a3b8;
}

/* Terminal */
.terminal {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #1e293b;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.18);
}

.terminal__bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: #1e293b;
}

.terminal__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.terminal__dot--red { background: #ef4444; }
.terminal__dot--yellow { background: #f59e0b; }
.terminal__dot--green { background: #22c55e; }

.terminal__label {
  margin-left: auto;
  font-size: 11px;
  font-family: ui-monospace, monospace;
  color: #64748b;
}

.terminal__body {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 14px 12px;
  background: #0f172a;
}

.terminal__code {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.6;
  font-family: ui-monospace, 'Cascadia Code', 'SF Mono', monospace;
  color: #a5f3fc;
  word-break: break-all;
}

.terminal__copy {
  flex-shrink: 0;
  padding: 6px;
  color: #64748b;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.terminal__copy:hover {
  color: #e2e8f0;
  background: rgba(255, 255, 255, 0.08);
}

/* Meta list */
.meta-list {
  margin: 0;
}

.meta-list__item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.meta-list__item:last-child:not(.meta-list__item--highlight) {
  border-bottom: none;
  padding-bottom: 0;
}

.meta-list__item:first-child {
  padding-top: 0;
}

.meta-list__item dt {
  flex-shrink: 0;
  font-size: 12px;
  color: #94a3b8;
}

.meta-list__item dd {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  text-align: right;
}

.meta-list__item--highlight {
  margin-top: 4px;
  padding: 12px;
  align-items: center;
  background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%);
  border-radius: 10px;
  border-bottom: none;
}

.meta-list__item--highlight dt {
  color: #3b82f6;
  font-weight: 600;
}

.meta-version {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  padding: 2px 8px;
  background: #f1f5f9;
  border-radius: 4px;
}

.meta-usage {
  font-size: 1.25rem;
  font-weight: 700;
  color: #2563eb;
}

.meta-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
}

.meta-badge--passed { background: #ecfccb; color: #365314; }
.meta-badge--failed { background: #fee2e2; color: #7f1d1d; }
.meta-badge--pending { background: #f1f5f9; color: #64748b; }

.aside-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  font-size: 13px;
  color: #94a3b8;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px dashed #e2e8f0;
}

.aside-footer strong {
  color: #475569;
}

/* Animation */
.skill-animate {
  animation: skillFadeUp 0.5s ease both;
  animation-delay: var(--delay, 0s);
}

@keyframes skillFadeUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Loading */
.skill-state {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

.skill-skeleton__hero {
  height: 200px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skill-skeleton__body {
  display: flex;
  gap: 2rem;
  margin-top: 2rem;
}

.skill-skeleton__main {
  flex: 1;
  height: 400px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skill-skeleton__aside {
  width: 340px;
  height: 320px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Markdown */
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

@media (max-width: 1024px) {
  .skill-body__grid {
    flex-direction: column;
  }

  .skill-aside {
    width: 100%;
    position: static;
  }

  .guide-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .skill-hero__head {
    flex-direction: column;
  }

  .skill-hero__title {
    font-size: 1.5rem;
  }

  .skill-skeleton__body {
    flex-direction: column;
  }

  .skill-skeleton__aside {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skill-animate {
    animation: none;
  }
}
</style>
