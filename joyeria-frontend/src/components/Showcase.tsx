import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import { ChevronLeft, ChevronRight, Eye } from 'lucide-react'
import type { Product } from '../types'
import Price from './Price'
import Rating from './Rating'
import SmartImage from './SmartImage'

interface Props {
  products: Product[]
  onQuickView?: (product: Product) => void
}

export default function Showcase({ products, onQuickView }: Props) {
  const scroller = useRef<HTMLDivElement>(null)
  const [canLeft, setCanLeft] = useState(false)
  const [canRight, setCanRight] = useState(false)
  const reduced = useReducedMotion()

  const update = () => {
    const el = scroller.current
    if (!el) return
    setCanLeft(el.scrollLeft > 4)
    setCanRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 4)
  }

  useEffect(() => {
    const el = scroller.current
    if (!el) return
    update()
    el.addEventListener('scroll', update, { passive: true })
    window.addEventListener('resize', update)
    return () => { el.removeEventListener('scroll', update); window.removeEventListener('resize', update) }
  }, [products])

  const scrollBy = (dir: 1 | -1) => {
    scroller.current?.scrollBy({ left: dir * scroller.current.clientWidth * 0.7, behavior: 'smooth' })
  }

  if (products.length === 0) return null

  return (
    <div className="relative">
      <div className="scrollbar-hide -mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-2 sm:gap-6 sm:px-0 lg:gap-8">
        {products.map((p, i) => (
          <motion.div
            key={p.id}
            initial={reduced ? false : { opacity: 0, y: 18 }}
            whileInView={reduced ? undefined : { opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: (i % 6) * 0.05 }}
            className="group flex w-[68%] shrink-0 snap-start flex-col sm:w-[42%] md:w-[30%] lg:w-[23%]">
            <div className="relative aspect-[4/5] overflow-hidden bg-surface-muted shadow-sm transition-shadow duration-500 group-hover:shadow-[0_34px_64px_-38px_rgba(41,37,31,0.6)]">
              <Link to={`/productos/${p.slug}`} className="block h-full">
                <SmartImage
                  src={p.images?.[0]?.url}
                  alt={p.name}
                  className="h-full w-full object-cover transition-transform duration-[700ms] ease-out will-change-transform group-hover:scale-[1.05]"
                />
              </Link>

              {p.discountPercent > 0 && (
                <span className="absolute left-3 top-3 bg-[#C9A227] px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.16em] text-[#29251F]">
                  -{p.discountPercent}%
                </span>
              )}

              {onQuickView && (
                <button
                  onClick={(e) => { e.preventDefault(); onQuickView(p) }}
                  aria-label={`Vista rápida de ${p.name}`}
                  className="absolute inset-x-3 bottom-3 flex min-h-[44px] items-center justify-center gap-1.5 border border-ink/10 bg-surface/92 py-2.5 text-[11px] font-medium uppercase tracking-[0.18em] text-foreground backdrop-blur-sm transition-all duration-300 translate-y-0 opacity-100 hover:border-ink/30 lg:translate-y-3 lg:opacity-0 lg:group-hover:translate-y-0 lg:group-hover:opacity-100">
                  <Eye size={13} /> Vista rápida
                </button>
              )}
            </div>

            <Link to={`/productos/${p.slug}`} className="mt-4 flex flex-col">
              <p className="text-[10px] font-medium uppercase tracking-[0.22em] text-gold">{p.categoryName}</p>
              <h3 className="mt-1.5 font-serif text-base font-medium leading-snug text-foreground transition-colors group-hover:text-gold md:text-lg">
                {p.name}
              </h3>
              <div className="mt-2 flex items-center justify-between">
                <Price price={p.price} size="sm" />
                <Rating value={p.averageRating} size={12} />
              </div>
            </Link>
          </motion.div>
        ))}
      </div>

      <div className="mt-6 flex justify-end gap-2">
        <button
          onClick={() => scrollBy(-1)}
          disabled={!canLeft}
          aria-label="Anterior"
          className={`inline-flex h-10 w-10 items-center justify-center border transition-colors ${
            canLeft ? 'border-ink/20 text-foreground hover:border-gold hover:text-gold' : 'border-line text-foreground-faint/40'
          }`}>
          <ChevronLeft size={18} />
        </button>
        <button
          onClick={() => scrollBy(1)}
          disabled={!canRight}
          aria-label="Siguiente"
          className={`inline-flex h-10 w-10 items-center justify-center border transition-colors ${
            canRight ? 'border-ink/20 text-foreground hover:border-gold hover:text-gold' : 'border-line text-foreground-faint/40'
          }`}>
          <ChevronRight size={18} />
        </button>
      </div>
    </div>
  )
}
