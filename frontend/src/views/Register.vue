<template>
  <div class="auth-page">
    <div class="auth-backdrop" aria-hidden="true" />
    <main class="auth-main">
      <div class="auth-card" role="region" aria-labelledby="register-heading">
        <header class="auth-card-header">
          <h1 id="register-heading" class="auth-title">
            <span class="auth-title-accent">注册</span>
          </h1>
          <p class="auth-desc">注册即可登录，无需审批</p>
        </header>
        <el-form @submit.prevent="submit" class="auth-form">
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
              minlength="6"
              autocomplete="new-password"
              aria-label="密码"
            />
          </el-form-item>
          <el-alert v-if="error" type="error" :title="error" show-icon class="auth-alert" />
          <el-form-item class="auth-submit-item">
            <el-button
              type="primary"
              native-type="submit"
              :loading="loading"
              :disabled="loading"
              class="auth-btn-submit"
              size="large"
            >
              {{ loading ? '注册中…' : '注册' }}
            </el-button>
          </el-form-item>
        </el-form>
        <p class="auth-footer">
          已有账号？
          <router-link to="/login" class="auth-link">登录</router-link>
        </p>
      </div>
    </main>
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
    const redirect = auth.user?.role === 'ADMIN' ? '/admin' : '/'
    router.push(redirect)
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  height: 100%;
  min-height: 0;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1.5rem;
  overflow: hidden;
  box-sizing: border-box;
}

.auth-backdrop {
  position: fixed;
  top: 4rem; /* 与 AppNavbar h-16 一致 */
  left: 0;
  right: 0;
  bottom: 0;
  background:
    linear-gradient(rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(165deg, #f0f7ff 0%, #e8f4f8 40%, #f5f9fc 100%);
  background-size: 28px 28px, 28px 28px, 100% 100%;
  background-position: 0 0, 0 0, 0 0;
}

.auth-backdrop::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -20%;
  width: 70%;
  height: 120%;
  background: radial-gradient(ellipse at center, rgba(46, 201, 215, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

.auth-backdrop::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -15%;
  width: 50%;
  height: 100%;
  background: radial-gradient(ellipse at center, rgba(46, 104, 184, 0.08) 0%, transparent 65%);
  pointer-events: none;
}

.auth-main {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
}

.auth-card {
  background: #fff;
  border-radius: 16px;
  padding: 2rem 2rem 2.25rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.8);
  animation: auth-fade-up 0.5s ease-out both;
}

.auth-card-header {
  margin-bottom: 1.75rem;
  text-align: center;
}

.auth-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin: 0 0 0.375rem 0;
}

.auth-title-accent {
  background: linear-gradient(90deg, #2e68b8 0%, #2ec9d7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-desc {
  font-size: 0.9375rem;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 1.25rem;
}

.auth-form :deep(.el-form-item__content) {
  width: 100%;
}

.auth-form :deep(.el-input) {
  width: 100%;
  display: block;
}

.auth-form :deep(.el-input__wrapper) {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.auth-form :deep(.el-input__inner) {
  min-width: 0;
}

.auth-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #334155;
  min-width: 5rem;
}

.auth-alert {
  margin-bottom: 1rem;
}

.auth-submit-item {
  margin-bottom: 0;
  margin-top: 0.5rem;
}

.auth-submit-item :deep(.el-form-item__content) {
  justify-content: stretch;
}

.auth-btn-submit {
  width: 100%;
  min-height: 44px;
  font-weight: 600;
  border-radius: 10px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.auth-btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}

.auth-btn-submit:active:not(:disabled) {
  transform: translateY(0);
}

.auth-footer {
  text-align: center;
  font-size: 0.875rem;
  color: #64748b;
  margin: 1.5rem 0 0 0;
}

.auth-link {
  color: var(--el-color-primary);
  font-weight: 500;
  text-decoration: none;
  transition: color 0.2s, opacity 0.2s;
  cursor: pointer;
}

.auth-link:hover {
  opacity: 0.85;
  text-decoration: underline;
}

.auth-link:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
  border-radius: 4px;
}

@keyframes auth-fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-card {
    animation: none;
  }
  .auth-btn-submit:hover:not(:disabled) {
    transform: none;
  }
}
</style>
