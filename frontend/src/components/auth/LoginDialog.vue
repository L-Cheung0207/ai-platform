<script setup>
import { computed, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Icons from '../Icons.vue'
import { useAuthStore } from '../../stores/auth'

const visible = defineModel({ type: Boolean, required: true })

defineProps({
  title: {
    type: String,
    default: '登录',
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
    width="440px"
    align-center
    header-class="login-dialog-header"
    body-class="login-dialog-body"
    modal-class="login-dialog-modal"
    append-to-body
    destroy-on-close
    :lock-scroll="false"
    :close-on-click-modal="!loading"
    @closed="resetForm"
  >
    <template #header="{ titleId, titleClass }">
      <div class="login-dialog__header">
        <div class="login-dialog__brand">
          <span class="login-dialog__logo" aria-hidden="true">
            <Icons name="logo" :size="22" />
          </span>
          <div class="login-dialog__titles">
            <p class="login-dialog__eyebrow">AI Native Hub</p>
            <h2 :id="titleId" :class="titleClass" class="login-dialog__title">{{ title }}</h2>
          </div>
        </div>
      </div>
    </template>

    <el-form
      label-position="top"
      class="login-dialog__form"
      @submit.prevent="submit"
    >
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
          {{ loading ? '登录中…' : '登录' }}
        </el-button>
      </el-form-item>
    </el-form>

    <p class="login-dialog__footer">
      还没有账号？
      <button type="button" class="login-dialog__link" @click="goRegister">立即注册</button>
    </p>
  </el-dialog>
</template>

<style scoped>
.login-dialog__header {
  padding-right: 2rem;
}

.login-dialog__brand {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.login-dialog__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #2e68b8 0%, #2ec9d7 100%);
  box-shadow: 0 10px 20px rgba(46, 104, 184, 0.22);
}

.login-dialog__titles {
  min-width: 0;
}

.login-dialog__eyebrow {
  margin: 0 0 0.15rem;
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.login-dialog__title {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.02em;
  background: linear-gradient(90deg, #2e68b8 0%, #2ec9d7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-dialog__form :deep(.el-form-item) {
  margin-bottom: 1rem;
}

.login-dialog__form :deep(.el-form-item__label) {
  margin-bottom: 0.35rem;
  padding: 0;
  color: #334155;
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.4;
}

.login-dialog__form :deep(.el-form-item__content) {
  width: 100%;
}

.login-dialog__form :deep(.el-input) {
  width: 100%;
}

.login-dialog__form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(203, 213, 225, 0.95) inset;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.login-dialog__form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.95) inset;
}

.login-dialog__form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(46, 104, 184, 0.55) inset,
    0 0 0 3px rgba(46, 104, 184, 0.12);
}

.login-dialog__alert {
  margin-bottom: 0.75rem;
  border-radius: 10px;
}

.login-dialog__submit-item {
  margin-bottom: 0;
  margin-top: 0.35rem;
}

.login-dialog__submit-item :deep(.el-form-item__content) {
  justify-content: stretch;
}

.login-dialog__submit {
  width: 100%;
  min-height: 46px;
  border: none;
  border-radius: 10px;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: linear-gradient(90deg, #2e68b8 0%, #2ec9d7 100%);
  box-shadow: 0 10px 24px rgba(46, 104, 184, 0.24);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.login-dialog__submit:hover:not(:disabled) {
  filter: brightness(1.03);
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(46, 104, 184, 0.28);
}

.login-dialog__submit:active:not(:disabled) {
  transform: translateY(0);
}

.login-dialog__footer {
  margin: 1rem 0 0;
  padding-top: 1rem;
  border-top: 1px solid rgba(241, 245, 249, 0.95);
  color: #64748b;
  font-size: 0.875rem;
  text-align: center;
}

.login-dialog__link {
  border: 0;
  background: transparent;
  color: #2e68b8;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
  padding: 0;
  transition: color 0.2s ease, opacity 0.2s ease;
}

.login-dialog__link:hover {
  color: #2ec9d7;
  text-decoration: underline;
}

.login-dialog__link:focus-visible {
  outline: 2px solid #2e68b8;
  outline-offset: 2px;
  border-radius: 4px;
}

@media (prefers-reduced-motion: reduce) {
  .login-dialog__submit:hover:not(:disabled) {
    transform: none;
  }
}
</style>
