import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { Product } from '@/api/types'

export interface CartItem {
  product: Product
  quantity: number
}

export const MAX_QUANTITY_PER_ITEM = 10

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const count = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  const total = computed(() =>
    items.value.reduce((sum, i) => sum + i.product.price * i.quantity, 0),
  )
  const isEmpty = computed(() => items.value.length === 0)

  function quantityOf(sku: string) {
    return items.value.find((i) => i.product.sku === sku)?.quantity ?? 0
  }

  /** Añade una unidad respetando el stock disponible y el máximo por línea. */
  function add(product: Product) {
    const limit = Math.min(product.availableQuantity, MAX_QUANTITY_PER_ITEM)
    const item = items.value.find((i) => i.product.sku === product.sku)
    if (item) {
      if (item.quantity < limit) item.quantity++
    } else if (limit > 0) {
      items.value.push({ product, quantity: 1 })
    }
  }

  function setQuantity(sku: string, quantity: number) {
    const item = items.value.find((i) => i.product.sku === sku)
    if (!item) return
    const limit = Math.min(item.product.availableQuantity, MAX_QUANTITY_PER_ITEM)
    if (quantity <= 0) remove(sku)
    else item.quantity = Math.min(quantity, limit)
  }

  function remove(sku: string) {
    items.value = items.value.filter((i) => i.product.sku !== sku)
  }

  function clear() {
    items.value = []
  }

  function toOrderRequest() {
    return { lines: items.value.map((i) => ({ sku: i.product.sku, quantity: i.quantity })) }
  }

  return { items, count, total, isEmpty, quantityOf, add, setQuantity, remove, clear, toOrderRequest }
})
