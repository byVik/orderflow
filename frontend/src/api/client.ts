import { request } from './http'
import type { Order, PlaceOrderRequest, Product } from './types'

export const api = {
  login: (username: string) =>
    request<{ accessToken: string; expiresIn: number }>('/api/auth/token', {
      method: 'POST',
      body: JSON.stringify({ username }),
    }),

  products: () => request<Product[]>('/api/products'),

  orders: () => request<Order[]>('/api/orders'),

  order: (id: string) => request<Order>(`/api/orders/${encodeURIComponent(id)}`),

  placeOrder: (body: PlaceOrderRequest) =>
    request<Order>('/api/orders', { method: 'POST', body: JSON.stringify(body) }),

  cancelOrder: (id: string) =>
    request<Order>(`/api/orders/${encodeURIComponent(id)}/cancel`, { method: 'POST' }),
}
