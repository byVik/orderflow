import { describe, expect, it } from 'vitest'
import { ApiError } from '@/api/http'
import { currentLocale, setLocale } from '@/i18n'
import { errorMessage, rejectionMessage } from '@/i18n/errors'
import { formatPrice, formatUnits } from '@/utils/format'

describe('idioma', () => {
  it('empieza en inglés', () => {
    expect(currentLocale()).toBe('en')
  })

  it('al cambiarlo se recuerda y se actualiza el lang de la página', () => {
    setLocale('es')

    expect(currentLocale()).toBe('es')
    expect(localStorage.getItem('orderflow.locale')).toBe('es')
    expect(document.documentElement.lang).toBe('es')
  })

  it('precios y plurales siguen al idioma', () => {
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
  it('traduce por el código, con los datos del error', () => {
    const error = new ApiError(400, 'Product XX-99 is not in the catalog', 'UNKNOWN_PRODUCT', { sku: 'XX-99' })

    expect(errorMessage(error)).toBe('Product XX-99 is not in the catalog.')
    setLocale('es')
    expect(errorMessage(error)).toBe('El producto XX-99 no existe en el catálogo.')
  })

  it('muestra el estado del pedido con su etiqueta, no con el valor interno', () => {
    const error = new ApiError(409, 'Cannot cancel order', 'ORDER_NOT_PENDING', { status: 'CONFIRMED' })

    expect(errorMessage(error)).toBe('This order can no longer be changed because it is confirmed.')
    setLocale('es')
    expect(errorMessage(error)).toBe('Este pedido ya no se puede modificar porque está confirmado.')
  })

  it('con un código desconocido muestra el detalle del backend', () => {
    expect(errorMessage(new ApiError(418, 'I am a teapot', 'TEAPOT'))).toBe('I am a teapot')
    expect(errorMessage(new ApiError(500, 'Error 500'))).toBe('Error 500')
  })

  it('traduce los errores que pone el cliente', () => {
    expect(errorMessage(new ApiError(0, 'Network error', 'NETWORK'))).toContain('Could not reach the server')
    expect(errorMessage(new ApiError(401, 'Session expired', 'SESSION_EXPIRED'))).toContain('session has expired')
  })

  it('un error que no viene de la API da un mensaje genérico', () => {
    expect(errorMessage(new Error('boom'))).toBe('Something went wrong. Try again in a moment.')
  })
})

describe('rejectionMessage', () => {
  it('traduce los códigos de inventario', () => {
    expect(rejectionMessage('INSUFFICIENT_STOCK')).toBe('There was not enough stock for at least one product.')
    setLocale('es')
    expect(rejectionMessage('INSUFFICIENT_STOCK')).toBe('No había stock suficiente de al menos un producto.')
  })

  it('devuelve tal cual lo que no es un código conocido', () => {
    expect(rejectionMessage('Sin stock de KB-01')).toBe('Sin stock de KB-01')
    expect(rejectionMessage('SOMETHING_NEW')).toBe('SOMETHING_NEW')
  })
})
