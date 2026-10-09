<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OrderStatus } from '@/api/types'
import { rejectionMessage } from '@/i18n/errors'

const props = defineProps<{ status: OrderStatus; rejectionReason?: string | null }>()
const { t } = useI18n()

type StepKind = 'done' | 'active' | 'failed' | 'idle'
interface Step {
  kind: StepKind
  /** Clave de los textos del paso: timeline.<name>.title y timeline.<name>.caption. */
  name: string
}

const RECEIVED: Step = { kind: 'done', name: 'received' }

// Los tres pasos de la saga, contados para quien compra: pedido, reserva en inventario y resultado.
const STEPS: Record<OrderStatus, Step[]> = {
  PENDING: [RECEIVED, { kind: 'active', name: 'reserving' }, { kind: 'idle', name: 'awaiting' }],
  CONFIRMED: [RECEIVED, { kind: 'done', name: 'reserved' }, { kind: 'done', name: 'confirmed' }],
  REJECTED: [RECEIVED, { kind: 'failed', name: 'notReserved' }, { kind: 'failed', name: 'rejected' }],
  CANCELLED: [RECEIVED, { kind: 'idle', name: 'voided' }, { kind: 'failed', name: 'cancelled' }],
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
  <section class="card p-6" :aria-label="t('timeline.label')">
    <!-- En móvil los pasos se apilan (icono a la izquierda); desde md van en fila unidos por una línea. -->
    <ol class="flex flex-col gap-5 md:flex-row md:gap-0">
      <li v-for="(step, index) in steps" :key="step.name" class="flex gap-3 md:flex-1 md:flex-col" :data-kind="step.kind">
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
          <span class="text-[15px] font-semibold" :class="step.kind === 'idle' ? 'text-muted' : 'text-ink'">{{ t(`timeline.${step.name}.title`) }}</span>
          <span class="text-[13px] leading-snug text-muted">{{ t(`timeline.${step.name}.caption`) }}</span>
        </div>
      </li>
    </ol>
    <p v-if="rejectionReason" class="alert mt-5">{{ rejectionMessage(rejectionReason) }}</p>
  </section>
</template>
