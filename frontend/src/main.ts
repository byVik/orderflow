import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from './features/auth/auth.store'
import { configureHttp } from './shared/api/http'
import { i18n } from './shared/i18n'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(i18n)

const auth = useAuthStore()
configureHttp(
  () => auth.token,
  () => {
    // Session expired: the cart is kept, and so is the page to return to after signing in again.
    auth.logout()
    const current = router.currentRoute.value
    if (current.name !== 'login') {
      router.push({ name: 'login', query: { redirect: current.fullPath } })
    }
  },
)

app.mount('#app')
