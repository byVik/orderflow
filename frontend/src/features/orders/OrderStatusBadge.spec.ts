import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { setLocale } from '@/shared/i18n'
import OrderStatusBadge from './OrderStatusBadge.vue'

describe('OrderStatusBadge', () => {
  it.each([
    ['PENDING', 'Pending'],
    ['CONFIRMED', 'Confirmed'],
    ['REJECTED', 'Rejected'],
    ['CANCELLED', 'Cancelled'],
  ] as const)('shows %s as "%s"', (status, label) => {
    const wrapper = mount(OrderStatusBadge, { props: { status } })
    expect(wrapper.text()).toBe(label)
  })

  it('changes its text when the language changes, without remounting', async () => {
    const wrapper = mount(OrderStatusBadge, { props: { status: 'CONFIRMED' } })

    setLocale('es')
    await nextTick()

    expect(wrapper.text()).toBe('Confirmado')
  })
})
