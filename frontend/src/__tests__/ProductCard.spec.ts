import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ProductCard from '@/components/ProductCard.vue'

const product = { sku: 'KB-01', name: 'Teclado', price: 89.9, availableQuantity: 2 }

describe('ProductCard', () => {
  it('emite add al pulsar el botón', async () => {
    const wrapper = mount(ProductCard, { props: { product, inCart: 0 } })
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('add')?.[0]).toEqual([product])
  })

  it('deshabilita el botón cuando ya está todo el stock en el carrito', () => {
    const wrapper = mount(ProductCard, { props: { product, inCart: 2 } })
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  })

  it('indica cuando no hay stock', () => {
    const wrapper = mount(ProductCard, { props: { product: { ...product, availableQuantity: 0 }, inCart: 0 } })
    expect(wrapper.text()).toContain('Out of stock')
  })
})
