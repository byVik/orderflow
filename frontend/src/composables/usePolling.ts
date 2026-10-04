import { onBeforeUnmount, onMounted, ref } from 'vue'

interface PollingOptions {
  /** Espera antes de la segunda consulta. */
  intervalMs?: number
  /** Cada espera es un 50 % más larga que la anterior, hasta este tope. */
  maxIntervalMs?: number
  /** Número de consultas tras el cual se deja de preguntar y `stalled` pasa a true. */
  maxAttempts?: number
}

/**
 * Ejecuta `task` repetidamente mientras `shouldContinue()` devuelva true, espaciando cada vez más
 * las consultas. Se usa para refrescar pedidos PENDING hasta que el backend los confirme o rechace.
 */
export function usePolling(task: () => Promise<void>, shouldContinue: () => boolean, options: PollingOptions = {}) {
  const { intervalMs = 1500, maxIntervalMs = 10_000, maxAttempts = 40 } = options
  const stalled = ref(false)

  let timer: ReturnType<typeof setTimeout> | undefined
  let stopped = false
  let attempts = 0
  let delay = intervalMs

  async function tick() {
    await task()
    attempts++
    if (stopped || !shouldContinue()) return
    if (attempts >= maxAttempts) {
      stalled.value = true
      return
    }
    // setTimeout encadenado: la siguiente consulta no empieza hasta que termina la anterior.
    timer = setTimeout(tick, delay)
    delay = Math.min(Math.round(delay * 1.5), maxIntervalMs)
  }

  onMounted(tick)
  onBeforeUnmount(() => {
    stopped = true
    clearTimeout(timer)
  })

  return { stalled }
}
