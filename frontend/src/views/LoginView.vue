<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { errorMessage } from '@/i18n/errors'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const username = ref('')
// Se guarda el error, no su texto: así el mensaje cambia de idioma con el resto de la pantalla.
const error = ref<unknown>(null)
const submitting = ref(false)

const STEPS = ['login.step1', 'login.step2', 'login.step3']

async function submit() {
  error.value = null
  submitting.value = true
  try {
    await auth.login(username.value.trim())
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/catalog'
    router.push(redirect)
  } catch (e) {
    error.value = e
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen flex-wrap">
    <section class="flex flex-[1_1_440px] flex-col justify-between gap-14 bg-ink p-8 text-ground sm:p-14">
      <div class="flex items-center gap-3 text-[22px] font-bold tracking-tight">
        <AppLogo :size="32" tone="light" />
        OrderFlow
      </div>
      <div class="flex max-w-115 flex-col gap-5">
        <h1 class="text-4xl leading-[1.1] font-bold tracking-tight sm:text-[44px]">{{ t('login.headline') }}</h1>
        <p class="text-lg leading-normal text-[#c7d0db]">{{ t('login.intro') }}</p>
      </div>
      <ol class="flex max-w-115 flex-col gap-4">
        <li v-for="(step, index) in STEPS" :key="step" class="flex items-baseline gap-4">
          <span class="num text-sm text-brand-300">0{{ index + 1 }}</span>
          <span class="leading-snug text-[#e4e9ef]">{{ t(step) }}</span>
        </li>
      </ol>
    </section>

    <section class="flex flex-[1_1_440px] flex-col px-6 py-6">
      <LanguageSwitch class="self-end" />
      <form class="m-auto flex w-full max-w-100 flex-col gap-6 py-8" @submit.prevent="submit">
        <div class="flex flex-col gap-2">
          <h2 class="text-[28px] font-bold tracking-tight">{{ t('login.title') }}</h2>
          <p class="text-[15px] leading-normal text-muted">{{ t('login.demoNote') }}</p>
        </div>
        <div class="flex flex-col gap-2">
          <label for="username" class="text-sm font-semibold">{{ t('login.username') }}</label>
          <input
            id="username"
            v-model="username"
            required
            minlength="3"
            maxlength="30"
            pattern="[a-zA-Z0-9._\-]+"
            autocomplete="username"
            aria-describedby="username-hint"
            class="h-12 w-full rounded-lg border border-line-strong bg-white px-3.5 text-base focus:border-brand-600 focus:outline-none"
          />
          <span id="username-hint" class="text-[13px] text-muted">{{ t('login.usernameHint') }}</span>
        </div>
        <p v-if="error" class="alert" role="alert">{{ errorMessage(error) }}</p>
        <button type="submit" :disabled="submitting" class="btn btn-primary h-12 text-base">
          {{ submitting ? t('login.submitting') : t('login.submit') }}
        </button>
      </form>
    </section>
  </div>
</template>
