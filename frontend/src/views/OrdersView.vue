<script setup lang="ts">
import { useOrdersStore } from '@/stores/orders'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { usePolling } from '@/composables/usePolling'
import { formatDate, formatPrice, shortId } from '@/utils/format'

const store = useOrdersStore()
usePolling(store.fetchAll, store.hasPending)
</script>

<template>
  <section>
    <h1 class="text-2xl font-bold">Mis pedidos</h1>
    <p v-if="store.error" class="mt-6 text-rose-700">{{ store.error }}</p>
    <p v-else-if="!store.loading && store.orders.length === 0" class="mt-6 text-slate-500">
      Todavía no has hecho ningún pedido.
    </p>
    <table v-else class="mt-6 w-full overflow-hidden rounded-xl border border-slate-200 bg-white text-sm">
      <thead class="bg-slate-50 text-left text-slate-500">
        <tr>
          <th class="p-3">Pedido</th>
          <th class="p-3">Fecha</th>
          <th class="p-3">Estado</th>
          <th class="p-3 text-right">Total</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-slate-100">
        <tr v-for="order in store.orders" :key="order.id" class="hover:bg-slate-50">
          <td class="p-3">
            <RouterLink :to="{ name: 'order', params: { id: order.id } }" class="font-mono text-brand-600 hover:underline">
              #{{ shortId(order.id) }}
            </RouterLink>
          </td>
          <td class="p-3">{{ formatDate(order.createdAt) }}</td>
          <td class="p-3"><OrderStatusBadge :status="order.status" /></td>
          <td class="p-3 text-right font-semibold">{{ formatPrice(order.total) }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
