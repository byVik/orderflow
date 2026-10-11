<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCartStore } from '@/features/cart/cart.store'
import { errorMessage } from '@/shared/i18n/errors'
import { catalogApi } from './catalog.api'
import type { Product } from './catalog.types'
import ProductCard from './ProductCard.vue'

const cart = useCartStore()
const { t } = useI18n()
const products = ref<Product[]>([])
const loading = ref(true)
const error = ref<unknown>(null)

const maxStock = computed(() => Math.max(0, ...products.value.map((p) => p.availableQuantity)))

onMounted(async () => {
  try {
    products.value = await catalogApi.products()
    cart.refresh(products.value)
  } catch (e) {
    error.value = e
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="page">
    <div class="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2">
      <h1 class="text-[32px] font-bold tracking-tight">{{ t('catalog.title') }}</h1>
      <p v-if="!loading && !error" class="text-[15px] text-muted">{{ t('catalog.summary', products.length) }}</p>
    </div>
    <p v-if="loading" class="mt-7 text-muted">{{ t('catalog.loading') }}</p>
    <p v-else-if="error" class="alert mt-7" role="alert">{{ errorMessage(error) }}</p>
    <div v-else class="mt-7 grid grid-cols-[repeat(auto-fill,minmax(250px,1fr))] gap-5">
      <ProductCard
        v-for="product in products"
        :key="product.sku"
        :product="product"
        :in-cart="cart.quantityOf(product.sku)"
        :max-stock="maxStock"
        @add="cart.add"
      />
    </div>
  </main>
</template>
