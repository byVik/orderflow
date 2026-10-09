import { config } from '@vue/test-utils'
import { beforeEach } from 'vitest'
import { i18n, setLocale } from '@/i18n'

// Todo componente montado en un test recibe el plugin de idiomas, y cada test empieza en inglés.
config.global.plugins = [i18n]

beforeEach(() => setLocale('en'))
