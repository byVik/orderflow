import type { ProblemDetail } from './types'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type TokenProvider = () => string | null
type UnauthorizedHandler = () => void

let getToken: TokenProvider = () => null
let onUnauthorized: UnauthorizedHandler = () => {}

/** Permite que el store de auth inyecte el token sin crear dependencias circulares. */
export function configureHttp(tokenProvider: TokenProvider, unauthorizedHandler: UnauthorizedHandler) {
  getToken = tokenProvider
  onUnauthorized = unauthorizedHandler
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(path, { ...init, headers })

  if (response.status === 401) {
    onUnauthorized()
    throw new ApiError(401, 'Tu sesión ha caducado. Vuelve a entrar.')
  }
  if (!response.ok) {
    throw new ApiError(response.status, await errorMessage(response))
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as ProblemDetail
    return problem.detail ?? problem.title ?? `Error ${response.status}`
  } catch {
    return `Error ${response.status}`
  }
}
