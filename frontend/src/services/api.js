import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else {
    delete config.headers.Authorization
  }
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

api.interceptors.response.use(
  (res) => {
    const data = res.data
    if (res.status === 204) return undefined
    if (data && typeof data.code !== 'undefined' && data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data?.data !== undefined ? data.data : data
  },
  async (err) => {
    const status = err.response?.status
    const d = err.response?.data
    const msg = d?.message || d?.error || d?.msg || err.message || '网络错误'
    // 仅在登录失效时清除登录状态，避免偶发 403 误伤已登录会话
    if (status === 401) {
      const { useAuthStore } = await import('../stores/auth')
      useAuthStore().logout()
      const router = (await import('../router')).default
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    }
    return Promise.reject(new Error(msg))
  }
)

export function setAuthToken(token) {
  if (token) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete api.defaults.headers.common['Authorization']
  }
}

export default api
