<template>
  <div class="admin-layout min-h-screen flex flex-col bg-[var(--bg-body)]">
    <!-- 顶部导航栏 -->
    <header
      class="sticky top-0 z-30 h-16 shrink-0 bg-[var(--bg-card)] border-b border-[var(--border-color)] shadow-[var(--shadow-card)]"
      role="banner"
    >
      <div class="h-full px-6 flex items-center justify-between">
        <router-link
          to="/"
          class="flex items-center gap-2.5 text-lg font-bold text-[var(--text-primary)] hover:text-primary transition-colors duration-200 cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded-lg -m-1 p-1 min-h-[44px] min-w-[44px] items-center justify-center sm:justify-start"
          aria-label="返回首页"
        >
          <Icons name="logo" :size="26" class="text-primary shrink-0" />
          <span class="hidden sm:inline">AI Skill 资产平台</span>
        </router-link>
        <div class="flex items-center gap-2">
          <router-link
            to="/"
            class="text-sm text-[var(--text-secondary)] hover:text-primary transition-colors duration-200 cursor-pointer px-3 py-2 rounded-lg min-h-[44px] min-w-[44px] flex items-center justify-center focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
          >
            返回首页
          </router-link>
          <span class="text-sm text-[var(--text-muted)] px-2 py-1 rounded" aria-hidden="true">
            {{ auth.user?.username }}
          </span>
          <el-button
            size="small"
            class="cursor-pointer"
            @click="auth.logout(); $router.push('/')"
          >
            退出
          </el-button>
        </div>
      </div>
    </header>

    <div class="flex flex-1 min-h-0">
      <!-- 侧边栏 -->
      <aside
        class="w-56 shrink-0 bg-[var(--bg-card)] border-r border-[var(--border-color)] flex flex-col overflow-hidden"
        role="navigation"
        aria-label="管理后台菜单"
      >
        <nav class="py-4 px-3 overflow-y-auto">
          <!-- 内容管理 -->
          <div class="mb-4">
            <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
              内容
            </p>
            <ul class="space-y-0.5">
              <li v-for="item in menuContent" :key="item.path">
                <router-link
                  :to="item.path"
                  class="admin-nav-item"
                  :class="{ 'admin-nav-item--active': isActive(item.path) }"
                >
                  <component :is="item.icon" class="admin-nav-item__icon" aria-hidden="true" />
                  <span>{{ item.label }}</span>
                </router-link>
              </li>
            </ul>
          </div>
          <!-- 工具与配置 -->
          <div class="mb-4">
            <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
              工具与配置
            </p>
            <ul class="space-y-0.5">
              <li v-for="item in menuTools" :key="item.path">
                <router-link
                  :to="item.path"
                  class="admin-nav-item"
                  :class="{ 'admin-nav-item--active': isActive(item.path) }"
                >
                  <component :is="item.icon" class="admin-nav-item__icon" aria-hidden="true" />
                  <span>{{ item.label }}</span>
                </router-link>
              </li>
            </ul>
          </div>
          <!-- 数据 -->
          <div>
            <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
              数据
            </p>
            <ul class="space-y-0.5">
              <li v-for="item in menuData" :key="item.path">
                <router-link
                  :to="item.path"
                  class="admin-nav-item"
                  :class="{ 'admin-nav-item--active': isActive(item.path) }"
                >
                  <component :is="item.icon" class="admin-nav-item__icon" aria-hidden="true" />
                  <span>{{ item.label }}</span>
                </router-link>
              </li>
            </ul>
          </div>
        </nav>
      </aside>

      <!-- 主内容区 -->
      <main class="flex-1 p-6 overflow-auto min-w-0">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import Icons from '../../components/Icons.vue'

const route = useRoute()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

// Lucide-style 24x24 stroke icons (stroke-width 2)
const IconDocument = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
  h('polyline', { points: '14 2 14 8 20 8' }),
  h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
  h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
  h('line', { x1: '10', y1: '9', x2: '8', y2: '9' }),
])
const IconNewspaper = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M4 22h16a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16a2 2 0 0 1-2 2Zm0 0a2 2 0 0 1-2-2v-9c0-1.1.9-2 2-2h2' }),
  h('path', { d: 'M18 14h-8' }),
  h('path', { d: 'M15 18h-5' }),
  h('path', { d: 'M10 6h8v4h-8V6Z' }),
])
const IconWrench = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z' }),
])
const IconPlug = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M12 22v-5' }),
  h('path', { d: 'M9 8V2' }),
  h('path', { d: 'M15 8V2' }),
  h('path', { d: 'M18 8v5a4 4 0 0 1-4 4h-4a4 4 0 0 1-4-4V8Z' }),
])
const IconFileCode = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }),
  h('polyline', { points: '14 2 14 8 20 8' }),
  h('path', { d: 'M10 13l-2 2 2 2' }),
  h('path', { d: 'M14 17l2-2-2-2' }),
])
const IconBookOpen = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z' }),
  h('path', { d: 'M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z' }),
])
const IconExternalLink = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6' }),
  h('polyline', { points: '15 3 21 3 21 9' }),
  h('line', { x1: '10', y1: '14', x2: '21', y2: '3' }),
])
const IconBarChart = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('line', { x1: '12', y1: '20', x2: '12', y2: '10' }),
  h('line', { x1: '18', y1: '20', x2: '18', y2: '4' }),
  h('line', { x1: '6', y1: '20', x2: '6', y2: '16' }),
])
const IconUsers = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }),
  h('circle', { cx: '9', cy: '7', r: '4' }),
  h('path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87' }),
  h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' }),
])

const menuContent = [
  { path: '/admin/articles', label: 'AI知识库管理', icon: IconDocument },
  { path: '/admin/news', label: '资讯管理', icon: IconNewspaper },
]
const menuTools = [
  { path: '/admin/ai-tools', label: 'AI 工具管理', icon: IconWrench },
  { path: '/admin/mcp', label: 'MCP 管理', icon: IconPlug },
  { path: '/admin/rules', label: 'Rule 管理', icon: IconFileCode },
  { path: '/admin/skills', label: 'Skill 资产', icon: IconBookOpen },
  { path: '/admin/external-skills', label: '外部 Skill', icon: IconExternalLink },
]
const menuData = [
  { path: '/admin/skill-operations', label: 'Skill 运营', icon: IconBarChart },
  { path: '/admin/users', label: '用户管理', icon: IconUsers },
  { path: '/admin/llm-leaderboard', label: 'LLM 排行榜', icon: IconBarChart },
]
</script>

<style scoped>
@reference "../../assets/styles.css";

.admin-nav-item {
  @apply flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-[var(--text-primary)] transition-colors duration-200 cursor-pointer min-h-[44px] focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2;
}
.admin-nav-item:hover {
  @apply bg-gray-100 text-primary;
}
.admin-nav-item--active {
  @apply bg-primary/10 text-primary;
}
.admin-nav-item--active:hover {
  @apply bg-primary/15 text-primary;
}
.admin-nav-item__icon {
  @apply shrink-0 text-[var(--text-secondary)];
}
.admin-nav-item--active .admin-nav-item__icon {
  @apply text-primary;
}

@media (prefers-reduced-motion: reduce) {
  .admin-nav-item,
  header a {
    transition-duration: 0.01ms;
  }
}
</style>
