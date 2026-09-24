import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  esbuild: {
    drop: mode === 'production' ? ['debugger'] : [],
    pure: mode === 'production' ? ['console.log', 'console.debug', 'console.info', 'console.table'] : [],
  },
  build: {
    sourcemap: false,
    minify: 'esbuild',
  },
  server: {
    open: false,
    proxy: {
      '/basket': {
        target: 'http://localhost:8881',
        changeOrigin: true,
      },
      '/web-api': {
        target: 'http://localhost:8881',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/web-api/, ''),
      },
      '/head': {
        target: 'http://localhost:8881',
        changeOrigin: true,
      },
      '/player-avatar': {
        target: 'http://localhost:8881',
        changeOrigin: true,
      },
    },
  },
}))
