import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ordersApi } from './orders.api'
import type { Order } from './orders.types'

export const PAGE_SIZE = 10

/**
 * With pages by position, an order created between two requests pushes the rest down one place,
 * and the first of the next page is one we already had. The first copy is kept.
 */
function withoutDuplicates(orders: Order[]): Order[] {
  const seen = new Set<string>()
  return orders.filter((order) => !seen.has(order.id) && seen.add(order.id))
}

export const useOrdersStore = defineStore('orders', () => {
  const orders = ref<Order[]>([])
  const loading = ref(false)
  const loadingMore = ref(false)
  // The error itself, not its text: the view translates it into the current language.
  const error = ref<unknown>(null)
  // How many pages are on screen. The first load brings one.
  const pagesLoaded = ref(1)
  const hasNext = ref(false)

  const hasPending = () => orders.value.some((o) => o.status === 'PENDING')

  /** Loads the pages that are on screen. The polling calls it again while an order is PENDING. */
  async function fetchAll() {
    loading.value = true
    error.value = null
    const pageCount = pagesLoaded.value
    try {
      const pages = await Promise.all(
        Array.from({ length: pageCount }, (_, page) => ordersApi.orders(page, PAGE_SIZE)),
      )
      // "Load more" finished while this was in flight: this answer is missing that page.
      if (pageCount !== pagesLoaded.value) return
      orders.value = withoutDuplicates(pages.flatMap((page) => page.content))
      hasNext.value = pages.at(-1)?.hasNext ?? false
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (!hasNext.value || loadingMore.value) return
    loadingMore.value = true
    error.value = null
    try {
      const next = await ordersApi.orders(pagesLoaded.value, PAGE_SIZE)
      orders.value = withoutDuplicates([...orders.value, ...next.content])
      hasNext.value = next.hasNext
      pagesLoaded.value++
    } catch (e) {
      error.value = e
    } finally {
      loadingMore.value = false
    }
  }

  function upsert(order: Order) {
    const index = orders.value.findIndex((o) => o.id === order.id)
    if (index >= 0) orders.value[index] = order
    else orders.value.unshift(order)
  }

  return { orders, loading, loadingMore, error, hasNext, hasPending, fetchAll, loadMore, upsert }
})
