import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { initAuthState } from '@/composables/useAuth'
import '@/assets/styles/index.scss'

const app = createApp(App)

app.use(createPinia())
void initAuthState()
app.use(router)
app.use(ElementPlus)

app.mount('#app')
