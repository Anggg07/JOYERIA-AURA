import { useEffect, useRef, type DependencyList } from 'react'

export type AbortableEffect = (signal: AbortSignal) => void | (() => void)

export default function useAbortableEffect(effect: AbortableEffect, deps: DependencyList): void {
  const controllerRef = useRef<AbortController | null>(null)

  useEffect(() => {
    controllerRef.current?.abort()
    const controller = new AbortController()
    controllerRef.current = controller
    const cleanup = effect(controller.signal)
    return () => {
      controller.abort()
      if (typeof cleanup === 'function') cleanup()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
}
