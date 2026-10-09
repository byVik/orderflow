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
  /** Código del motivo (INSUFFICIENT_STOCK…); el texto lo pone la interfaz. */
  rejectionReason: string | null
  total: number
  createdAt: string
  lines: OrderLine[]
}

export interface PlaceOrderRequest {
  lines: { sku: string; quantity: number }[]
}

export type ProblemParams = Record<string, string | number>

/** RFC 7807 Problem Details devuelto por el backend, con el código estable y sus datos. */
export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
  code?: string
  params?: ProblemParams
}
