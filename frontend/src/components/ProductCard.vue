<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Product } from '@/api/types'
import { MAX_QUANTITY_PER_ITEM } from '@/stores/cart'
import { formatPrice } from '@/utils/format'

const props = withDefaults(defineProps<{ product: Product; inCart: number; maxStock?: number }>(), { maxStock: 0 })
const emit = defineEmits<{ add: [product: Product] }>()
const { t } = useI18n()

// El mismo límite que aplica el carrito: stock disponible y máximo por línea.
const remaining = computed(() => Math.min(props.product.availableQuantity, MAX_QUANTITY_PER_ITEM) - props.inCart)
const outOfStock = computed(() => props.product.availableQuantity === 0)
const lowStock = computed(() => !outOfStock.value && props.product.availableQuantity <= 5)
const stockBar = computed(() => {
  const scale = Math.max(props.maxStock, props.product.availableQuantity, 1)
  return `${Math.round((props.product.availableQuantity / scale) * 100)}%`
})
</script>

<template>
  <article class="card flex flex-col gap-3.5 p-5">
    <!-- Alturas mínimas fijas: precios y botones quedan alineados entre tarjetas. -->
    <div class="flex min-h-6 items-center justify-between gap-3">
      <span class="num text-xs tracking-wider text-muted">{{ product.sku }}</span>
      <span v-if="lowStock" class="rounded-md bg-warn-50 px-2 py-0.5 text-xs font-semibold text-warn-800">{{ t('product.lastUnits') }}</span>
    </div>
    <h3 class="min-h-12 text-[17px] leading-snug font-semibold">{{ product.name }}</h3>
    <p class="num text-2xl font-medium tracking-tight">{{ formatPrice(product.price) }}</p>
    <div class="flex flex-col gap-1.5">
      <div class="h-1 overflow-hidden rounded-full bg-chip" aria-hidden="true">
        <div class="h-1 rounded-full" :class="lowStock ? 'bg-warn-600' : 'bg-brand-600'" :style="{ width: stockBar }"></div>
      </div>
      <span class="text-[13px] text-muted">
        <template v-if="outOfStock">{{ t('product.outOfStock') }}</template>
        <template v-else>{{ t('product.available', product.availableQuantity) }}</template>
      </span>
    </div>
    <button
      type="button"
      class="btn mt-auto"
      :class="inCart ? 'btn-primary' : 'btn-outline'"
      :disabled="remaining <= 0"
      @click="emit('add', product)"
    >
      {{ inCart ? t('product.addAnother') : t('product.add') }}
      <span
        v-if="inCart"
        class="num min-w-5.5 rounded-full bg-white px-1.5 text-center text-[13px] leading-5.5 font-medium text-brand-700"
        :aria-label="t('product.inCart', { count: inCart })"
      >{{ inCart }}</span>
    </button>
  </article>
</template>
