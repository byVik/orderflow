<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { errorMessage } from '@/i18n/errors'
import { useCartStore } from '@/stores/cart'
import { useOrdersStore } from '@/stores/orders'
import { formatPrice } from '@/utils/format'

const cart = useCartStore()
const orders = useOrdersStore()
const router = useRouter()
const { t } = useI18n()

const submitting = ref(false)
const error = ref<unknown>(null)

// El carrito se guarda en el navegador: al volver, precio y stock pueden haber cambiado.
onMounted(async () => {
  if (cart.isEmpty) return
  try {
    cart.refresh(await api.products())
  } catch {
    // Sin catálogo se muestra lo guardado; el servidor valida igualmente al hacer el pedido.
  }
})

async function checkout() {
  submitting.value = true
  error.value = null
  try {
    const order = await api.placeOrder(cart.toOrderRequest())
    orders.upsert(order)
    cart.clear()
    router.push({ name: 'order', params: { id: order.id } })
  } catch (e) {
    error.value = e
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="page">
    <h1 class="text-[32px] font-bold tracking-tight">{{ t('cart.title') }}</h1>

    <div v-if="cart.isEmpty" class="mt-7 rounded-xl border border-dashed border-line-strong p-10 text-center text-muted">
      {{ t('cart.empty') }}
      <RouterLink to="/catalog" class="font-semibold text-brand-700 underline">{{ t('common.browseCatalog') }}</RouterLink>
    </div>

    <div v-else class="mt-7 flex flex-wrap items-start gap-6">
      <ul class="card min-w-0 flex-[999_1_520px] divide-y divide-chip">
        <li v-for="item in cart.items" :key="item.product.sku" class="flex flex-wrap items-center gap-x-6 gap-y-4 p-5">
          <div class="flex min-w-0 flex-[1_1_220px] flex-col gap-1">
            <span class="text-[17px] font-semibold">{{ item.product.name }}</span>
            <span class="num text-[13px] text-muted">{{ item.product.sku }} · {{ t('cart.perUnit', { price: formatPrice(item.product.price) }) }}</span>
          </div>
          <div class="flex items-center overflow-hidden rounded-lg border border-line-strong">
            <button
              type="button"
              class="h-11 w-11 cursor-pointer bg-white text-xl hover:bg-ground"
              :aria-label="t('cart.removeOne', { name: item.product.name })"
              @click="cart.setQuantity(item.product.sku, item.quantity - 1)"
            >
              −
            </button>
            <span class="num min-w-10 text-center" aria-live="polite">{{ item.quantity }}</span>
            <button
              type="button"
              class="h-11 w-11 cursor-pointer bg-white text-xl hover:bg-ground disabled:cursor-not-allowed disabled:opacity-40"
              :aria-label="t('cart.addOne', { name: item.product.name })"
              :disabled="item.quantity >= cart.limitOf(item.product)"
              @click="cart.setQuantity(item.product.sku, item.quantity + 1)"
            >
              +
            </button>
          </div>
          <span class="num min-w-24 text-right text-[17px] font-medium">{{ formatPrice(item.product.price * item.quantity) }}</span>
          <button type="button" class="btn px-1 text-sm font-medium text-danger-700 underline" @click="cart.remove(item.product.sku)">
            {{ t('cart.remove') }}
          </button>
        </li>
      </ul>

      <aside class="card flex flex-[1_1_300px] flex-col gap-4 p-6">
        <h2 class="text-lg font-bold">{{ t('cart.summary') }}</h2>
        <div class="flex justify-between text-[15px] text-muted">
          <span>{{ t('cart.units') }}</span>
          <span class="num">{{ cart.count }}</span>
        </div>
        <div class="flex items-baseline justify-between border-t border-line pt-4">
          <span class="font-semibold">{{ t('common.total') }}</span>
          <span class="num text-[26px] font-medium tracking-tight">{{ formatPrice(cart.total) }}</span>
        </div>
        <button type="button" :disabled="submitting" class="btn btn-primary h-12 text-base" @click="checkout">
          {{ submitting ? t('cart.submitting') : t('cart.checkout') }}
        </button>
        <p v-if="error" class="alert" role="alert">{{ errorMessage(error) }}</p>
        <p class="text-[13px] leading-normal text-muted">{{ t('cart.note') }}</p>
      </aside>
    </div>
  </main>
</template>
