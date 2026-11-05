import { motion, AnimatePresence, useReducedMotion } from 'framer-motion'
import { Sun, Moon } from 'lucide-react'
import { useTheme } from '../contexts/ThemeContext'

interface Props {
  className?: string
  label?: string
  withText?: boolean
}

export default function ThemeToggle({ className = '', label = 'Cambiar tema', withText = false }: Props) {
  const { theme, toggleTheme } = useTheme()
  const reduced = useReducedMotion()
  const isDark = theme === 'dark'

  return (
    <button
      onClick={toggleTheme}
      aria-label={label}
      aria-pressed={isDark}
      title={label}
      className={`inline-flex items-center gap-2 p-2 ${withText ? 'text-sm font-medium text-foreground-muted' : 'text-foreground-muted'} hover:text-primary transition-colors ${className}`}>
      <span className="relative overflow-hidden">
        <AnimatePresence initial={false} mode="wait">
          <motion.span
            key={theme}
            initial={reduced ? false : { rotate: -90, scale: 0.4, opacity: 0 }}
            animate={{ rotate: 0, scale: 1, opacity: 1 }}
            exit={reduced ? undefined : { rotate: 90, scale: 0.4, opacity: 0 }}
            transition={{ duration: 0.25 }}
            className="flex items-center justify-center">
            {isDark ? <Sun size={19} /> : <Moon size={19} />}
          </motion.span>
        </AnimatePresence>
      </span>
      {withText && <span>{isDark ? 'Modo claro' : 'Modo oscuro'}</span>}
    </button>
  )
}