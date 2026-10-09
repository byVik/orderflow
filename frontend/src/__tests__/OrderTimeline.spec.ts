import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderTimeline from '@/components/OrderTimeline.vue'

const kinds = (wrapper: ReturnType<typeof mount>) => wrapper.findAll('li').map((li) => li.attributes('data-kind'))

describe('OrderTimeline', () => {
  it('pendiente: recibido, reservando y a la espera de confirmación', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'PENDING' } })

    expect(kinds(wrapper)).toEqual(['done', 'active', 'idle'])
    expect(wrapper.text()).toContain('Reserving stock')
  })

  it('confirmado: los tres pasos completados', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'CONFIRMED' } })

    expect(kinds(wrapper)).toEqual(['done', 'done', 'done'])
  })

  it('rechazado: traduce el código de motivo que envía inventario', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'REJECTED', rejectionReason: 'INSUFFICIENT_STOCK' } })

    expect(kinds(wrapper)).toEqual(['done', 'failed', 'failed'])
    expect(wrapper.text()).toContain('There was not enough stock')
  })

  it('rechazado: un motivo que no es un código conocido se muestra tal cual', () => {
    const wrapper = mount(OrderTimeline, {
      props: { status: 'REJECTED', rejectionReason: 'Stock insuficiente de MC-01 (disponible 3)' },
    })

    expect(wrapper.text()).toContain('Stock insuficiente de MC-01')
  })

  it('cancelado: avisa de que el stock reservado se devuelve', () => {
    const wrapper = mount(OrderTimeline, { props: { status: 'CANCELLED' } })

    expect(wrapper.text()).toContain('it is returned')
  })
})
