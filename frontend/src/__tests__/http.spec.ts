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

  it('convierte un Problem Detail en ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(409, { detail: 'No se puede cancelar' })))
    configureHttp(() => null, () => {})

    await expect(request('/api/orders/1/cancel')).rejects.toEqual(new ApiError(409, 'No se puede cancelar'))
  })

  it('avisa cuando la sesión ha caducado', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })))
    const onUnauthorized = vi.fn()
    configureHttp(() => 'old', onUnauthorized)

    await expect(request('/api/orders')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })
})
