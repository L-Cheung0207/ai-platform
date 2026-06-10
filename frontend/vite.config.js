import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8081'
  const devServerHost = env.VITE_DEV_SERVER_HOST || '0.0.0.0'
  const devServerPort = Number(env.VITE_DEV_SERVER_PORT || 8888)

  return {
    plugins: [vue(), tailwindcss()],
    server: {
      host: devServerHost,
      port: devServerPort,
      strictPort: true,
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
        '/uploads': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
