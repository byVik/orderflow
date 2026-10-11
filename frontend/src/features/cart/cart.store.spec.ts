import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import type { Product } from '@/features/catalog/catalog.types'
import { MAX_QUANTITY_PER_ITEM, useCartStore } from './cart.store'

const product = (sku: string, price: number, availableQuantity: number): Product => ({
  sku,
  name: `Product ${sku}`,
  price,
  availableQuantity,
})

describe('cart store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('adds up quantities and total', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    cart.add(product('KB-01', 89.9, 10))
    cart.add(product('MS-01', 39.9, 10))

    expect(cart.count).toBe(3)
    expect(cart.total).toBeCloseTo(219.7)
  })

  it('does not allow exceeding the available stock', () => {
    const cart = useCartStore()
    const mic = product('MC-01', 79, 2)
    cart.add(mic)
    cart.add(mic)
    cart.add(mic)

    expect(cart.quantityOf('MC-01')).toBe(2)
  })

  it('caps the quantity per line', () => {
    const cart = useCartStore()
    cart.add(product('LS-01', 29.9, 100))
    cart.setQuantity('LS-01', 500)

    expect(cart.quantityOf('LS-01')).toBe(MAX_QUANTITY_PER_ITEM)
  })

  it('setting the quantity to 0 removes the line', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    cart.setQuantity('KB-01', 0)

    expect(cart.isEmpty).toBe(true)
  })

  it('does not add products that are out of stock', () => {
    const cart = useCartStore()
    cart.add(product('XX-00', 10, 0))
    expect(cart.isEmpty).toBe(true)
  })

  it('builds the request for the backend', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    expect(cart.toOrderRequest()).toEqual({ lines: [{ sku: 'KB-01', quantity: 1 }] })
  })

  it('the cart survives a page reload', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    cart.add(product('KB-01', 89.9, 10))

    setActivePinia(createPinia()) // another "tab": a new store that reads what was saved
    expect(useCartStore().quantityOf('KB-01')).toBe(2)
  })

  it('refreshing the catalog updates prices and trims what is no longer available', () => {
    const cart = useCartStore()
    cart.add(product('KB-01', 89.9, 10))
    cart.setQuantity('KB-01', 5)
    cart.add(product('MS-01', 39.9, 10))
    cart.add(product('GONE', 5, 10))

    cart.refresh([product('KB-01', 99.9, 2), product('MS-01', 39.9, 0)])

    expect(cart.quantityOf('KB-01')).toBe(2)
    expect(cart.items[0].product.price).toBe(99.9)
    expect(cart.quantityOf('MS-01')).toBe(0)
    expect(cart.quantityOf('GONE')).toBe(0)
  })
})
