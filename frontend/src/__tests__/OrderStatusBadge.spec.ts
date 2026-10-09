import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import OrderStatusBadge from '@/components/OrderStatusBadge.vue'
import { setLocale } from '@/i18n'

describe('OrderStatusBadge', () => {
  it.each([
    ['PENDING', 'Pending'],
    ['CONFIRMED', 'Confirmed'],
    ['REJECTED', 'Rejected'],
    ['CANCELLED', 'Cancelled'],
  ] as const)('muestra %s como "%s"', (status, label) => {
    const wrapper = mount(OrderStatusBadge, { props: { status } })
    expect(wrapper.text()).toBe(label)
  })

  it('cambia de texto al cambiar de idioma, sin volver a montarse', async () => {
    const wrapper = mount(OrderStatusBadge, { props: { status: 'CONFIRMED' } })

    setLocale('es')
    await nextTick()

    expect(wrapper.text()).toBe('Confirmado')
  })
})
