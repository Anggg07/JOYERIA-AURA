import { Star } from 'lucide-react'

interface RatingProps {
  value?: number
  count?: number
  size?: number
  showValue?: boolean
  className?: string
}

export default function Rating({ value, count, size = 13, showValue = true, className = '' }: RatingProps) {
  if (!value && value !== 0) return null
  const pct = Math.max(0, Math.min(100, (value / 5) * 100))

  return (
    <div className={`flex items-center gap-1.5 ${className}`} aria-label={`Calificación ${value} de 5`}>
      <div className="relative inline-flex" style={{ width: size * 5 + 4, height: size }}>
        <div className="flex gap-0.5 text-foreground/20">
          {Array.from({ length: 5 }).map((_, i) => <Star key={i} size={size} fill="currentColor" strokeWidth={0} />)}
        </div>
        <div className="absolute inset-0 overflow-hidden" style={{ width: `${pct}%` }}>
          <div className="flex gap-0.5 text-[#C9A227]">
            {Array.from({ length: 5 }).map((_, i) => <Star key={i} size={size} fill="currentColor" strokeWidth={0} />)}
          </div>
        </div>
      </div>
      {showValue && (
        <span className="text-xs text-foreground-faint">{value.toFixed(1)}{count != null ? ` (${count})` : ''}</span>
      )}
    </div>
  )
}