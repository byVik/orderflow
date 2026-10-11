import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ApiError } from '@/shared/api/http'
import { ordersApi } from './orders.api'
import { PAGE_SIZE, useOrdersStore } from './orders.store'
import type { Order, Page } from './orders.types'

vi.mock('./orders.api', () => ({ ordersApi: { orders: vi.fn() } }))
const api = vi.mocked(ordersApi.orders)

const order = (id: string): Order => ({
  id,
  status: 'CONFIRMED',
  rejectionReason: null,
  total: 10,
  createdAt: '2026-10-11T10:00:00Z',
  lines: [],
})

const page = (ids: string[], number: number, hasNext: boolean): Page<Order> => ({
  content: ids.map(order),
  page: number,
  size: PAGE_SIZE,
  hasNext,
})

const ids = (orders: Order[]) => orders.map((o) => o.id)

describe('orders store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    api.mockReset()
  })

  it('loads the first page', async () => {
    api.mockResolvedValueOnce(page(['c', 'b'], 0, true))
    const store = useOrdersStore()

    await store.fetchAll()

    expect(api).toHaveBeenCalledWith(0, PAGE_SIZE)
    expect(ids(store.orders)).toEqual(['c', 'b'])
    expect(store.hasNext).toBe(true)
  })

  it('load more appends the next page and stops offering more on the last one', async () => {
    api.mockResolvedValueOnce(page(['c', 'b'], 0, true)).mockResolvedValueOnce(page(['a'], 1, false))
    const store = useOrdersStore()
    await store.fetchAll()

    await store.loadMore()

    expect(api).toHaveBeenLastCalledWith(1, PAGE_SIZE)
    expect(ids(store.orders)).toEqual(['c', 'b', 'a'])
    expect(store.hasNext).toBe(false)
  })

  it('does not ask for a page that does not exist', async () => {
    api.mockResolvedValueOnce(page(['a'], 0, false))
    const store = useOrdersStore()
    await store.fetchAll()

    await store.loadMore()

    expect(api).toHaveBeenCalledTimes(1)
  })

  it('refreshing reloads every page on screen, not just the first', async () => {
    api
      .mockResolvedValueOnce(page(['c', 'b'], 0, true))
      .mockResolvedValueOnce(page(['a'], 1, false))
      .mockResolvedValueOnce(page(['c', 'b'], 0, true))
      .mockResolvedValueOnce(page(['a'], 1, false))
    const store = useOrdersStore()
    await store.fetchAll()
    await store.loadMore()

    await store.fetchAll()

    expect(api).toHaveBeenCalledTimes(4)
    expect(ids(store.orders)).toEqual(['c', 'b', 'a'])
  })

  it('drops an order that comes again because a new one pushed it to the next page', async () => {
    api.mockResolvedValueOnce(page(['c', 'b'], 0, true)).mockResolvedValueOnce(page(['b', 'a'], 1, false))
    const store = useOrdersStore()
    await store.fetchAll()

    await store.loadMore()

    expect(ids(store.orders)).toEqual(['c', 'b', 'a'])
  })

  it('keeps the orders already loaded when loading more fails', async () => {
    const failure = new ApiError(503, 'Service unavailable')
    api.mockResolvedValueOnce(page(['c', 'b'], 0, true)).mockRejectedValueOnce(failure)
    const store = useOrdersStore()
    await store.fetchAll()

    await store.loadMore()

    expect(store.error).toBe(failure)
    expect(ids(store.orders)).toEqual(['c', 'b'])
    expect(store.hasNext).toBe(true)
  })
})
