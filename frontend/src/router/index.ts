import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/features/auth/auth.store'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/catalog' },
    { path: '/login', name: 'login', component: () => import('@/features/auth/LoginView.vue'), meta: { public: true } },
    { path: '/catalog', name: 'catalog', component: () => import('@/features/catalog/CatalogView.vue') },
    { path: '/cart', name: 'cart', component: () => import('@/features/cart/CartView.vue') },
    { path: '/orders', name: 'orders', component: () => import('@/features/orders/OrdersView.vue') },
    { path: '/orders/:id', name: 'order', component: () => import('@/features/orders/OrderDetailView.vue'), props: true },
    { path: '/:pathMatch(.*)*', redirect: '/catalog' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'catalog' }
  }
})
