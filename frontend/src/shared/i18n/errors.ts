import { ApiError } from '@/shared/api/http'
import { i18n } from '@/shared/i18n'

const CODE = /^[A-Z_]+$/

/** Translates a code if the current language knows it; otherwise returns null. */
function translateCode(namespace: 'errors' | 'rejections', code: string | null | undefined, params = {}) {
  if (!code || !CODE.test(code)) return null
  const key = `${namespace}.${code}`
  return i18n.global.te(key) ? i18n.global.t(key, params) : null
}

/** Params that are an order status are shown with their label: CONFIRMED → "confirmed". */
function localizedParams(params: ApiError['params'] = {}) {
  const status = typeof params.status === 'string' ? `status.${params.status}` : null
  return status && i18n.global.te(status) ? { ...params, status: i18n.global.t(status).toLowerCase() } : params
}

/**
 * Text of an error in the current language. It is translated from the code; if the code is
 * unknown, the detail sent by the backend is shown. It is called from the template, so the text
 * updates by itself when the language changes.
 */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return translateCode('errors', error.code, localizedParams(error.params)) ?? error.message
  }
  return i18n.global.t('errors.UNEXPECTED')
}

/** Text of the reason an order was rejected. A reason that is not a known code is shown as is. */
export function rejectionMessage(reason: string): string {
  return translateCode('rejections', reason) ?? reason
}
