<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <AppHeader v-if="auth.isAuthenticated" />
  <main class="mx-auto max-w-5xl px-4 py-8">
    <RouterView />
  </main>
</template>
