import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'

const CODE = /^[A-Z_]+$/

/** Traduce un código si el idioma actual lo conoce; si no, devuelve null. */
function translateCode(namespace: 'errors' | 'rejections', code: string | null | undefined, params = {}) {
  if (!code || !CODE.test(code)) return null
  const key = `${namespace}.${code}`
  return i18n.global.te(key) ? i18n.global.t(key, params) : null
}

/** Los parámetros que son un estado de pedido se muestran con su etiqueta: CONFIRMED → «confirmed». */
function localizedParams(params: ApiError['params'] = {}) {
  const status = typeof params.status === 'string' ? `status.${params.status}` : null
  return status && i18n.global.te(status) ? { ...params, status: i18n.global.t(status).toLowerCase() } : params
}

/**
 * Texto de un error en el idioma actual. Se traduce por el código; si el código no se conoce,
 * se muestra el detalle que envió el backend. Se llama desde la plantilla, así que el texto
 * cambia solo al cambiar de idioma.
 */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return translateCode('errors', error.code, localizedParams(error.params)) ?? error.message
  }
  return i18n.global.t('errors.UNEXPECTED')
}

/** Texto del motivo de rechazo de un pedido. Un motivo que no es un código conocido se muestra tal cual. */
export function rejectionMessage(reason: string): string {
  return translateCode('rejections', reason) ?? reason
}
