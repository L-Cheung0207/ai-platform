<template>
  <div class="min-h-[400px] flex items-center justify-center py-12 px-6">
    <el-card class="w-full max-w-md" shadow="hover">
      <template #header>
        <h2 class="text-xl font-semibold">注册</h2>
        <p class="text-sm text-gray-500 mt-1">注册即可登录，无需审批</p>
      </template>
      <el-form @submit.prevent="submit">
        <el-form-item label="用户名" required>
          <el-input v-model="username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码（至少 6 位）" required>
          <el-input v-model="password" type="password" placeholder="请输入密码" size="large" show-password minlength="6" />
        </el-form-item>
        <el-alert v-if="error" type="error" :title="error" show-icon class="mb-4" />
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="w-full" size="large">
            {{ loading ? '注册中…' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <p class="text-center text-sm text-gray-500">
        已有账号？<router-link to="/login" class="text-primary font-medium">登录</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

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
    await auth.register(username.value, password.value)
    await auth.login(username.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
