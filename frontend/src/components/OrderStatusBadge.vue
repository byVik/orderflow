<script setup lang="ts">
import { computed } from 'vue'
import type { OrderStatus } from '@/api/types'

const props = defineProps<{ status: OrderStatus }>()

// Los estados se distinguen también por la forma del punto, no solo por el color.
const STYLES: Record<OrderStatus, { label: string; pill: string; dot: string }> = {
  PENDING: { label: 'Pendiente', pill: 'bg-warn-50 text-warn-800', dot: 'rounded-full bg-warn-800 animate-pulse motion-reduce:animate-none' },
  CONFIRMED: { label: 'Confirmado', pill: 'bg-brand-50 text-brand-700', dot: 'rounded-full bg-brand-700' },
  REJECTED: { label: 'Rechazado', pill: 'bg-danger-50 text-danger-700', dot: 'rounded-[2px] bg-danger-700' },
  CANCELLED: { label: 'Cancelado', pill: 'bg-chip text-chip-ink', dot: 'rounded-[2px] bg-chip-ink' },
}

const style = computed(() => STYLES[props.status])
</script>

<template>
  <span class="inline-flex h-7 items-center gap-2 rounded-full px-3 text-[13px] font-semibold" :class="style.pill">
    <span class="h-2 w-2" :class="style.dot" aria-hidden="true"></span>
    {{ style.label }}
  </span>
</template>
