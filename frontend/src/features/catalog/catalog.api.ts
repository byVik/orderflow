import { request } from '@/shared/api/http'
import type { Product } from './catalog.types'

export const catalogApi = {
  products: () => request<Product[]>('/api/products'),
}
