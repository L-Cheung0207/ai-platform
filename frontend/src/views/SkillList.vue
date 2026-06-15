<template>
  <div class="skill-list-page">
    <PageHero
      variant="green"
      title="AI Skill 资产库"
      subtitle="按层级、状态和场景沉淀可复用的组织级 AI 能力"
    >
      <div class="skills-hero-toolbar">
        <div class="global-search skills-hero-search">
          <el-input
            v-model="keyword"
            placeholder="搜索 Skill（最多 200 字）"
            maxlength="200"
            show-word-limit
            clearable
            size="large"
            @keyup.enter="search"
          />
          <el-button type="primary" size="large" @click="search">搜索</el-button>
        </div>
        <el-button
          type="primary"
          size="large"
          :icon="Upload"
          class="skills-upload-btn"
          @click="openContributorWorkspace"
        >
          上传 Skill
        </el-button>
      </div>
    </PageHero>

    <div class="max-w-[1280px] mx-auto px-6 py-8">
      <ContributorSkillWorkspace v-model="contributorWorkspaceVisible" @changed="refreshSkills" />
      <LoginDialog
        v-model="loginDialogVisible"
        @success="onContributorLoginSuccess"
      />

      <!-- 标签筛选 -->
      <div v-if="tagList.length > 0" class="skills-tag-filter mb-8">
        <button
          type="button"
          :class="[
            'px-4 py-2 rounded-full text-[14px] font-medium transition-all duration-200 whitespace-nowrap',
            selectedTagIds.length === 0
              ? 'bg-primary text-white'
              : 'bg-[#f3f4f6] text-[#4b5563] hover:bg-[#e5e7eb]'
          ]"
          @click="clearTagFilter"
        >
          全部标签
        </button>
        <button
          v-for="t in tagList"
          :key="t.id"
          type="button"
          :class="[
            'px-4 py-2 rounded-full text-[14px] font-medium transition-all duration-200 whitespace-nowrap',
            selectedTagIds.includes(t.id)
              ? 'bg-primary text-white'
              : 'bg-[#f3f4f6] text-[#4b5563] hover:bg-[#e5e7eb]'
          ]"
          @click="toggleTag(t.id)"
        >
          {{ t.name }}
        </button>
      </div>

      <!-- 网格卡片 -->
      <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4" aria-busy="true">
        <div v-for="i in 6" :key="i" class="rounded-xl border border-[#e5e7eb] bg-white p-5 animate-pulse">
          <div class="h-5 bg-[#e5e7eb] rounded w-2/3 mb-3" />
          <div class="flex gap-2 mb-3 flex-wrap">
            <div class="h-6 w-16 bg-[#f3f4f6] rounded" />
            <div class="h-6 w-20 bg-[#f3f4f6] rounded" />
          </div>
          <div class="h-4 bg-[#f3f4f6] rounded w-full" />
          <div class="h-4 bg-[#f3f4f6] rounded w-[80%] mt-2" />
        </div>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <router-link
          v-for="s in displayItems"
          :key="s.id"
          :to="'/skills/' + s.id"
          class="block p-5 bg-white border border-[#e5e7eb] rounded-xl hover:border-primary hover:shadow-[0_4px_12px_rgba(37,99,235,0.1)] transition-all duration-200"
        >
          <div class="font-semibold text-[16px] text-[#1f2937] mb-2 truncate">{{ s.name }}</div>
          <div class="flex flex-wrap gap-2 mb-2">
            <span v-for="t in (s.tagNames || []).slice(0, 4)" :key="t" class="tag-chip tag-chip--sm">{{ t }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--asset">{{ assetLevelLabel(s.assetLevel) }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--status">{{ lifecycleLabel(s.lifecycleStatus) }}</span>
            <span class="tag-chip tag-chip--sm">{{ skillCategoryLabel(s.skillCategory) }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--priority">{{ buildPriorityLabel(s.buildPriority) }}</span>
            <span class="tag-chip tag-chip--sm tag-chip--validation">{{ validationStatusLabel(s.templateValidationStatus) }}</span>
          </div>
          <p class="text-[14px] text-[#6b7280] leading-relaxed line-clamp-2">{{ s.description || '暂无描述' }}</p>
          <div class="mt-4 flex items-center justify-between text-[12px] text-[#9ca3af]">
            <span>{{ s.teamName || '未分配团队' }}</span>
            <span>{{ s.nextReviewAt ? '复审 ' + s.nextReviewAt : riskLabel(s.riskLevel) }}</span>
          </div>
        </router-link>
      </div>

      <div class="flex justify-center mt-8">
        <el-pagination
          v-if="total > 0 && !loading"
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="onPageChange"
        />
      </div>

      <el-alert v-if="error" type="error" :title="error" show-icon class="mt-4" />
      <p v-if="!loading && !error && displayItems.length === 0" class="text-center text-[#6b7280] py-12">暂无 Skill</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { Upload } from '@element-plus/icons-vue'
import PageHero from '../components/PageHero.vue'
import LoginDialog from '../components/auth/LoginDialog.vue'
import ContributorSkillWorkspace from '../components/skills/ContributorSkillWorkspace.vue'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref(route.query.keyword || '')
const loading = ref(false)
const error = ref('')
const contributorWorkspaceVisible = ref(false)
const loginDialogVisible = shallowRef(false)
const tagList = ref([])
const selectedTagIds = ref([])

const assetLevelOptions = [
  { value: 'TEAM', label: '团队级' },
  { value: 'COMPANY', label: '公司级' },
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

const displayItems = computed(() => items.value)

async function loadTags() {
  try {
    const list = await api.get('/tags', { params: { forEntity: 'skills' } })
    tagList.value = list || []
  } catch {
    tagList.value = []
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (selectedTagIds.value.length > 0) params.tags = selectedTagIds.value.join(',')

    const data = await api.get('/skills', { params })
    items.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function optionLabel(options, value, fallback = '—') {
  return options.find((item) => item.value === value)?.label || fallback
}

function assetLevelLabel(value) {
  return optionLabel(assetLevelOptions, value, '团队级')
}

function lifecycleLabel(value) {
  return value === 'APPROVED' ? '已入库' : '未入库'
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

function search() {
  page.value = 1
  load()
}

function toggleTag(id) {
  const idx = selectedTagIds.value.indexOf(id)
  if (idx >= 0) {
    selectedTagIds.value = selectedTagIds.value.filter((x) => x !== id)
  } else {
    selectedTagIds.value = [...selectedTagIds.value, id]
  }
  page.value = 1
  load()
}

function clearTagFilter() {
  selectedTagIds.value = []
  page.value = 1
  load()
}

function refreshSkills() {
  loadTags()
  load()
}

function openContributorWorkspace() {
  if (!auth.isAuthenticated) {
    loginDialogVisible.value = true
    return
  }
  contributorWorkspaceVisible.value = true
}

function onContributorLoginSuccess() {
  contributorWorkspaceVisible.value = true
}

watch(() => route.query.keyword, (v) => {
  if (v !== undefined) keyword.value = v || ''
})

function onPageChange() {
  if (typeFilter.value === 'all') return // 全部模式为前端分页，displayItems 自动更新
  load() // 内部/外部模式需重新请求
}

onMounted(() => {
  loadTags()
  load()
})
</script>

<style scoped>
.skill-list-page :deep(.page-hero-inner) {
  max-width: 780px;
}

.skills-hero-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 0.5rem;
}

.skills-hero-search {
  flex: 1;
  margin: 0;
}

.skills-upload-btn {
  --el-button-bg-color: #10b981;
  --el-button-border-color: #10b981;
  --el-button-hover-bg-color: #059669;
  --el-button-hover-border-color: #059669;
  --el-button-active-bg-color: #047857;
  --el-button-active-border-color: #047857;
  flex-shrink: 0;
  min-width: 124px;
  box-shadow: 0 10px 20px rgba(16, 185, 129, 0.22);
  font-weight: 700;
}

.skills-upload-btn:hover {
  box-shadow: 0 12px 24px rgba(5, 150, 105, 0.28);
}

.skills-tag-filter {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  overflow-x: auto;
}

.skills-tag-filter::-webkit-scrollbar {
  height: 4px;
}

.skills-tag-filter::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}

@media (max-width: 640px) {
  .skills-hero-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .skills-upload-btn {
    width: 100%;
  }
}
</style>
