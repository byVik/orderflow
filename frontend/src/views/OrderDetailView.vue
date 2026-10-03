<script setup lang="ts">
import { ref } from 'vue'
import { api } from '@/api/client'
import type { Order } from '@/api/types'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { usePolling } from '@/composables/usePolling'
import { useOrdersStore } from '@/stores/orders'
import { formatDate, formatPrice, shortId } from '@/utils/format'

const props = defineProps<{ id: string }>()
const orders = useOrdersStore()

const order = ref<Order | null>(null)
const error = ref<string | null>(null)
const cancelling = ref(false)

async function load() {
  try {
    order.value = await api.order(props.id)
    orders.upsert(order.value)
  } catch (e) {
    error.value = (e as Error).message
  }
}

usePolling(load, () => order.value?.status === 'PENDING' && !error.value)

async function cancel() {
  cancelling.value = true
  try {
    order.value = await api.cancelOrder(props.id)
    orders.upsert(order.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    cancelling.value = false
  }
}
</script>

<template>
  <section>
    <RouterLink to="/orders" class="text-sm text-brand-600 hover:underline">← Mis pedidos</RouterLink>
    <p v-if="error" class="mt-6 text-rose-700">{{ error }}</p>
    <div v-else-if="order" class="mt-4 rounded-xl border border-slate-200 bg-white p-6">
      <div class="flex flex-wrap items-center gap-3">
        <h1 class="font-mono text-xl font-bold">#{{ shortId(order.id) }}</h1>
        <OrderStatusBadge :status="order.status" />
        <span class="text-sm text-slate-500">{{ formatDate(order.createdAt) }}</span>
      </div>

      <p v-if="order.status === 'PENDING'" class="mt-3 text-sm text-slate-500">
        Esperando a que el servicio de inventario reserve el stock…
      </p>
      <p v-if="order.rejectionReason" class="mt-3 rounded-md bg-rose-50 p-3 text-sm text-rose-800">
        {{ order.rejectionReason }}
      </p>

      <table class="mt-6 w-full text-sm">
        <thead class="text-left text-slate-500">
          <tr><th class="py-2">SKU</th><th>Cantidad</th><th class="text-right">Precio</th><th class="text-right">Subtotal</th></tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="line in order.lines" :key="line.sku">
            <td class="py-2 font-mono">{{ line.sku }}</td>
            <td>{{ line.quantity }}</td>
            <td class="text-right">{{ formatPrice(line.unitPrice) }}</td>
            <td class="text-right">{{ formatPrice(line.subtotal) }}</td>
          </tr>
        </tbody>
      </table>

      <div class="mt-6 flex items-center justify-between">
        <p class="text-lg">Total: <strong>{{ formatPrice(order.total) }}</strong></p>
        <button
          v-if="order.status === 'PENDING'"
          :disabled="cancelling"
          class="rounded-md border border-rose-300 px-3 py-1.5 text-sm text-rose-700 hover:bg-rose-50 disabled:opacity-60"
          @click="cancel"
        >
          Cancelar pedido
        </button>
      </div>
    </div>
  </section>
</template>
