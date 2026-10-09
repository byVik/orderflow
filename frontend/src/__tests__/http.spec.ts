import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, configureHttp, request } from '@/api/http'

afterEach(() => vi.unstubAllGlobals())

const jsonResponse = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('request', () => {
  it('envía el token Bearer', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, []))
    vi.stubGlobal('fetch', fetchMock)
    configureHttp(() => 'abc', () => {})

    await request('/api/orders')

    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer abc')
  })

  it('convierte un Problem Detail en ApiError con su código y sus datos', async () => {
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

  it('una respuesta de error sin cuerpo JSON no tiene código', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('oops', { status: 500 })))
    configureHttp(() => null, () => {})

    const error = await request('/api/orders').catch((e) => e)

    expect(error).toMatchObject({ status: 500, message: 'Error 500', code: undefined })
  })

  it('avisa cuando la sesión ha caducado', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })))
    const onUnauthorized = vi.fn()
    configureHttp(() => 'old', onUnauthorized)

    await expect(request('/api/orders')).rejects.toMatchObject({ status: 401, code: 'SESSION_EXPIRED' })
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('sin red lanza un ApiError con el código NETWORK', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    configureHttp(() => null, () => {})

    await expect(request('/api/orders')).rejects.toMatchObject({ code: 'NETWORK' })
  })
})
