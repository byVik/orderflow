import { config } from '@vue/test-utils'
import { beforeEach } from 'vitest'
import { i18n, setLocale } from '@/shared/i18n'

// Every component mounted in a test gets the i18n plugin, and every test starts in English.
config.global.plugins = [i18n]

beforeEach(() => setLocale('en'))
