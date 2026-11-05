import { formatPrice } from '../utils/format'

interface PriceProps {
  price: number
  comparePrice?: number
  size?: 'sm' | 'md' | 'lg'
}

export default function Price({ price, comparePrice, size = 'md' }: PriceProps) {
  const sizes = { sm: 'text-sm', md: 'text-lg', lg: 'text-2xl' }
  return (
    <div className="flex items-center gap-3">
      <span className={`font-semibold text-foreground ${sizes[size]}`}>{formatPrice(price)}</span>
      {comparePrice && comparePrice > price && (
        <span className="text-sm text-foreground-faint line-through">{formatPrice(comparePrice)}</span>
      )}
    </div>
  )
}
