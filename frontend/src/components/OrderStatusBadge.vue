<script setup lang="ts">
import { computed } from 'vue'
import type { OrderStatus } from '@/api/types'

const props = defineProps<{ status: OrderStatus }>()

const STYLES: Record<OrderStatus, { label: string; classes: string }> = {
  PENDING: { label: 'Pendiente', classes: 'bg-amber-100 text-amber-800' },
  CONFIRMED: { label: 'Confirmado', classes: 'bg-emerald-100 text-emerald-800' },
  REJECTED: { label: 'Rechazado', classes: 'bg-rose-100 text-rose-800' },
  CANCELLED: { label: 'Cancelado', classes: 'bg-slate-200 text-slate-700' },
}

const style = computed(() => STYLES[props.status])
</script>

<template>
  <span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium" :class="style.classes">
    <span v-if="status === 'PENDING'" class="mr-1.5 h-1.5 w-1.5 animate-pulse rounded-full bg-amber-500"></span>
    {{ style.label }}
  </span>
</template>
