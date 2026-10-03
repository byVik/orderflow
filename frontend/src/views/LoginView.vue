<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const error = ref<string | null>(null)
const submitting = ref(false)

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
  <div class="mx-auto mt-16 max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
    <h1 class="text-2xl font-bold text-brand-700">OrderFlow</h1>
    <p class="mt-1 text-sm text-slate-500">Demo: entra con cualquier nombre de usuario (3–30 caracteres).</p>
    <form class="mt-6 space-y-4" @submit.prevent="submit">
      <label class="block">
        <span class="text-sm font-medium">Usuario</span>
        <input
          v-model="username"
          required
          minlength="3"
          maxlength="30"
          pattern="[a-zA-Z0-9._\-]+"
          autocomplete="username"
          class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 focus:border-brand-500 focus:outline-none"
        />
      </label>
      <p v-if="error" class="text-sm text-rose-700">{{ error }}</p>
      <button
        type="submit"
        :disabled="submitting"
        class="w-full rounded-md bg-brand-500 py-2 font-medium text-white hover:bg-brand-600 disabled:opacity-60"
      >
        {{ submitting ? 'Entrando…' : 'Entrar' }}
      </button>
    </form>
  </div>
</template>
