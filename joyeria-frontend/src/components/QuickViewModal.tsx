import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion'
import { X, ShoppingCart, Ruler, Truck, Package } from 'lucide-react'
import type { Product } from '../types'
import Price from './Price'
import Rating from './Rating'
import SmartImage from './SmartImage'

interface Props {
  product: Product | null
  onClose: () => void
  onAddToCart?: (id: number, qty: number) => void
}

export default function QuickViewModal({ product, onClose, onAddToCart }: Props) {
  const [active, setActive] = useState(0)
  const [qty, setQty] = useState(1)
  const reduced = useReducedMotion()

  useEffect(() => {
    if (product) { setActive(0); setQty(1) }
  }, [product])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    if (product) {
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', onKey)
    }
    return () => {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKey)
    }
  }, [product, onClose])

  if (!product) return null

  const images = product.images?.length ? product.images : []
  const img = images[active]?.url
  const outOfStock = product.stock <= 0
  const rawSpecs: Array<[string, string | undefined]> = [
    ['Material', product.materialNames?.join(', ')],
    ['Color', product.color],
    ['Talla', product.size],
    ['Peso', product.weight],
    ['Dimensiones', product.dimensions],
  ]
  const specs = rawSpecs.filter(([, v]) => !!v)

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <motion.div className="absolute inset-0 bg-black/50 backdrop-blur-[2px]"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose} />
        <motion.div
          role="dialog"
          aria-modal="true"
          aria-label={product.name}
          initial={reduced ? { opacity: 1 } : { opacity: 0, y: 24, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={reduced ? { opacity: 0 } : { opacity: 0, y: 24, scale: 0.97 }}
          transition={{ duration: 0.25 }}
          className="relative bg-surface rounded-2xl shadow-2xl w-full max-w-3xl max-h-[92vh] overflow-y-auto">
          <button onClick={onClose} aria-label="Cerrar"
            className="absolute top-3.5 right-3.5 z-10 w-11 h-11 rounded-full bg-surface/90 shadow-sm flex items-center justify-center text-foreground-faint hover:text-foreground transition-colors">
            <X size={18} />
          </button>

          <div className="grid grid-cols-1 md:grid-cols-2">
            {/* Gallery */}
            <div className="bg-surface-muted md:border-r border-line/70 p-4">
              <div className="aspect-[4/5] rounded-lg overflow-hidden bg-surface-muted">
                <SmartImage src={img} alt={product.name} className="w-full h-full object-cover" />
              </div>
              {images.length > 1 && (
                <div className="flex flex-wrap gap-2 mt-3">
                  {images.map((im, i) => (
                    <button key={i} onClick={() => setActive(i)}
                      className={`w-16 h-16 rounded-lg overflow-hidden border-2 transition ${i === active ? 'border-[#C9A227]' : 'border-transparent opacity-70 hover:opacity-100'}`}>
                      <SmartImage src={im.url} alt={im.alt || ''} className="w-full h-full object-cover" />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Info */}
            <div className="p-6 flex flex-col">
              <p className="text-[11px] uppercase tracking-[0.2em] text-[#C9A227] font-medium">{product.categoryName}</p>
              <h2 className="font-serif text-2xl text-foreground font-medium mt-1 mb-2">{product.name}</h2>
              <Rating value={product.averageRating} count={product.reviewCount} size={14} className="mb-3" />

              <Price price={product.price} comparePrice={product.comparePrice} size="lg" />

              {product.description && (
                <p className="text-sm text-foreground-muted leading-relaxed mt-4 line-clamp-3">{product.description}</p>
              )}

              {specs.length > 0 && (
                <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-2 text-sm border-t border-line/70 pt-4">
                  {specs.map(([k, v]) => (
                    <div key={k}>
                      <dt className="text-[11px] uppercase tracking-wider text-foreground-faint">{k}</dt>
                      <dd className="text-foreground font-medium">{v}</dd>
                    </div>
                  ))}
                </dl>
              )}

              <div className="mt-5 border-t border-line/70 pt-5 space-y-3">
                <div className="flex items-center justify-between text-xs text-foreground-faint">
                  {outOfStock ? (
                    <span className="text-red-500 font-medium">Producto agotado</span>
                  ) : product.stock <= 5 ? (
                    <span className="text-primary font-medium">Solo quedan {product.stock} unidades</span>
                  ) : (
                    <span className="text-accent-green font-medium">Disponible</span>
                  )}
                  <span className="flex items-center gap-1"><Ruler size={13} /> Talla {product.size || 'estándar'}</span>
                </div>

                {!outOfStock && (
                  <div className="flex items-center gap-3">
                    <div className="flex items-center border border-line rounded-lg">
                      <button onClick={() => setQty(Math.max(1, qty - 1))} aria-label="Disminuir cantidad" className="min-w-11 min-h-11 flex items-center justify-center text-foreground-faint hover:text-foreground">−</button>
                      <span className="w-8 text-center text-sm font-medium">{qty}</span>
                      <button onClick={() => setQty(Math.min(product.stock, qty + 1))} aria-label="Aumentar cantidad" className="min-w-11 min-h-11 flex items-center justify-center text-foreground-faint hover:text-foreground">+</button>
                    </div>
                    <button
                      onClick={() => { onAddToCart?.(product.id, qty); onClose() }}
                      className="flex-1 bg-[#C9A227] hover:bg-[#b8911f] text-white text-sm font-medium py-2.5 rounded-lg flex items-center justify-center gap-2 transition-colors">
                      <ShoppingCart size={16} /> Agregar al carrito
                    </button>
                  </div>
                )}

                <Link to={`/productos/${product.slug}`} onClick={onClose}
                  className="block text-center text-sm text-foreground hover:text-[#C9A227] underline underline-offset-4 transition-colors">
                  Ver ficha completa del producto
                </Link>

                <div className="flex items-center justify-center gap-4 text-[11px] text-foreground-faint">
                  <span className="flex items-center gap-1"><Truck size={13} /> Despacho 24-72h</span>
                  <span className="flex items-center gap-1"><Package size={13} /> Estuche de regalo</span>
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}