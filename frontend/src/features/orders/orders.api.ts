import { request } from '@/shared/api/http'
import type { Order, Page, PlaceOrderRequest } from './orders.types'

export const ordersApi = {
  orders: (page: number, size: number) => request<Page<Order>>(`/api/orders?page=${page}&size=${size}`),

  order: (id: string) => request<Order>(`/api/orders/${encodeURIComponent(id)}`),

  placeOrder: (body: PlaceOrderRequest) =>
    request<Order>('/api/orders', { method: 'POST', body: JSON.stringify(body) }),

  cancelOrder: (id: string) =>
    request<Order>(`/api/orders/${encodeURIComponent(id)}/cancel`, { method: 'POST' }),
}
