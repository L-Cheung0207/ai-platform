<script setup>
import { computed, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const visible = defineModel({ type: Boolean, required: true })

defineProps({
  title: {
    type: String,
    default: '登录',
  },
  description: {
    type: String,
    default: '登录后即可登记 Skill、上传 Rule',
  },
})

const emit = defineEmits(['success'])

const router = useRouter()
const auth = useAuthStore()

const username = shallowRef('')
const password = shallowRef('')
const loading = shallowRef(false)
const error = shallowRef('')

const canSubmit = computed(() => username.value.trim() && password.value)

function resetForm() {
  username.value = ''
  password.value = ''
  error.value = ''
  loading.value = false
}

async function submit() {
  if (loading.value) return
  if (!canSubmit.value) {
    error.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const data = await auth.login(username.value.trim(), password.value)
    visible.value = false
    ElMessage.success('登录成功')
    emit('success', data)
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

function goRegister() {
  visible.value = false
  router.push('/register')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="420px"
    align-center
    class="login-dialog"
    append-to-body
    destroy-on-close
    @closed="resetForm"
  >
    <div class="login-dialog__intro">{{ description }}</div>
    <el-form class="login-dialog__form" @submit.prevent="submit">
      <el-form-item label="用户名" required>
        <el-input
          v-model="username"
          placeholder="请输入用户名"
          size="large"
          autocomplete="username"
          aria-label="用户名"
        />
      </el-form-item>
      <el-form-item label="密码" required>
        <el-input
          v-model="password"
          type="password"
          placeholder="请输入密码"
          size="large"
          show-password
          autocomplete="current-password"
          aria-label="密码"
        />
      </el-form-item>
      <el-alert v-if="error" type="error" :title="error" show-icon class="login-dialog__alert" />
      <el-form-item class="login-dialog__submit-item">
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          :disabled="loading"
          size="large"
          class="login-dialog__submit"
        >
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </el-form-item>
    </el-form>
    <p class="login-dialog__footer">
      还没有账号？
      <button type="button" class="login-dialog__link" @click="goRegister">注册</button>
    </p>
  </el-dialog>
</template>

<style scoped>
:global(.login-dialog.el-dialog.is-align-center) {
  margin: auto !important;
}

.login-dialog__intro {
  margin: 0 0 20px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.login-dialog__form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-dialog__form :deep(.el-form-item__content) {
  width: 100%;
}

.login-dialog__form :deep(.el-input) {
  width: 100%;
}

.login-dialog__form :deep(.el-form-item__label) {
  min-width: 5rem;
  color: #334155;
  font-weight: 500;
}

.login-dialog__alert {
  margin-bottom: 16px;
}

.login-dialog__submit-item {
  margin-bottom: 0;
}

.login-dialog__submit-item :deep(.el-form-item__content) {
  justify-content: stretch;
}

.login-dialog__submit {
  width: 100%;
  min-height: 44px;
  border-radius: 8px;
  font-weight: 600;
}

.login-dialog__footer {
  margin: 18px 0 0;
  color: #64748b;
  font-size: 14px;
  text-align: center;
}

.login-dialog__link {
  border: 0;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
  font: inherit;
  font-weight: 600;
  padding: 0;
}

.login-dialog__link:hover {
  text-decoration: underline;
}
</style>
