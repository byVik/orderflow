import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, configureHttp, request } from './http'

afterEach(() => vi.unstubAllGlobals())

const jsonResponse = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('request', () => {
  it('sends the Bearer token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, []))
    vi.stubGlobal('fetch', fetchMock)
    configureHttp(() => 'abc', () => {})

    await request('/api/orders')

    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer abc')
  })

  it('turns a Problem Detail into an ApiError with its code and params', async () => {
    const problem = { detail: 'Cannot cancel order', code: 'ORDER_NOT_PENDING', params: { status: 'CONFIRMED' } }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(409, problem)))
    configureHttp(() => null, () => {})

    const error = await request('/api/orders/1/cancel').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 409,
      message: 'Cannot cancel order',
      code: 'ORDER_NOT_PENDING',
      params: { status: 'CONFIRMED' },
    })
  })

  it('an error response without a JSON body has no code', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('oops', { status: 500 })))
    configureHttp(() => null, () => {})

    const error = await request('/api/orders').catch((e) => e)

    expect(error).toMatchObject({ status: 500, message: 'Error 500', code: undefined })
  })

  it('notifies when the session has expired', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })))
    const onUnauthorized = vi.fn()
    configureHttp(() => 'old', onUnauthorized)

    await expect(request('/api/orders')).rejects.toMatchObject({ status: 401, code: 'SESSION_EXPIRED' })
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('with no network it throws an ApiError with the NETWORK code', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    configureHttp(() => null, () => {})

    await expect(request('/api/orders')).rejects.toMatchObject({ code: 'NETWORK' })
  })
})
