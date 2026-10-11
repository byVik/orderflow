import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ordersApi } from './orders.api'
import OrdersView from './OrdersView.vue'
import type { Order, Page } from './orders.types'

vi.mock('./orders.api', () => ({ ordersApi: { orders: vi.fn() } }))
const api = vi.mocked(ordersApi.orders)

const order = (id: string): Order => ({
  id,
  status: 'CONFIRMED',
  rejectionReason: null,
  total: 10,
  createdAt: '2026-10-11T10:00:00Z',
  lines: [{ sku: 'KB-01', quantity: 1, unitPrice: 10, subtotal: 10 }],
})

const page = (ids: string[], number: number, hasNext: boolean): Page<Order> => ({
  content: ids.map(order),
  page: number,
  size: 10,
  hasNext,
})

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(OrdersView, { global: { plugins: [pinia], stubs: { RouterLink: RouterLinkStub } } })
  await flushPromises()
  return wrapper
}

describe('OrdersView', () => {
  beforeEach(() => api.mockReset())

  it('offers to load more while there is a next page, and appends it', async () => {
    api
      .mockResolvedValueOnce(page(['aaaaaaaa-1', 'bbbbbbbb-2'], 0, true))
      .mockResolvedValueOnce(page(['cccccccc-3'], 1, false))
    const wrapper = await mountView()
    expect(wrapper.findAll('li')).toHaveLength(2)

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(wrapper.findAll('li')).toHaveLength(3)
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('shows no button when everything fits in one page', async () => {
    api.mockResolvedValueOnce(page(['aaaaaaaa-1'], 0, false))

    const wrapper = await mountView()

    expect(wrapper.findAll('li')).toHaveLength(1)
    expect(wrapper.find('button').exists()).toBe(false)
  })
})
