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
  /** Reason code (INSUFFICIENT_STOCK…); the interface supplies the text. */
  rejectionReason: string | null
  total: number
  createdAt: string
  lines: OrderLine[]
}

export interface PlaceOrderRequest {
  lines: { sku: string; quantity: number }[]
}
