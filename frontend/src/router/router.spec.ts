import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { router } from '@/router'

const session = (expiresAt: number) =>
  localStorage.setItem('orderflow.session', JSON.stringify({ username: 'viktor', token: 't', expiresAt }))

describe('router guard', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('without a session it redirects to login and remembers the destination', async () => {
    setActivePinia(createPinia())

    await router.push('/orders')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/orders')
  })

  it('an expired session does not count as a session', async () => {
    session(Date.now() - 1000)
    setActivePinia(createPinia())

    await router.push('/cart')

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('with a session it lets you through, and login leads to the catalog', async () => {
    session(Date.now() + 60_000)
    setActivePinia(createPinia())

    await router.push('/orders')
    expect(router.currentRoute.value.name).toBe('orders')

    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('catalog')
  })
})
