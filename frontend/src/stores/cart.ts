import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import type { Product } from '@/api/types'

export interface CartItem {
  product: Product
  quantity: number
}

export const MAX_QUANTITY_PER_ITEM = 10

const STORAGE_KEY = 'orderflow.cart'

function loadItems(): CartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as CartItem[]) : []
  } catch {
    return []
  }
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>(loadItems())

  // El carrito sobrevive a una recarga de la página. flush 'sync': se guarda en el mismo instante.
  watch(
    items,
    (value) => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
      } catch {
        // Almacenamiento no disponible (modo privado): el carrito funciona solo en memoria.
      }
    },
    { deep: true, flush: 'sync' },
  )

  const count = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  const total = computed(() =>
    items.value.reduce((sum, i) => sum + i.product.price * i.quantity, 0),
  )
  const isEmpty = computed(() => items.value.length === 0)

  function quantityOf(sku: string) {
    return items.value.find((i) => i.product.sku === sku)?.quantity ?? 0
  }

  /** Máximo de unidades de un producto: su stock disponible o el tope por línea. */
  function limitOf(product: Product) {
    return Math.min(product.availableQuantity, MAX_QUANTITY_PER_ITEM)
  }

  /** Añade una unidad respetando el stock disponible y el máximo por línea. */
  function add(product: Product) {
    const limit = limitOf(product)
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
    if (quantity <= 0) remove(sku)
    else item.quantity = Math.min(quantity, limitOf(item.product))
  }

  function remove(sku: string) {
    items.value = items.value.filter((i) => i.product.sku !== sku)
  }

  function clear() {
    items.value = []
  }

  /**
   * Actualiza precio y stock con el catálogo recién cargado y ajusta las cantidades. Un producto
   * que ya no existe o se ha quedado sin stock sale del carrito.
   */
  function refresh(catalog: Product[]) {
    const bySku = new Map(catalog.map((p) => [p.sku, p]))
    items.value = items.value.flatMap((item) => {
      const product = bySku.get(item.product.sku)
      if (!product || limitOf(product) === 0) return []
      return [{ product, quantity: Math.min(item.quantity, limitOf(product)) }]
    })
  }

  function toOrderRequest() {
    return { lines: items.value.map((i) => ({ sku: i.product.sku, quantity: i.quantity })) }
  }

  return { items, count, total, isEmpty, quantityOf, limitOf, add, setQuantity, remove, clear, refresh, toOrderRequest }
})
