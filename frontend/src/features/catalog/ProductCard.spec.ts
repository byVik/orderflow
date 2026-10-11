import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ProductCard from './ProductCard.vue'

const product = { sku: 'KB-01', name: 'Keyboard', price: 89.9, availableQuantity: 2 }

describe('ProductCard', () => {
  it('emits add when the button is clicked', async () => {
    const wrapper = mount(ProductCard, { props: { product, inCart: 0 } })
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('add')?.[0]).toEqual([product])
  })

  it('disables the button when all the stock is already in the cart', () => {
    const wrapper = mount(ProductCard, { props: { product, inCart: 2 } })
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  })

  it('shows when there is no stock', () => {
    const wrapper = mount(ProductCard, { props: { product: { ...product, availableQuantity: 0 }, inCart: 0 } })
    expect(wrapper.text()).toContain('Out of stock')
  })
})
