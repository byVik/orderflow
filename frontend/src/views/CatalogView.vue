<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/api/client'
import type { Product } from '@/api/types'
import ProductCard from '@/components/ProductCard.vue'
import { useCartStore } from '@/stores/cart'

const cart = useCartStore()
const products = ref<Product[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const maxStock = computed(() => Math.max(0, ...products.value.map((p) => p.availableQuantity)))

onMounted(async () => {
  try {
    products.value = await api.products()
    cart.refresh(products.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="page">
    <div class="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2">
      <h1 class="text-[32px] font-bold tracking-tight">Catálogo</h1>
      <p v-if="!loading && !error" class="text-[15px] text-muted">
        {{ products.length }} productos · el stock se reserva al confirmar el pedido
      </p>
    </div>
    <p v-if="loading" class="mt-7 text-muted">Cargando productos…</p>
    <p v-else-if="error" class="alert mt-7" role="alert">{{ error }}</p>
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
