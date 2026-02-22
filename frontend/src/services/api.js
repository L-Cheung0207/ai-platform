import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
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
    // 401（含 token 过期）或访问管理接口时的 403：清除登录状态并跳转登录页
    if (status === 401 || (status === 403 && err.config?.url?.includes('/admin/'))) {
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
