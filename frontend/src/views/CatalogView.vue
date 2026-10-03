<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import type { Product } from '@/api/types'
import ProductCard from '@/components/ProductCard.vue'
import { useCartStore } from '@/stores/cart'

const cart = useCartStore()
const products = ref<Product[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    products.value = await api.products()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <h1 class="text-2xl font-bold">Catálogo</h1>
    <p v-if="loading" class="mt-6 text-slate-500">Cargando productos…</p>
    <p v-else-if="error" class="mt-6 text-rose-700">{{ error }}</p>
    <div v-else class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <ProductCard
        v-for="product in products"
        :key="product.sku"
        :product="product"
        :in-cart="cart.quantityOf(product.sku)"
        @add="cart.add"
      />
    </div>
  </section>
</template>
