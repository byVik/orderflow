<script setup lang="ts">
withDefaults(defineProps<{ size?: number; tone?: 'brand' | 'light' }>(), { size: 26, tone: 'brand' })
</script>

<template>
  <!-- Dos flechas opuestas: el pedido va a inventario y el resultado vuelve. -->
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 32 32"
    fill="none"
    stroke="currentColor"
    stroke-width="2.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    :class="tone === 'light' ? 'text-brand-300' : 'text-brand-600'"
  >
    <path d="M5 11h17l-4-4" />
    <path d="M27 21H10l4 4" />
  </svg>
</template>
