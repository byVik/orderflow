const currency = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' })
const dateTime = new Intl.DateTimeFormat('es-ES', { dateStyle: 'medium', timeStyle: 'short' })

export const formatPrice = (value: number) => currency.format(value)
export const formatDate = (iso: string) => dateTime.format(new Date(iso))
export const formatUnits = (units: number) => (units === 1 ? '1 artículo' : `${units} artículos`)
export const shortId = (id: string) => id.slice(0, 8).toUpperCase()
