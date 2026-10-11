import { describe, expect, it } from 'vitest'
import { ApiError } from '@/shared/api/http'
import { currentLocale, setLocale } from '@/shared/i18n'
import { formatPrice, formatUnits } from '@/shared/utils/format'
import { errorMessage, rejectionMessage } from './errors'

describe('language', () => {
  it('starts in English', () => {
    expect(currentLocale()).toBe('en')
  })

  it('switching it is remembered and updates the page lang', () => {
    setLocale('es')

    expect(currentLocale()).toBe('es')
    expect(localStorage.getItem('orderflow.locale')).toBe('es')
    expect(document.documentElement.lang).toBe('es')
  })

  it('prices and plurals follow the language', () => {
    expect(formatPrice(89.9)).toContain('89.90')
    expect(formatUnits(1)).toBe('1 item')
    expect(formatUnits(3)).toBe('3 items')

    setLocale('es')

    expect(formatPrice(89.9)).toContain('89,90')
    expect(formatUnits(1)).toBe('1 artículo')
    expect(formatUnits(3)).toBe('3 artículos')
  })
})

describe('errorMessage', () => {
  it('translates from the code, with the error params', () => {
    const error = new ApiError(400, 'Product XX-99 is not in the catalog', 'UNKNOWN_PRODUCT', { sku: 'XX-99' })

    expect(errorMessage(error)).toBe('Product XX-99 is not in the catalog.')
    setLocale('es')
    expect(errorMessage(error)).toBe('El producto XX-99 no existe en el catálogo.')
  })

  it('shows the order status with its label, not the internal value', () => {
    const error = new ApiError(409, 'Cannot cancel order', 'ORDER_NOT_PENDING', { status: 'CONFIRMED' })

    expect(errorMessage(error)).toBe('This order can no longer be changed because it is confirmed.')
    setLocale('es')
    expect(errorMessage(error)).toBe('Este pedido ya no se puede modificar porque está confirmado.')
  })

  it('with an unknown code it shows the backend detail', () => {
    expect(errorMessage(new ApiError(418, 'I am a teapot', 'TEAPOT'))).toBe('I am a teapot')
    expect(errorMessage(new ApiError(500, 'Error 500'))).toBe('Error 500')
  })

  it('translates the errors set by the client', () => {
    expect(errorMessage(new ApiError(0, 'Network error', 'NETWORK'))).toContain('Could not reach the server')
    expect(errorMessage(new ApiError(401, 'Session expired', 'SESSION_EXPIRED'))).toContain('session has expired')
  })

  it('an error that does not come from the API gives a generic message', () => {
    expect(errorMessage(new Error('boom'))).toBe('Something went wrong. Try again in a moment.')
  })
})

describe('rejectionMessage', () => {
  it('translates the inventory codes', () => {
    expect(rejectionMessage('INSUFFICIENT_STOCK')).toBe('There was not enough stock for at least one product.')
    setLocale('es')
    expect(rejectionMessage('INSUFFICIENT_STOCK')).toBe('No había stock suficiente de al menos un producto.')
  })

  it('returns anything that is not a known code as is', () => {
    expect(rejectionMessage('No stock of KB-01')).toBe('No stock of KB-01')
    expect(rejectionMessage('SOMETHING_NEW')).toBe('SOMETHING_NEW')
  })
})
