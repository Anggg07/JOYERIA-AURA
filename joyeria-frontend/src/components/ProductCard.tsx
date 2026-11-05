import { memo } from 'react'
import { Link } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import { Heart, ShoppingCart, Eye } from 'lucide-react'
import type { Product } from '../types'
import Price from './Price'
import Rating from './Rating'
import SmartImage from './SmartImage'

interface Props {
  product: Product
  onAddToCart?: (id: number) => void
  onQuickView?: (product: Product) => void
  onToggleFavorite?: (id: number) => void
  isFavorite?: boolean
}

function ProductCard({ product, onAddToCart, onQuickView, onToggleFavorite, isFavorite }: Props) {
  const img = product.images?.length ? product.images[0].url : ''
  const img2 = product.images && product.images.length > 1 ? product.images[1].url : ''
  const reduced = useReducedMotion()
  const lowStock = product.stock > 0 && product.stock <= 5
  const outOfStock = product.stock <= 0

  return (
    <motion.div
      initial={reduced ? false : { opacity: 0, y: 20 }}
      whileInView={reduced ? undefined : { opacity: 1, y: 0 }}
      viewport={{ once: true }}
      className="group relative flex flex-col transition-transform duration-500 ease-out hover:-translate-y-1">
      {/* Fotografía: sin marco, flota sobre el fondo marfil */}
      <div className="relative aspect-[4/5] overflow-hidden bg-surface-muted shadow-sm transition-shadow duration-500 group-hover:shadow-[0_34px_64px_-38px_rgba(41,37,31,0.6)]">
        <Link to={`/productos/${product.slug}`} className="block h-full">
          <SmartImage
            src={img}
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-[700ms] ease-out will-change-transform group-hover:scale-[1.05]"
          />
          {img2 && (
            <SmartImage
              src={img2}
              alt=""
              aria-hidden
              className="absolute inset-0 h-full w-full object-cover opacity-0 transition-opacity duration-700 ease-out group-hover:opacity-100"
            />
          )}
        </Link>

        {/* Quick view — siempre accesible en móvil (sin depender del hover) */}
        {onQuickView && (
          <button
            onClick={() => onQuickView(product)}
            aria-label={`Vista rápida de ${product.name}`}
            className="absolute inset-x-3 bottom-3 flex min-h-[44px] items-center justify-center gap-1.5 border border-ink/10 bg-surface/92 py-3 text-[11px] font-medium uppercase tracking-[0.18em] text-foreground backdrop-blur-sm transition-all duration-300 translate-y-0 opacity-100 hover:border-ink/30 lg:translate-y-2 lg:opacity-0 lg:group-hover:translate-y-0 lg:group-hover:opacity-100">
            <Eye size={14} /> Vista rápida
          </button>
        )}

        {/* Badges — acentos discretos */}
        <div className="absolute left-3 top-3 flex flex-col items-start gap-1.5">
          {product.discountPercent > 0 && (
            <span className="bg-[#C9A227] px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-[#29251F]">
              -{product.discountPercent}%
            </span>
          )}
          {product.isNew && (
            <span className="bg-[#C9A227] text-[#29251F] text-[10px] px-2 py-0.5 font-semibold uppercase tracking-[0.16em]">
              Nuevo
            </span>
          )}
          {lowStock && !outOfStock && (
            <span className="border border-ink/15 bg-surface/90 px-2 py-0.5 text-[11px] font-medium text-foreground-muted backdrop-blur-sm">
              Pocas unidades
            </span>
          )}
        </div>

        {/* Favoritos */}
        {onToggleFavorite && (
          <motion.button
            whileTap={reduced ? undefined : { scale: 0.85 }}
            onClick={(e) => { e.preventDefault(); onToggleFavorite(product.id) }}
            className={`absolute right-3 top-3 flex h-11 w-11 items-center justify-center rounded-full backdrop-blur-sm transition-colors sm:h-10 sm:w-10 ${
              isFavorite
                ? 'bg-white/90 text-red-500'
                : 'bg-white/85 text-foreground-faint hover:text-red-500'}`}
            aria-label={isFavorite ? 'Quitar de favoritos' : 'Agregar a favoritos'}>
            <Heart size={16} fill={isFavorite ? 'currentColor' : 'none'} />
          </motion.button>
        )}
      </div>

      {/* Ficha */}
      <div className="flex flex-1 flex-col pt-4">
        <Link to={`/productos/${product.slug}`} className="flex flex-col">
          <p className="text-[10px] font-medium uppercase tracking-[0.22em] text-gold">{product.categoryName}</p>
          <h3 className="mt-1.5 font-serif text-[16px] font-medium leading-snug text-foreground transition-colors group-hover:text-gold">
            {product.name}
          </h3>
        </Link>
        <Rating value={product.averageRating} count={product.reviewCount} className="mt-2" />
        <div className="mt-3">
          <Price price={product.price} comparePrice={product.comparePrice} size="sm" />
        </div>

        <div className="mt-auto pt-4">
          {outOfStock ? (
            <div className="flex-1 border border-line/70 py-2.5 text-center text-[11px] uppercase tracking-[0.18em] text-foreground-faint">
              Agotado
            </div>
          ) : onAddToCart ? (
            <button
              onClick={(e) => { e.preventDefault(); onAddToCart(product.id) }}
              className="flex min-h-[44px] w-full items-center justify-center gap-1.5 border border-ink/15 py-2.5 text-[11px] font-medium uppercase tracking-[0.18em] text-foreground transition-colors duration-300 hover:border-ink hover:bg-ink hover:text-background">
              <ShoppingCart size={14} /> Agregar al carrito
            </button>
          ) : (
            <Link
              to={`/productos/${product.slug}`}
              className="flex min-h-[44px] w-full items-center justify-center border border-ink/15 py-2.5 text-[11px] font-medium uppercase tracking-[0.18em] text-foreground transition-colors duration-300 hover:border-ink hover:bg-ink hover:text-background">
              Ver producto
            </Link>
          )}
        </div>
      </div>
    </motion.div>
  )
}

export default memo(ProductCard)
