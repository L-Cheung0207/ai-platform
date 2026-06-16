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
          <span class="hidden lg:inline whitespace-nowrap">AI原生实践中心（AI Native Hub）</span>
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

    <div class="admin-layout__body flex flex-1 min-h-0">
      <!-- 侧边栏 -->
      <aside
        class="admin-layout__sidebar w-56 shrink-0 bg-[var(--bg-card)] border-r border-[var(--border-color)] flex flex-col overflow-hidden"
        role="navigation"
        aria-label="管理后台菜单"
      >
        <nav class="admin-layout__nav py-4 px-3 overflow-y-auto">
          <div
            v-for="(group, index) in menuGroups"
            :key="group.title"
            :class="index < menuGroups.length - 1 ? 'mb-4' : ''"
          >
            <p class="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
              {{ group.title }}
            </p>
            <ul class="space-y-0.5">
              <li v-for="item in group.items" :key="item.path">
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
      <main class="admin-layout__main flex-1 p-6 overflow-auto min-w-0">
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

const activePath = computed(() => {
  const candidates = menuGroups
    .flatMap((group) => group.items)
    .map((item) => item.path)
    .filter((path) => route.path === path || route.path.startsWith(path + '/'))
    .sort((a, b) => b.length - a.length)

  return candidates[0] || ''
})

function isActive(path) {
  return activePath.value === path
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
const IconBarChart = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('line', { x1: '12', y1: '20', x2: '12', y2: '10' }),
  h('line', { x1: '18', y1: '20', x2: '18', y2: '4' }),
  h('line', { x1: '6', y1: '20', x2: '6', y2: '16' }),
])
const IconTrophy = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M6 9H4.5a2.5 2.5 0 0 1 0-5H6' }),
  h('path', { d: 'M18 9h1.5a2.5 2.5 0 0 0 0-5H18' }),
  h('path', { d: 'M4 22h16' }),
  h('path', { d: 'M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22' }),
  h('path', { d: 'M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22' }),
  h('path', { d: 'M18 2H6v7a6 6 0 0 0 12 0V2Z' }),
])
const IconUsers = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }),
  h('circle', { cx: '9', cy: '7', r: '4' }),
  h('path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87' }),
  h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' }),
])
const IconMessageSquare = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z' }),
])
const IconFolderTree = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', class: 'w-5 h-5 shrink-0' }, [
  h('path', { d: 'M3 3h6v6H3z' }),
  h('path', { d: 'M15 3h6v6h-6z' }),
  h('path', { d: 'M15 15h6v6h-6z' }),
  h('path', { d: 'M9 6h3a3 3 0 0 1 3 3v6' }),
])

const menuGroups = [
  {
    title: 'Skill 资产',
    items: [
      { path: '/admin/skills', label: 'Skill 资产', icon: IconBookOpen },
      { path: '/admin/rules', label: 'Rule 管理', icon: IconFileCode },
      { path: '/admin/skill-operations', label: 'Skill 运营', icon: IconBarChart },
    ],
  },
  {
    title: '内容与资讯',
    items: [
      { path: '/admin/articles', label: 'AI 知识库', icon: IconDocument },
      { path: '/admin/news', label: 'AI资讯', icon: IconNewspaper },
      { path: '/admin/github-trending', label: 'GitHub Trending', icon: IconTrophy },
      { path: '/admin/llm-leaderboard', label: 'LLM 排行榜', icon: IconTrophy },
      { path: '/admin/forum', label: '论坛管理', icon: IconMessageSquare },
      { path: '/admin/forum/categories', label: '论坛分类', icon: IconFolderTree },
    ],
  },
  {
    title: '系统',
    items: [
      { path: '/admin/modules', label: '模块管理', icon: IconBarChart },
      { path: '/admin/users', label: '用户管理', icon: IconUsers },
    ],
  },
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

@media (max-width: 768px) {
  .admin-layout__body {
    flex-direction: column;
    overflow: hidden;
  }

  .admin-layout__sidebar {
    width: 100%;
    max-height: 42vh;
    border-right: 0;
    border-bottom: 1px solid var(--border-color);
    overflow: auto;
  }

  .admin-layout__nav {
    overflow: auto;
  }

  .admin-layout__main {
    padding: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .admin-nav-item,
  header a {
    transition-duration: 0.01ms;
  }
}
</style>
