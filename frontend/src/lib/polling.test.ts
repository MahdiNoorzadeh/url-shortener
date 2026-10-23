import { afterEach, describe, expect, it, vi } from 'vitest'
import { startPolling } from './polling'

afterEach(() => {
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe('startPolling', () => {
  it('runs immediately and repeats after the interval', async () => {
    vi.useFakeTimers()

    const task = vi.fn().mockResolvedValue(undefined)
    const stop = startPolling(task, 5_000)

    expect(task).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(4_999)
    expect(task).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(1)
    expect(task).toHaveBeenCalledTimes(2)

    stop()
  })

  it('does not overlap a task that is still running', async () => {
    vi.useFakeTimers()

    let resolveTask!: () => void
    const task = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveTask = resolve
        }),
    )

    const stop = startPolling(task, 5_000)

    await vi.advanceTimersByTimeAsync(10_000)
    expect(task).toHaveBeenCalledTimes(1)

    resolveTask()
    await Promise.resolve()

    await vi.advanceTimersByTimeAsync(5_000)
    expect(task).toHaveBeenCalledTimes(2)

    stop()
  })

  it('does not schedule another refresh after cleanup', async () => {
    vi.useFakeTimers()

    let resolveTask!: () => void
    const task = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveTask = resolve
        }),
    )

    const stop = startPolling(task, 5_000)

    stop()
    resolveTask()

    await vi.advanceTimersByTimeAsync(10_000)

    expect(task).toHaveBeenCalledTimes(1)
  })
})
