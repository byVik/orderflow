import { onBeforeUnmount, onMounted } from 'vue'

/**
 * Ejecuta `task` cada `intervalMs` mientras `shouldContinue()` devuelva true.
 * Se usa para refrescar pedidos PENDING hasta que el backend los confirme o rechace.
 */
export function usePolling(task: () => Promise<void>, shouldContinue: () => boolean, intervalMs = 1500) {
  let timer: ReturnType<typeof setTimeout> | undefined
  let stopped = false

  async function tick() {
    await task()
    if (!stopped && shouldContinue()) {
      timer = setTimeout(tick, intervalMs)
    }
  }

  onMounted(tick)
  onBeforeUnmount(() => {
    stopped = true
    clearTimeout(timer)
  })
}
