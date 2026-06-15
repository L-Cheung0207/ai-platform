<template>
  <header class="sticky top-0 z-50 h-16 bg-white border-b border-[#e5e7eb] shadow-[0_1px_2px_rgba(0,0,0,0.05)]">
    <div class="max-w-7xl mx-auto h-full px-6 flex items-center justify-between">
      <router-link
        to="/"
        class="shrink-0 flex items-center gap-2 text-lg font-bold text-[#1f2937] hover:text-primary transition-colors duration-200 cursor-pointer focus:outline-none rounded-lg -m-1 p-1"
        aria-label="返回首页"
      >
        <Icons name="logo" :size="28" class="text-primary" />
        <span class="hidden lg:inline whitespace-nowrap">AI原生实践中心（AI Native Hub）</span>
      </router-link>
      <nav class="flex min-w-0 items-center gap-1 overflow-x-auto">
        <router-link
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="shrink-0 whitespace-nowrap px-3 py-2 rounded-lg text-sm transition-colors duration-200 cursor-pointer min-h-[44px] min-w-[44px] flex items-center justify-center focus:outline-none"
          :class="isActive(link) ? 'text-primary font-medium' : 'text-[#6b7280] hover:text-primary'"
        >
          {{ link.label }}
        </router-link>
        <template v-if="auth.isAuthenticated">
          <router-link
            v-if="auth.user?.role === 'ADMIN'"
            to="/admin"
            class="shrink-0 whitespace-nowrap px-3 py-2 rounded-lg text-sm text-gray-600 hover:text-primary transition-colors duration-200 cursor-pointer"
          >
            管理后台
          </router-link>
          <span class="shrink-0 whitespace-nowrap px-2 text-xs text-gray-400">{{ auth.user?.username }}</span>
          <el-button size="small" @click="auth.logout(); $router.push('/')">退出</el-button>
        </template>
        <template v-else>
          <el-button v-if="usesSkillsLoginDialog" type="primary" size="small" @click="loginDialogVisible = true">登录/注册</el-button>
          <router-link v-else to="/login" class="shrink-0">
            <el-button type="primary" size="small">登录/注册</el-button>
          </router-link>
        </template>
      </nav>
    </div>
  </header>
  <LoginDialog
    v-if="usesSkillsLoginDialog"
    v-model="loginDialogVisible"
  />
</template>

<script setup>
import { computed, shallowRef } from 'vue'
import Icons from './Icons.vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginDialog from './auth/LoginDialog.vue'

const route = useRoute()
const auth = useAuthStore()
const loginDialogVisible = shallowRef(false)
const usesSkillsLoginDialog = computed(() => (
  route.path === '/skills' || route.path.startsWith('/skills/')
))

const navLinks = [
  { path: '/', label: '主页' },
  { path: '/skills', label: 'Skill资产库', activePaths: ['/skills'] },
  { path: '/articles', label: 'AI知识库' },
  { path: '/forum', label: '论坛' },
  { path: '/news', label: 'AI资讯' },
  { path: '/llm-leaderboard', label: 'LLM 排行榜' },
]

function isActive(link) {
  const path = typeof link === 'string' ? link : link.path
  const activePaths = link.activePaths
  if (path === '/') return route.path === '/'
  if (activePaths?.length) {
    return activePaths.some((p) => route.path === p || route.path.startsWith(p + '/'))
  }
  return route.path === path || route.path.startsWith(path + '/')
}
</script>
