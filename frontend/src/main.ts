import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { configureHttp } from './api/http'
import { useAuthStore } from './stores/auth'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)

const auth = useAuthStore()
configureHttp(
  () => auth.token,
  () => {
    auth.logout()
    router.push({ name: 'login' })
  },
)

app.mount('#app')
