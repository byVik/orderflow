<script setup lang="ts">
import { computed } from 'vue'
import type { OrderStatus } from '@/api/types'

const props = defineProps<{ status: OrderStatus; rejectionReason?: string | null }>()

type StepKind = 'done' | 'active' | 'failed' | 'idle'
interface Step {
  kind: StepKind
  title: string
  caption: string
}

const RECEIVED: Step = { kind: 'done', title: 'Pedido recibido', caption: 'Guardado con los precios del catálogo.' }

// Los tres pasos de la saga, contados para quien compra: pedido, reserva en inventario y resultado.
const STEPS: Record<OrderStatus, Step[]> = {
  PENDING: [
    RECEIVED,
    { kind: 'active', title: 'Reservando stock', caption: 'Inventario está comprobando las existencias.' },
    { kind: 'idle', title: 'Confirmación', caption: 'Llegará en cuanto termine la reserva.' },
  ],
  CONFIRMED: [
    RECEIVED,
    { kind: 'done', title: 'Stock reservado', caption: 'Todas las líneas tenían existencias.' },
    { kind: 'done', title: 'Pedido confirmado', caption: 'No hay nada más que hacer.' },
  ],
  REJECTED: [
    RECEIVED,
    { kind: 'failed', title: 'Stock no reservado', caption: 'Faltaba una línea y no se reservó ninguna.' },
    { kind: 'failed', title: 'Pedido rechazado', caption: 'No se ha descontado stock.' },
  ],
  CANCELLED: [
    RECEIVED,
    { kind: 'idle', title: 'Reserva anulada', caption: 'Si ya se había reservado, el stock se devuelve.' },
    { kind: 'failed', title: 'Pedido cancelado', caption: 'Lo cancelaste antes de confirmarse.' },
  ],
}

const CIRCLE: Record<StepKind, string> = {
  done: 'border-brand-600 bg-brand-600',
  failed: 'border-danger-700 bg-danger-700',
  active: 'border-warn-600 bg-white',
  idle: 'border-line-strong bg-white',
}

const steps = computed(() => STEPS[props.status])
</script>

<template>
  <section class="card p-6" aria-label="Estado del pedido">
    <!-- En móvil los pasos se apilan (icono a la izquierda); desde md van en fila unidos por una línea. -->
    <ol class="flex flex-col gap-5 md:flex-row md:gap-0">
      <li v-for="(step, index) in steps" :key="step.title" class="flex gap-3 md:flex-1 md:flex-col" :data-kind="step.kind">
        <div class="flex items-center gap-3 self-start md:self-stretch" aria-hidden="true">
          <span class="flex h-8 w-8 flex-none items-center justify-center rounded-full border-2" :class="CIRCLE[step.kind]">
            <svg v-if="step.kind === 'done'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <path d="M5 12.5l4.5 4.5L19 7.5" />
            </svg>
            <svg v-else-if="step.kind === 'failed'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round">
              <path d="M6 6l12 12" />
              <path d="M18 6L6 18" />
            </svg>
            <span v-else-if="step.kind === 'active'" class="h-2.5 w-2.5 animate-pulse rounded-full bg-warn-600 motion-reduce:animate-none"></span>
          </span>
          <span
            v-if="index < steps.length - 1"
            class="mr-3 hidden h-0.5 flex-1 md:block"
            :class="step.kind === 'done' ? 'bg-brand-600' : 'bg-line'"
          ></span>
        </div>
        <div class="flex flex-col gap-1 md:pr-4">
          <span class="text-[15px] font-semibold" :class="step.kind === 'idle' ? 'text-muted' : 'text-ink'">{{ step.title }}</span>
          <span class="text-[13px] leading-snug text-muted">{{ step.caption }}</span>
        </div>
      </li>
    </ol>
    <p v-if="rejectionReason" class="alert mt-5">{{ rejectionReason }}</p>
  </section>
</template>
