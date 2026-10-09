import { createI18n } from 'vue-i18n'
import en from './locales/en'
import es from './locales/es'

export const LOCALES = ['en', 'es'] as const
export type Locale = (typeof LOCALES)[number]

const DEFAULT_LOCALE: Locale = 'en'
const STORAGE_KEY = 'orderflow.locale'

function loadLocale(): Locale {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return LOCALES.includes(saved as Locale) ? (saved as Locale) : DEFAULT_LOCALE
  } catch {
    return DEFAULT_LOCALE
  }
}

export const i18n = createI18n({
  legacy: false,
  locale: loadLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: { en, es },
})

export const currentLocale = (): Locale => i18n.global.locale.value

/** Cambia el idioma, lo recuerda para la próxima visita y actualiza el atributo lang de la página. */
export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale
  document.documentElement.lang = locale
  try {
    localStorage.setItem(STORAGE_KEY, locale)
  } catch {
    // Almacenamiento no disponible (modo privado): el idioma dura lo que dure la pestaña.
  }
}

document.documentElement.lang = currentLocale()
