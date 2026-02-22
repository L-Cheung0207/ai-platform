<template>
  <div class="min-h-[400px] flex items-center justify-center py-12 px-6">
    <el-card class="w-full max-w-md" shadow="hover">
      <template #header>
        <h2 class="text-xl font-semibold">登录</h2>
        <p class="text-sm text-gray-500 mt-1">登录后即可登记 Skill、上传 Rule</p>
      </template>
      <el-form @submit.prevent="submit">
        <el-form-item label="用户名" required>
          <el-input v-model="username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-alert v-if="error" type="error" :title="error" show-icon class="mb-4" />
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">
            {{ loading ? '登录中…' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="text-center text-sm text-gray-500">
        还没有账号？<router-link to="/register" class="text-primary font-medium">注册</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
