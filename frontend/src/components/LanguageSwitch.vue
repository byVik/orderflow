<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { LOCALES, setLocale } from '@/i18n'

const { locale, t } = useI18n()
</script>

<template>
  <div class="flex overflow-hidden rounded-lg border border-line-strong" role="group" :aria-label="t('language.label')">
    <button
      v-for="code in LOCALES"
      :key="code"
      type="button"
      class="h-11 min-w-11 cursor-pointer px-2 text-[13px] font-semibold uppercase"
      :class="code === locale ? 'bg-ink text-white' : 'bg-white text-muted hover:text-ink'"
      :aria-pressed="code === locale"
      :title="t(`language.${code}`)"
      :lang="code"
      @click="setLocale(code)"
    >
      {{ code }}
    </button>
  </div>
</template>
