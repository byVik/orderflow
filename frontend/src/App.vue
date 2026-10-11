<script setup lang="ts">
import { useAuthStore } from '@/features/auth/auth.store'
import AppHeader from '@/shared/components/AppHeader.vue'

const auth = useAuthStore()
</script>

<template>
  <AppHeader v-if="auth.isAuthenticated" />
  <RouterView />
</template>
