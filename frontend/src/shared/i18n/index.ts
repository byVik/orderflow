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

/** Switches the language, remembers it for the next visit and updates the page's lang attribute. */
export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale
  document.documentElement.lang = locale
  try {
    localStorage.setItem(STORAGE_KEY, locale)
  } catch {
    // Storage unavailable (private mode): the language lasts as long as the tab.
  }
}

document.documentElement.lang = currentLocale()
