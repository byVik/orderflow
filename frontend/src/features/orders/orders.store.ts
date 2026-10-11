import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ordersApi } from './orders.api'
import type { Order } from './orders.types'

export const useOrdersStore = defineStore('orders', () => {
  const orders = ref<Order[]>([])
  const loading = ref(false)
  // The error itself, not its text: the view translates it into the current language.
  const error = ref<unknown>(null)

  const hasPending = () => orders.value.some((o) => o.status === 'PENDING')

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      orders.value = await ordersApi.orders()
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  function upsert(order: Order) {
    const index = orders.value.findIndex((o) => o.id === order.id)
    if (index >= 0) orders.value[index] = order
    else orders.value.unshift(order)
  }

  return { orders, loading, error, hasPending, fetchAll, upsert }
})
