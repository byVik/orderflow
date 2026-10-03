import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { MAX_QUANTITY_PER_ITEM, useCartStore } from '@/stores/cart'
import type { Product } from '@/api/types'

const product = (sku: string, price: number, availableQuantity: number): Product => ({
  sku,
  name: `Producto ${sku}`,
  price,
  availableQuantity,
})

describe('cart store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('suma cantidades y total', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    cart.add(product('KB-01', 89.9, 10))
    cart.add(product('MS-01', 39.9, 10))

    expect(cart.count).toBe(3)
    expect(cart.total).toBeCloseTo(219.7)
  })

  it('no permite superar el stock disponible', () => {
    const cart = useCartStore()
    const mic = product('MC-01', 79, 2)
    cart.add(mic)
    cart.add(mic)
    cart.add(mic)

    expect(cart.quantityOf('MC-01')).toBe(2)
  })

  it('limita la cantidad máxima por línea', () => {
    const cart = useCartStore()
    cart.add(product('LS-01', 29.9, 100))
    cart.setQuantity('LS-01', 500)

    expect(cart.quantityOf('LS-01')).toBe(MAX_QUANTITY_PER_ITEM)
  })

  it('poner cantidad 0 elimina la línea', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    cart.setQuantity('KB-01', 0)

    expect(cart.isEmpty).toBe(true)
  })

  it('no añade productos sin stock', () => {
    const cart = useCartStore()
    cart.add(product('XX-00', 10, 0))
    expect(cart.isEmpty).toBe(true)
  })

  it('genera la petición para el backend', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    expect(cart.toOrderRequest()).toEqual({ lines: [{ sku: 'KB-01', quantity: 1 }] })
  })
})
