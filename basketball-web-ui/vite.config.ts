import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
    server: {
    open: false,
    proxy: {
      '/web-api': {
        // 官网业务后端 basketball-web :8881（admin 后台为 :8080）
        target: 'http://localhost:18881',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/web-api/, ''),
      },
      '/head': {
        target: 'http://localhost:18881',
        changeOrigin: true,
      },
      '/player-avatar': {
        target: 'http://localhost:18881',
        changeOrigin: true,
      },
    },
  },
})
