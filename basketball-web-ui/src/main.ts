import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { initAuthState } from '@/composables/useAuth'
import '@/assets/styles/index.scss'
import '@/assets/styles/account-layout.css'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()

  app.use(pinia)
  await initAuthState()
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
}

void bootstrap()
