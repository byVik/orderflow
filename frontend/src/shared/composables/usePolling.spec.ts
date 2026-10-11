import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, type Ref } from 'vue'
import { mount } from '@vue/test-utils'
import { usePolling } from './usePolling'

/** A composable with lifecycle hooks needs a component to host it. */
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

  it('runs on mount and spaces the following requests further apart', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    mountPolling(task, () => true)
    await vi.advanceTimersByTimeAsync(0)
    expect(task).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(1000) // first wait: 1 s
    expect(task).toHaveBeenCalledTimes(2)

    await vi.advanceTimersByTimeAsync(1000) // the second one is already 1.5 s
    expect(task).toHaveBeenCalledTimes(2)
    await vi.advanceTimersByTimeAsync(500)
    expect(task).toHaveBeenCalledTimes(3)
  })

  it('stops when nothing is pending any more', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    mountPolling(task, () => false)
    await vi.advanceTimersByTimeAsync(60_000)

    expect(task).toHaveBeenCalledTimes(1)
  })

  it('stops polling when the component unmounts', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    const { wrapper } = mountPolling(task, () => true)
    await vi.advanceTimersByTimeAsync(0)

    wrapper.unmount()
    await vi.advanceTimersByTimeAsync(60_000)

    expect(task).toHaveBeenCalledTimes(1)
  })

  it('gives up after the maximum number of requests and reports it', async () => {
    const task = vi.fn().mockResolvedValue(undefined)
    const { stalled } = mountPolling(task, () => true, 3)
    await vi.advanceTimersByTimeAsync(60_000)

    expect(task).toHaveBeenCalledTimes(3)
    expect(stalled.value).toBe(true)
  })
})
