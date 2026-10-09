<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()
const { t } = useI18n()

// Salida voluntaria: se vacía el carrito porque puede entrar otra persona en este navegador.
function logout() {
  cart.clear()
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="border-b border-line bg-white">
    <nav class="mx-auto flex max-w-280 flex-wrap items-center gap-x-7 gap-y-2 px-6 py-3" :aria-label="t('nav.main')">
      <RouterLink to="/catalog" class="flex items-center gap-2.5 text-[19px] font-bold tracking-tight">
        <AppLogo />
        OrderFlow
      </RouterLink>
      <RouterLink to="/catalog" class="nav-link" active-class="nav-link--active">{{ t('nav.catalog') }}</RouterLink>
      <RouterLink to="/orders" class="nav-link" active-class="nav-link--active">{{ t('nav.orders') }}</RouterLink>
      <div class="ml-auto flex flex-wrap items-center gap-x-5 gap-y-2">
        <RouterLink to="/cart" class="btn btn-primary">
          {{ t('nav.cart') }}
          <span
            v-if="cart.count"
            data-testid="cart-count"
            class="num min-w-5.5 rounded-full bg-white px-1.5 text-center text-[13px] leading-5.5 font-medium text-brand-700"
          >{{ cart.count }}</span>
        </RouterLink>
        <LanguageSwitch />
        <span class="text-sm text-muted">{{ auth.username }}</span>
        <button type="button" class="btn btn-quiet text-sm" @click="logout">{{ t('nav.logout') }}</button>
      </div>
    </nav>
  </header>
</template>

<style scoped>
@reference '../style.css';

.nav-link {
  @apply py-3 text-[15px] font-medium text-muted hover:text-ink;
}
.nav-link--active {
  @apply font-semibold text-ink;
  box-shadow: inset 0 -2px 0 var(--color-brand-600);
}
</style>
