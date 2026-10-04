import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { router } from '@/router'

const session = (expiresAt: number) =>
  localStorage.setItem('orderflow.session', JSON.stringify({ username: 'viktor', token: 't', expiresAt }))

describe('guard del router', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('sin sesión redirige al login y recuerda a dónde se iba', async () => {
    setActivePinia(createPinia())

    await router.push('/orders')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/orders')
  })

  it('una sesión caducada no cuenta como sesión', async () => {
    session(Date.now() - 1000)
    setActivePinia(createPinia())

    await router.push('/cart')

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('con sesión deja pasar, y el login lleva al catálogo', async () => {
    session(Date.now() + 60_000)
    setActivePinia(createPinia())

    await router.push('/orders')
    expect(router.currentRoute.value.name).toBe('orders')

    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('catalog')
  })
})
