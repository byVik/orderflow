<script setup lang="ts">
import { useOrdersStore } from '@/stores/orders'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { usePolling } from '@/composables/usePolling'
import { formatDate, formatPrice, formatUnits, shortId } from '@/utils/format'
import type { Order } from '@/api/types'

const store = useOrdersStore()
usePolling(store.fetchAll, store.hasPending)

const units = (order: Order) => order.lines.reduce((sum, line) => sum + line.quantity, 0)
</script>

<template>
  <main class="page">
    <div class="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2">
      <h1 class="text-[32px] font-bold tracking-tight">Mis pedidos</h1>
      <p class="text-[15px] text-muted">Se actualiza solo mientras haya pedidos pendientes</p>
    </div>
    <p v-if="store.error" class="alert mt-7" role="alert">{{ store.error }}</p>
    <div
      v-else-if="!store.loading && store.orders.length === 0"
      class="mt-7 rounded-xl border border-dashed border-line-strong p-10 text-center text-muted"
    >
      Todavía no has hecho ningún pedido.
      <RouterLink to="/catalog" class="font-semibold text-brand-700 underline">Ver catálogo</RouterLink>
    </div>
    <ul v-else class="mt-7 flex flex-col gap-3">
      <li v-for="order in store.orders" :key="order.id">
        <RouterLink
          :to="{ name: 'order', params: { id: order.id } }"
          class="card flex flex-wrap items-center gap-x-6 gap-y-3 px-5 py-4.5 hover:border-line-strong"
        >
          <span class="num font-medium">#{{ shortId(order.id) }}</span>
          <span class="flex-[1_1_180px] text-sm text-muted">{{ formatDate(order.createdAt) }} · {{ formatUnits(units(order)) }}</span>
          <OrderStatusBadge :status="order.status" />
          <span class="num min-w-26 text-right text-[17px] font-medium">{{ formatPrice(order.total) }}</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-muted" aria-hidden="true">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </RouterLink>
      </li>
    </ul>
  </main>
</template>
