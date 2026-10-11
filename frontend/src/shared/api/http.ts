export type ProblemParams = Record<string, string | number>

/** RFC 7807 Problem Details returned by the backend, with the stable code and its data. */
export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
  code?: string
  params?: ProblemParams
}

export class ApiError extends Error {
  readonly status: number
  /** Stable error code: sent by the backend, or set by this client (SESSION_EXPIRED, NETWORK). */
  readonly code?: string
  readonly params?: ProblemParams

  constructor(status: number, message: string, code?: string, params?: ProblemParams) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.params = params
  }
}

type TokenProvider = () => string | null
type UnauthorizedHandler = () => void

let getToken: TokenProvider = () => null
let onUnauthorized: UnauthorizedHandler = () => {}

/** Lets the auth store inject the token without creating circular dependencies. */
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

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    // fetch only rejects when there is no response at all: no network, or the server is down.
    throw new ApiError(0, 'Network error', 'NETWORK')
  }

  if (response.status === 401) {
    onUnauthorized()
    throw new ApiError(401, 'Session expired', 'SESSION_EXPIRED')
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

/** The message is the English fallback; what the user sees is translated from the code. */
async function toApiError(response: Response): Promise<ApiError> {
  const fallback = `Error ${response.status}`
  try {
    const problem = (await response.json()) as ProblemDetail
    return new ApiError(response.status, problem.detail ?? problem.title ?? fallback, problem.code, problem.params)
  } catch {
    return new ApiError(response.status, fallback)
  }
}
