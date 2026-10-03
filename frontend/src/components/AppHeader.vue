<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

function logout() {
  cart.clear()
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <nav class="mx-auto flex max-w-5xl items-center gap-6 px-4 py-3">
      <RouterLink to="/catalog" class="text-lg font-bold text-brand-600">OrderFlow</RouterLink>
      <RouterLink to="/catalog" class="nav-link" active-class="nav-link--active">Catálogo</RouterLink>
      <RouterLink to="/orders" class="nav-link" active-class="nav-link--active">Mis pedidos</RouterLink>
      <div class="ml-auto flex items-center gap-4">
        <RouterLink to="/cart" class="relative rounded-md bg-brand-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-600">
          Carrito
          <span v-if="cart.count" data-testid="cart-count" class="ml-1 rounded-full bg-white px-1.5 text-xs text-brand-700">{{ cart.count }}</span>
        </RouterLink>
        <span class="text-sm text-slate-500">{{ auth.username }}</span>
        <button class="text-sm text-slate-500 hover:text-slate-900" @click="logout">Salir</button>
      </div>
    </nav>
  </header>
</template>

<style scoped>
@reference '../style.css';

.nav-link {
  @apply text-sm text-slate-600 hover:text-slate-900;
}
.nav-link--active {
  @apply font-semibold text-slate-900;
}
</style>
