import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { motion } from 'framer-motion'

interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onAnimationStart' | 'onAnimationEnd' | 'onDrag' | 'onDragEnd' | 'onDragStart'> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  children: ReactNode
}

const variants = {
  primary: 'bg-[#C9A227] hover:bg-[#b8911f] text-white shadow-sm',
  secondary: 'bg-[#6F8F72] hover:bg-[#5a7a5d] text-white',
  outline: 'border-2 border-[#C9A227] text-[#C9A227] hover:bg-[#C9A227] hover:text-white',
  ghost: 'text-foreground-muted hover:bg-surface-muted',
  danger: 'bg-red-500 hover:bg-red-600 text-white',
}

const sizes = { sm: 'min-h-11 px-3 py-1.5 text-xs', md: 'px-5 py-2.5 text-sm', lg: 'px-8 py-3.5 text-base' }

export default function Button({ variant = 'primary', size = 'md', loading, disabled, children, className = '', ...props }: ButtonProps) {
  return (
    <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}
      className={`font-medium tracking-wide rounded transition-all duration-200 inline-flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={disabled || loading} {...props}>
      {loading && <span className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />}
      {children}
    </motion.button>
  )
}
