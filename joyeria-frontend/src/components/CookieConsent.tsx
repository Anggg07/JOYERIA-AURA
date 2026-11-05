import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Cookie } from 'lucide-react'
import Button from './Button'

export default function CookieConsent() {
  const [show, setShow] = useState(false)

  useEffect(() => {
    const consent = localStorage.getItem('cookieConsent')
    if (!consent) setShow(true)
  }, [])

  const accept = (type: 'all' | 'necessary') => {
    const consent = { type, ts: Date.now(), version: 1 }
    localStorage.setItem('cookieConsent', JSON.stringify(consent))
    window.dispatchEvent(new CustomEvent('cookie-consent', { detail: consent }))
    setShow(false)
  }

  return (
    <AnimatePresence>
      {show && (
        <motion.div initial={{ y: 100, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 100, opacity: 0 }}
          className="fixed bottom-0 left-0 right-0 z-[90] bg-surface border-t border-line shadow-lg">
          <div className="max-w-6xl mx-auto px-4 py-5 flex flex-col md:flex-row items-start md:items-center gap-4">
            <Cookie size={28} className="text-primary shrink-0 mt-1" />
            <div className="flex-1">
              <h4 className="font-serif font-medium text-foreground mb-1">Uso de cookies</h4>
              <p className="text-sm text-foreground-faint">Utilizamos cookies para mejorar tu experiencia. Puedes configurar tus preferencias o aceptar todas.</p>
            </div>
            <div className="flex gap-2 shrink-0">
              <Button variant="ghost" size="sm" onClick={() => accept('necessary')}>Solo necesarias</Button>
              <Button variant="outline" size="sm" onClick={() => accept('all')}>Aceptar todas</Button>
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
