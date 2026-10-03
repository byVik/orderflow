<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { useCartStore } from '@/stores/cart'
import { useOrdersStore } from '@/stores/orders'
import { formatPrice } from '@/utils/format'

const cart = useCartStore()
const orders = useOrdersStore()
const router = useRouter()

const submitting = ref(false)
const error = ref<string | null>(null)

async function checkout() {
  submitting.value = true
  error.value = null
  try {
    const order = await api.placeOrder(cart.toOrderRequest())
    orders.upsert(order)
    cart.clear()
    router.push({ name: 'order', params: { id: order.id } })
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section>
    <h1 class="text-2xl font-bold">Carrito</h1>

    <div v-if="cart.isEmpty" class="mt-6 rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-500">
      El carrito está vacío. <RouterLink to="/catalog" class="text-brand-600 underline">Ver catálogo</RouterLink>
    </div>

    <div v-else class="mt-6 rounded-xl border border-slate-200 bg-white">
      <ul class="divide-y divide-slate-100">
        <li v-for="item in cart.items" :key="item.product.sku" class="flex items-center gap-4 p-4">
          <div class="flex-1">
            <p class="font-medium">{{ item.product.name }}</p>
            <p class="text-sm text-slate-500">{{ formatPrice(item.product.price) }} / ud.</p>
          </div>
          <input
            type="number"
            min="0"
            :max="item.product.availableQuantity"
            :value="item.quantity"
            class="w-20 rounded-md border border-slate-300 px-2 py-1 text-right"
            :aria-label="`Cantidad de ${item.product.name}`"
            @change="cart.setQuantity(item.product.sku, Number(($event.target as HTMLInputElement).value))"
          />
          <p class="w-24 text-right font-semibold">{{ formatPrice(item.product.price * item.quantity) }}</p>
          <button class="text-sm text-rose-600 hover:underline" @click="cart.remove(item.product.sku)">Quitar</button>
        </li>
      </ul>
      <div class="flex items-center justify-between border-t border-slate-200 p-4">
        <p class="text-lg">Total: <strong>{{ formatPrice(cart.total) }}</strong></p>
        <button
          :disabled="submitting"
          class="rounded-md bg-brand-500 px-4 py-2 font-medium text-white hover:bg-brand-600 disabled:opacity-60"
          @click="checkout"
        >
          {{ submitting ? 'Enviando…' : 'Realizar pedido' }}
        </button>
      </div>
      <p v-if="error" class="px-4 pb-4 text-sm text-rose-700">{{ error }}</p>
    </div>
  </section>
</template>
