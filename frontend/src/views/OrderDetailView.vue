<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { api } from '@/api/client'
import type { Order } from '@/api/types'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import OrderTimeline from '@/components/OrderTimeline.vue'
import { usePolling } from '@/composables/usePolling'
import { errorMessage } from '@/i18n/errors'
import { useOrdersStore } from '@/stores/orders'
import { formatDate, formatPrice, shortId } from '@/utils/format'

const props = defineProps<{ id: string }>()
const orders = useOrdersStore()
const { t } = useI18n()

const order = ref<Order | null>(null)
const error = ref<unknown>(null)
const cancelling = ref(false)

async function load() {
  try {
    order.value = await api.order(props.id)
    orders.upsert(order.value)
  } catch (e) {
    error.value = e
  }
}

const { stalled } = usePolling(load, () => order.value?.status === 'PENDING' && !error.value)

async function cancel() {
  cancelling.value = true
  try {
    order.value = await api.cancelOrder(props.id)
    orders.upsert(order.value)
  } catch (e) {
    // Un 409 significa que el pedido cambió mientras tanto: se recarga para mostrar su estado real.
    error.value = e
    await load()
  } finally {
    cancelling.value = false
  }
}
</script>

<template>
  <main class="mx-auto w-full max-w-220 px-6 pt-8 pb-16">
    <RouterLink to="/orders" class="inline-flex h-11 items-center gap-1.5 text-sm font-semibold text-brand-700 hover:underline">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M15 6l-6 6 6 6" />
      </svg>
      {{ t('nav.orders') }}
    </RouterLink>

    <p v-if="error" class="alert mt-4" role="alert">{{ errorMessage(error) }}</p>

    <template v-if="order">
      <div class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-3">
        <h1 class="num text-3xl font-medium tracking-tight">#{{ shortId(order.id) }}</h1>
        <OrderStatusBadge :status="order.status" />
        <span class="text-sm text-muted">{{ formatDate(order.createdAt) }}</span>
      </div>

      <OrderTimeline class="mt-6" :status="order.status" :rejection-reason="order.rejectionReason" />
      <p v-if="stalled" class="mt-3 text-sm text-muted">{{ t('order.stalled') }}</p>

      <section class="card mt-5 overflow-x-auto">
        <table class="w-full min-w-120 text-[15px]">
          <thead class="text-left text-[13px] text-muted">
            <tr>
              <th scope="col" class="px-5 py-3.5 font-semibold">{{ t('order.product') }}</th>
              <th scope="col" class="px-5 py-3.5 text-right font-semibold">{{ t('order.quantity') }}</th>
              <th scope="col" class="px-5 py-3.5 text-right font-semibold">{{ t('order.price') }}</th>
              <th scope="col" class="px-5 py-3.5 text-right font-semibold">{{ t('order.subtotal') }}</th>
            </tr>
          </thead>
          <tbody class="num">
            <tr v-for="line in order.lines" :key="line.sku" class="border-t border-chip">
              <td class="px-5 py-3.5">{{ line.sku }}</td>
              <td class="px-5 py-3.5 text-right">{{ line.quantity }}</td>
              <td class="px-5 py-3.5 text-right">{{ formatPrice(line.unitPrice) }}</td>
              <td class="px-5 py-3.5 text-right">{{ formatPrice(line.subtotal) }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <div class="mt-5 flex flex-wrap items-center justify-between gap-4">
        <p>
          {{ t('common.total') }}
          <span class="num ml-3 text-[26px] font-medium tracking-tight">{{ formatPrice(order.total) }}</span>
        </p>
        <button v-if="order.status === 'PENDING'" type="button" :disabled="cancelling" class="btn btn-danger" @click="cancel">
          {{ cancelling ? t('order.cancelling') : t('order.cancel') }}
        </button>
      </div>
    </template>
  </main>
</template>
