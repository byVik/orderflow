import { currentLocale, i18n, type Locale } from '@/i18n'

// Precios y fechas siguen al idioma de la interfaz. Se leen dentro de la plantilla, así que
// cambian solos al cambiar de idioma.
const INTL: Record<Locale, { currency: Intl.NumberFormat; dateTime: Intl.DateTimeFormat }> = {
  en: {
    currency: new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'EUR' }),
    dateTime: new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }),
  },
  es: {
    currency: new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }),
    dateTime: new Intl.DateTimeFormat('es-ES', { dateStyle: 'medium', timeStyle: 'short' }),
  },
}

export const formatPrice = (value: number) => INTL[currentLocale()].currency.format(value)
export const formatDate = (iso: string) => INTL[currentLocale()].dateTime.format(new Date(iso))
export const formatUnits = (units: number) => i18n.global.t('orders.units', units)
export const shortId = (id: string) => id.slice(0, 8).toUpperCase()
