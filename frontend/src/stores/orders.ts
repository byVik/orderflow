import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/api/client'
import type { Order } from '@/api/types'

export const useOrdersStore = defineStore('orders', () => {
  const orders = ref<Order[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const hasPending = () => orders.value.some((o) => o.status === 'PENDING')

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      orders.value = await api.orders()
    } catch (e) {
      error.value = (e as Error).message
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
