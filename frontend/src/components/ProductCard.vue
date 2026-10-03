<script setup lang="ts">
import { computed } from 'vue'
import type { Product } from '@/api/types'
import { formatPrice } from '@/utils/format'

const props = defineProps<{ product: Product; inCart: number }>()
const emit = defineEmits<{ add: [product: Product] }>()

const remaining = computed(() => props.product.availableQuantity - props.inCart)
const lowStock = computed(() => props.product.availableQuantity > 0 && props.product.availableQuantity <= 5)
</script>

<template>
  <article class="flex flex-col rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-medium uppercase tracking-wide text-slate-400">{{ product.sku }}</p>
    <h3 class="mt-1 font-semibold">{{ product.name }}</h3>
    <p class="mt-2 text-xl font-bold text-brand-700">{{ formatPrice(product.price) }}</p>
    <p class="mt-1 text-sm" :class="lowStock ? 'text-amber-700' : 'text-slate-500'">
      <template v-if="product.availableQuantity === 0">Sin stock</template>
      <template v-else>{{ product.availableQuantity }} disponibles</template>
    </p>
    <button
      class="mt-4 rounded-md bg-brand-500 px-3 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:cursor-not-allowed disabled:bg-slate-300"
      :disabled="remaining <= 0"
      @click="emit('add', product)"
    >
      {{ inCart ? `Añadir otro (${inCart} en el carrito)` : 'Añadir al carrito' }}
    </button>
  </article>
</template>
