import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderTimeline from './OrderTimeline.vue'

const kinds = (wrapper: ReturnType<typeof mount>) => wrapper.findAll('li').map((li) => li.attributes('data-kind'))

describe('OrderTimeline', () => {
  it('pending: received, reserving and awaiting confirmation', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'PENDING' } })

    expect(kinds(wrapper)).toEqual(['done', 'active', 'idle'])
    expect(wrapper.text()).toContain('Reserving stock')
  })

  it('confirmed: all three steps done', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'CONFIRMED' } })

    expect(kinds(wrapper)).toEqual(['done', 'done', 'done'])
  })

  it('rejected: translates the reason code sent by inventory', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'REJECTED', rejectionReason: 'INSUFFICIENT_STOCK' } })

    expect(kinds(wrapper)).toEqual(['done', 'failed', 'failed'])
    expect(wrapper.text()).toContain('There was not enough stock')
  })

  it('rejected: a reason that is not a known code is shown as is', () => {
    const wrapper = mount(OrderTimeline, {
      props: { status: 'REJECTED', rejectionReason: 'Not enough stock of MC-01 (3 available)' },
    })

    expect(wrapper.text()).toContain('Not enough stock of MC-01')
  })

  it('cancelled: says the reserved stock is returned', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'CANCELLED' } })

    expect(wrapper.text()).toContain('it is returned')
  })
})
