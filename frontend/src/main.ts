import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { configureHttp } from './api/http'
import { i18n } from './i18n'
import { useAuthStore } from './stores/auth'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(i18n)

const auth = useAuthStore()
configureHttp(
  () => auth.token,
  () => {
    // Sesión caducada: se conserva el carrito y la página a la que volver tras entrar de nuevo.
    auth.logout()
    const current = router.currentRoute.value
    if (current.name !== 'login') {
      router.push({ name: 'login', query: { redirect: current.fullPath } })
    }
  },
)

app.mount('#app')
