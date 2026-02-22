<template>
  <div class="admin-layout min-h-screen flex flex-col bg-gray-50">
    <header class="h-16 bg-white border-b border-gray-200 px-6 flex items-center justify-between">
      <router-link to="/" class="flex items-center gap-2 font-bold text-gray-800">
        <span>🔷</span>
        <span>AI 资源与学习平台</span>
      </router-link>
      <div class="flex items-center gap-4">
        <router-link to="/" class="text-sm text-gray-500 hover:text-primary">返回首页</router-link>
        <span class="text-sm text-gray-400">{{ auth.user?.username }}</span>
        <el-button size="small" @click="auth.logout(); $router.push('/')">退出</el-button>
      </div>
    </header>

    <div class="flex flex-1 min-h-0">
      <aside class="w-52 bg-white border-r border-gray-200 py-4 shrink-0">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/admin/articles">文章管理</el-menu-item>
          <el-menu-item index="/admin/news">资讯管理</el-menu-item>
          <el-menu-item index="/admin/ai-tools">AI 工具管理</el-menu-item>
          <el-menu-item index="/admin/mcp">MCP 管理</el-menu-item>
          <el-menu-item index="/admin/rules">Rule 管理</el-menu-item>
          <el-menu-item index="/admin/skills">Skill 管理</el-menu-item>
          <el-menu-item index="/admin/external-skills">外部 Skill</el-menu-item>
        </el-menu>
      </aside>
      <main class="flex-1 p-6 overflow-auto">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)
</script>
