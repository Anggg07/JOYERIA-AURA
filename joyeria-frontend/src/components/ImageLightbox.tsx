import { useCallback, useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'
import { X, ZoomIn, ZoomOut } from 'lucide-react'
import SmartImage from './SmartImage'

const MIN_SCALE = 1
const MAX_SCALE = 4
const ZOOM_STEP = 0.6
const FOCUSABLE = 'button:not([disabled]), [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'

const clampScale = (value: number) => Math.min(MAX_SCALE, Math.max(MIN_SCALE, value))

interface ImageLightboxProps {
  src?: string | null
  alt: string
  isOpen: boolean
  onClose: () => void
  /** Texto opcional bajo la imagen (p. ej. el nombre de la pieza o colección). */
  caption?: string
}

/**
 * Visor de imagen ampliada reutilizable: fondo oscurecido, cierre con Esc o
 * clic fuera, bloqueo de scroll, restauración de foco y foco atrapado, además
 * de zoom (botones, rueda, doble clic) y gesto de pinza/arrastre en táctil.
 */
export default function ImageLightbox({ src, alt, isOpen, onClose, caption }: ImageLightboxProps) {
  const reduceMotion = useReducedMotion()
  const [scale, setScale] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const stageRef = useRef<HTMLDivElement>(null)
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeRef = useRef<HTMLButtonElement>(null)
  const restoreFocusRef = useRef<HTMLElement | null>(null)
  const pointers = useRef(new Map<number, { x: number; y: number }>())
  const pinch = useRef<{ distance: number; scale: number } | null>(null)
  const drag = useRef<{ id: number; startX: number; startY: number; originX: number; originY: number } | null>(null)

  const reset = useCallback(() => {
    setScale(1)
    setOffset({ x: 0, y: 0 })
  }, [])

  const zoomBy = useCallback((delta: number) => {
    setScale((current) => {
      const next = clampScale(current + delta)
      if (next === 1) setOffset({ x: 0, y: 0 })
      return next
    })
  }, [])

  const pointerDistance = () => {
    const values = Array.from(pointers.current.values())
    if (values.length < 2) return 0
    return Math.hypot(values[0].x - values[1].x, values[0].y - values[1].y) || 1
  }

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.currentTarget.setPointerCapture?.(event.pointerId)
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY })
    if (pointers.current.size === 2) {
      drag.current = null
      pinch.current = { distance: pointerDistance(), scale }
    } else if (pointers.current.size === 1 && scale > MIN_SCALE) {
      drag.current = { id: event.pointerId, startX: event.clientX, startY: event.clientY, originX: offset.x, originY: offset.y }
    }
  }

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!pointers.current.has(event.pointerId)) return
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY })
    if (pointers.current.size >= 2 && pinch.current) {
      event.preventDefault()
      const next = clampScale(pinch.current.scale * (pointerDistance() / pinch.current.distance))
      if (next === 1) setOffset({ x: 0, y: 0 })
      setScale(next)
    } else if (drag.current && drag.current.id === event.pointerId) {
      event.preventDefault()
      setOffset({
        x: drag.current.originX + (event.clientX - drag.current.startX),
        y: drag.current.originY + (event.clientY - drag.current.startY),
      })
    }
  }

  const handlePointerUp = (event: ReactPointerEvent<HTMLDivElement>) => {
    pointers.current.delete(event.pointerId)
    if (pointers.current.size < 2) pinch.current = null
    if (drag.current?.id === event.pointerId) drag.current = null
  }

  useEffect(() => {
    if (!isOpen) return
    reset()
    restoreFocusRef.current = document.activeElement as HTMLElement | null
    document.body.style.overflow = 'hidden'
    const focusTimer = window.setTimeout(() => closeRef.current?.focus(), 0)

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
        return
      }
      if (event.key === '+' || event.key === '=') {
        event.preventDefault()
        zoomBy(ZOOM_STEP)
        return
      }
      if (event.key === '-' || event.key === '_') {
        event.preventDefault()
        zoomBy(-ZOOM_STEP)
        return
      }
      if (event.key === '0') {
        event.preventDefault()
        reset()
        return
      }
      if (event.key !== 'Tab' || !dialogRef.current) return
      const focusables = Array.from(dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE))
      if (focusables.length === 0) return
      const first = focusables[0]
      const last = focusables[focusables.length - 1]
      const active = document.activeElement
      if (event.shiftKey && active === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && active === last) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => {
      window.clearTimeout(focusTimer)
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = ''
      restoreFocusRef.current?.focus?.()
    }
  }, [isOpen, onClose, zoomBy, reset])

  useEffect(() => {
    if (!isOpen) return
    const stage = stageRef.current
    if (!stage) return
    const onWheel = (event: WheelEvent) => {
      event.preventDefault()
      zoomBy(event.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP)
    }
    stage.addEventListener('wheel', onWheel, { passive: false })
    return () => stage.removeEventListener('wheel', onWheel)
  }, [isOpen, zoomBy])

  const transform = `translate(${offset.x}px, ${offset.y}px) scale(${scale})`

  return (
    <AnimatePresence>
      {isOpen && (
        <div
          ref={dialogRef}
          role="dialog"
          aria-modal="true"
          aria-label={`Vista ampliada: ${alt}`}
          className="fixed inset-0 z-[130] flex items-center justify-center p-4 sm:p-8"
        >
          <motion.div
            className="absolute inset-0 bg-black/85 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.25 }}
            onClick={onClose}
            aria-hidden="true"
          />

          <button
            ref={closeRef}
            type="button"
            onClick={onClose}
            aria-label="Cerrar vista ampliada"
            className="absolute right-4 top-4 z-20 inline-flex h-11 w-11 items-center justify-center rounded-full border border-white/20 bg-white/10 text-white transition hover:bg-white/20"
          >
            <X size={22} />
          </button>

          <motion.div
            className="relative z-10 flex max-h-full max-w-full flex-col items-center"
            initial={reduceMotion ? { opacity: 0 } : { opacity: 0, scale: 0.94 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={reduceMotion ? { opacity: 0 } : { opacity: 0, scale: 0.94 }}
            transition={{ duration: reduceMotion ? 0 : 0.3, ease: [0.22, 1, 0.36, 1] }}
          >
            <div
              ref={stageRef}
              onPointerDown={handlePointerDown}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              onPointerCancel={handlePointerUp}
              onDoubleClick={() => (scale === 1 ? setScale(2) : reset())}
              className="relative touch-none overscroll-contain select-none"
              style={{ cursor: scale > 1 ? 'grab' : 'zoom-in' }}
            >
              <SmartImage
                src={src}
                alt={alt}
                eager
                draggable={false}
                className="max-h-[82vh] w-auto max-w-[92vw] rounded-md object-contain shadow-2xl"
                style={{ transform, transition: reduceMotion ? 'none' : 'transform 0.2s ease-out' }}
              />
            </div>

            <div className="mt-5 flex items-center gap-2">
              <button
                type="button"
                onClick={() => zoomBy(-ZOOM_STEP)}
                aria-label="Alejar"
                className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-white/20 bg-white/10 text-white transition hover:bg-white/20"
              >
                <ZoomOut size={18} />
              </button>
              <button
                type="button"
                onClick={reset}
                aria-label="Restablecer zoom"
                className="min-w-[4.5rem] rounded-full border border-white/20 bg-white/10 px-3 py-2 text-xs font-medium tracking-wide text-white transition hover:bg-white/20"
              >
                {Math.round(scale * 100)}%
              </button>
              <button
                type="button"
                onClick={() => zoomBy(ZOOM_STEP)}
                aria-label="Acercar"
                className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-white/20 bg-white/10 text-white transition hover:bg-white/20"
              >
                <ZoomIn size={18} />
              </button>
            </div>

            {caption && (
              <p className="mt-3 max-w-[80vw] truncate text-center font-serif text-sm text-white/80">{caption}</p>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
