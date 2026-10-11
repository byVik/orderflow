import { onBeforeUnmount, onMounted, ref } from 'vue'

interface PollingOptions {
  /** Wait before the second request. */
  intervalMs?: number
  /** Each wait is 50% longer than the previous one, up to this cap. */
  maxIntervalMs?: number
  /** Number of requests after which polling stops and `stalled` becomes true. */
  maxAttempts?: number
}

/**
 * Runs `task` repeatedly while `shouldContinue()` returns true, spacing the requests further
 * apart each time. Used to refresh PENDING orders until the backend confirms or rejects them.
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
    // Chained setTimeout: the next request does not start until the previous one has finished.
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
