import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react'

interface Toast {
  id: string; message: string; type: 'success' | 'error' | 'info'
}

interface ToastContextType {
  toast: (message: string, type?: 'success' | 'error' | 'info') => void
}

const ToastContext = createContext<ToastContextType>({} as ToastContextType)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const toast = useCallback((message: string, type: 'success' | 'error' | 'info' = 'success') => {
    const id = Math.random().toString(36).substring(2, 9)
    setToasts(prev => [...prev, { id, message, type }])
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 3500)
  }, [])

  const remove = (id: string) => setToasts(prev => prev.filter(t => t.id !== id))

  const iconMap = { success: <CheckCircle size={18} className="text-green-500" />, error: <AlertCircle size={18} className="text-red-500" />, info: <Info size={18} className="text-blue-500" /> }

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="fixed top-24 right-4 z-[9999] flex flex-col gap-2">
        <AnimatePresence>
          {toasts.map(t => (
            <motion.div key={t.id} initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40 }}
              className="bg-surface border border-line shadow-lg rounded-lg px-4 py-3 flex items-center gap-3 w-[min(24rem,calc(100vw-2rem))] max-w-[400px]">
              {iconMap[t.type]}
              <p className="text-sm text-foreground flex-1">{t.message}</p>
              <button onClick={() => remove(t.id)} className="text-foreground-faint hover:text-foreground-muted"><X size={14} /></button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)