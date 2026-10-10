import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { i18n } from '@/i18n'
import { initAuthState } from '@/composables/useAuth'
import { installBrowserDeterrence } from '@/security/browserDeterrence'
import { useLocaleStore } from '@/stores/locale'
import '@/assets/styles/index.scss'
import '@/assets/styles/account-layout.css'

installBrowserDeterrence()

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()

  app.use(pinia)
  app.use(i18n)
  useLocaleStore().init()
  await initAuthState()
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
}

void bootstrap()
