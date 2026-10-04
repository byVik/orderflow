<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const error = ref<string | null>(null)
const submitting = ref(false)

const STEPS = [
  'Creas el pedido y queda pendiente.',
  'Inventario reserva el stock: todo o nada.',
  'El pedido se confirma, o se rechaza con el motivo.',
]

async function submit() {
  error.value = null
  submitting.value = true
  try {
    await auth.login(username.value.trim())
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/catalog'
    router.push(redirect)
  } catch (e) {
    error.value = (e as Error).message
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
        <h1 class="text-4xl leading-[1.1] font-bold tracking-tight sm:text-[44px]">Haz un pedido y mira cómo se confirma.</h1>
        <p class="text-lg leading-normal text-[#c7d0db]">
          El stock se reserva en segundo plano. El pedido pasa de pendiente a confirmado o rechazado en un par de segundos.
        </p>
      </div>
      <ol class="flex max-w-115 flex-col gap-4">
        <li v-for="(step, index) in STEPS" :key="step" class="flex items-baseline gap-4">
          <span class="num text-sm text-brand-300">0{{ index + 1 }}</span>
          <span class="leading-snug text-[#e4e9ef]">{{ step }}</span>
        </li>
      </ol>
    </section>

    <section class="flex flex-[1_1_440px] items-center justify-center px-6 py-14">
      <form class="flex w-full max-w-100 flex-col gap-6" @submit.prevent="submit">
        <div class="flex flex-col gap-2">
          <h2 class="text-[28px] font-bold tracking-tight">Entrar</h2>
          <p class="text-[15px] leading-normal text-muted">
            Entorno de demostración: escribe cualquier nombre de usuario. No hay contraseña.
          </p>
        </div>
        <div class="flex flex-col gap-2">
          <label for="username" class="text-sm font-semibold">Usuario</label>
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
          <span id="username-hint" class="text-[13px] text-muted">De 3 a 30 caracteres: letras, números, punto y guion.</span>
        </div>
        <p v-if="error" class="alert" role="alert">{{ error }}</p>
        <button type="submit" :disabled="submitting" class="btn btn-primary h-12 text-base">
          {{ submitting ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>
    </section>
  </div>
</template>
