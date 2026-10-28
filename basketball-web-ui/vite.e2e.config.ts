import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const target = 'http://127.0.0.1:18881'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5199,
    strictPort: true,
    proxy: {
      '/basket': { target, changeOrigin: true },
      '/web-api': { target, changeOrigin: true, rewrite: (path) => path.replace(/^\/web-api/, '') },
      '/head': { target, changeOrigin: true },
      '/player-avatar': { target, changeOrigin: true },
    },
  },
})
