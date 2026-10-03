import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'

describe('OrderStatusBadge', () => {
  it.each([
    ['PENDING', 'Pendiente'],
    ['CONFIRMED', 'Confirmado'],
    ['REJECTED', 'Rechazado'],
    ['CANCELLED', 'Cancelado'],
  ] as const)('muestra %s como "%s"', (status, label) => {
    const wrapper = mount(OrderStatusBadge, { props: { status } })
    expect(wrapper.text()).toBe(label)
  })
})
