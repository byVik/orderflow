import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '@/api/client'

const STORAGE_KEY = 'orderflow.session'

interface Session {
  username: string
  token: string
  expiresAt: number
}

function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as Session
    return session.expiresAt > Date.now() ? session : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<Session | null>(loadSession())

  const isAuthenticated = computed(() => session.value !== null)
  const username = computed(() => session.value?.username ?? '')
  const token = computed(() => session.value?.token ?? null)

  async function login(name: string) {
    const { accessToken, expiresIn } = await api.login(name)
    session.value = { username: name, token: accessToken, expiresAt: Date.now() + expiresIn * 1000 }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session.value))
  }

  function logout() {
    session.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  return { isAuthenticated, username, token, login, logout }
})
