export function startPolling(
  task: () => void | Promise<void>,
  intervalMs: number,
): () => void {
  let stopped = false
  let timeoutId: ReturnType<typeof setTimeout> | undefined

  async function run() {
    try {
      await task()
    } catch {
      // A failed refresh should not permanently stop polling.
    } finally {
      if (!stopped) {
        timeoutId = setTimeout(() => {
          void run()
        }, intervalMs)
      }
    }
  }

  void run()

  return () => {
    stopped = true

    if (timeoutId !== undefined) {
      clearTimeout(timeoutId)
    }
  }
}
