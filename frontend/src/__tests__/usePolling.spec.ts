import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, type Ref } from 'vue'
import { mount } from '@vue/test-utils'
import { usePolling } from '@/composables/usePolling'

/** Un composable con hooks de ciclo de vida necesita un componente que lo aloje. */
function mountPolling(task: () => Promise<void>, shouldContinue: () => boolean, maxAttempts = 40) {
  let stalled!: Ref<boolean>
  const wrapper = mount(
    defineComponent({
      setup() {
        stalled = usePolling(task, shouldContinue, { intervalMs: 1000, maxIntervalMs: 4000, maxAttempts }).stalled
        return () => null
      },
    }),
  )
  return { wrapper, stalled }
}

describe('usePolling', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('consulta al montar y espacia cada vez más las siguientes', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    mountPolling(task, () => true)
    await vi.advanceTimersByTimeAsync(0)
    expect(task).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(1000) // primera espera: 1 s
    expect(task).toHaveBeenCalledTimes(2)

    await vi.advanceTimersByTimeAsync(1000) // la segunda ya es de 1,5 s
    expect(task).toHaveBeenCalledTimes(2)
    await vi.advanceTimersByTimeAsync(500)
    expect(task).toHaveBeenCalledTimes(3)
  })

  it('se detiene cuando ya no hay nada pendiente', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    mountPolling(task, () => false)
    await vi.advanceTimersByTimeAsync(60_000)

    expect(task).toHaveBeenCalledTimes(1)
  })

  it('deja de consultar al desmontar el componente', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    const { wrapper } = mountPolling(task, () => true)
    await vi.advanceTimersByTimeAsync(0)

    wrapper.unmount()
    await vi.advanceTimersByTimeAsync(60_000)

    expect(task).toHaveBeenCalledTimes(1)
  })

  it('tras el máximo de consultas se rinde y lo indica', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    const { stalled } = mountPolling(task, () => true, 3)
    await vi.advanceTimersByTimeAsync(60_000)

    expect(task).toHaveBeenCalledTimes(3)
    expect(stalled.value).toBe(true)
  })
})
