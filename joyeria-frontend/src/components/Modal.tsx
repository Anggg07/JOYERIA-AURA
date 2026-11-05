import { type ReactNode, useEffect, useId, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X } from 'lucide-react'

interface Props { isOpen: boolean; onClose: () => void; title?: string; children: ReactNode; maxWidth?: string }

const FOCUSABLE =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'

export default function Modal({ isOpen, onClose, title, children, maxWidth = 'max-w-lg' }: Props) {
  const panelRef = useRef<HTMLDivElement>(null)
  const titleId = useId()

  useEffect(() => {
    if (!isOpen) return
    const previouslyFocused = document.activeElement as HTMLElement | null
    document.body.style.overflow = 'hidden'

    const getFocusable = () =>
      panelRef.current ? Array.from(panelRef.current.querySelectorAll<HTMLElement>(FOCUSABLE)) : []

    const focusTimer = window.setTimeout(() => {
      const items = getFocusable()
      ;(items[0] ?? panelRef.current)?.focus()
    }, 0)

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation()
        onClose()
        return
      }
      if (e.key !== 'Tab') return
      const items = getFocusable()
      if (items.length === 0) {
        e.preventDefault()
        panelRef.current?.focus()
        return
      }
      const first = items[0]
      const last = items[items.length - 1]
      const active = document.activeElement
      if (e.shiftKey && (active === first || !panelRef.current?.contains(active))) {
        e.preventDefault()
        last.focus()
      } else if (!e.shiftKey && active === last) {
        e.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => {
      window.clearTimeout(focusTimer)
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = ''
      previouslyFocused?.focus?.()
    }
  }, [isOpen, onClose])

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <motion.div className="absolute inset-0 bg-black/40" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose} />
          <motion.div ref={panelRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby={title ? titleId : undefined}
            aria-label={title ? undefined : 'Diálogo'}
            tabIndex={-1}
            className={`relative bg-surface rounded-lg shadow-xl ${maxWidth} w-full max-h-[90vh] overflow-y-auto outline-none`}
            initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}>
            <div className="flex items-center justify-between p-5 border-b border-line/70">
              {title && <h2 id={titleId} className="text-lg font-serif font-medium text-foreground">{title}</h2>}
              <button onClick={onClose} aria-label="Cerrar" className="text-foreground-faint hover:text-foreground-muted p-1"><X size={20} /></button>
            </div>
            <div className="p-5">{children}</div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
