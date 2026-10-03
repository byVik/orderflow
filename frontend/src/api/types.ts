export interface Product {
  sku: string
  name: string
  price: number
  availableQuantity: number
}

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED'

export interface OrderLine {
  sku: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Order {
  id: string
  status: OrderStatus
  rejectionReason: string | null
  total: number
  createdAt: string
  lines: OrderLine[]
}

export interface PlaceOrderRequest {
  lines: { sku: string; quantity: number }[]
}

/** RFC 7807 Problem Details devuelto por el backend. */
export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
}
