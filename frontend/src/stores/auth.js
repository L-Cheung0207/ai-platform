import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api, { setAuthToken } from '../services/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  if (token.value) setAuthToken(token.value)

  const isAuthenticated = computed(() => !!token.value)

  async function login(username, password) {
    const data = await api.post('/auth/login', { username, password })
    token.value = data.token
    user.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    setAuthToken(data.token)
    return data
  }

  async function register(username, password) {
    const data = await api.post('/auth/register', { username, password })
    return data
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setAuthToken(null)
  }

  async function fetchMe() {
    const data = await api.get('/auth/me')
    user.value = data
    localStorage.setItem('user', JSON.stringify(data))
    return data
  }

  return { token, user, isAuthenticated, login, register, logout, fetchMe }
})
