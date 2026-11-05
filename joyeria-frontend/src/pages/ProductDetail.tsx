import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { Heart, ShoppingCart, ChevronRight, Truck, Shield, Award, Star, Minus, Plus, Maximize2 } from 'lucide-react'
import { productService } from '../services/product.service'
import { userService } from '../services/user.service'
import useAbortableEffect from '../hooks/useAbortableEffect'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../contexts/ToastContext'
import type { Product, Review } from '../types'
import ProductCard from '../components/ProductCard'
import Price from '../components/Price'
import ReviewCard from '../components/ReviewCard'
import Skeleton from '../components/Skeleton'
import SmartImage from '../components/SmartImage'
import ImageLightbox from '../components/ImageLightbox'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function ProductDetail() {
  const { slug } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState<Product | null>(null)
  const [related, setRelated] = useState<Product[]>([])
  const [reviews, setReviews] = useState<Review[]>([])
  const [loading, setLoading] = useState(true)
  const [activeImage, setActiveImage] = useState(0)
  const [lightboxOpen, setLightboxOpen] = useState(false)
  const [quantity, setQuantity] = useState(1)
  const [isFavorite, setIsFavorite] = useState(false)
  const { addItem } = useCart()
  const { isAuthenticated } = useAuth()
  const { toast } = useToast()

  useDocumentTitle(product ? product.name : 'Producto')

  useAbortableEffect((signal) => {
    if (!slug) return
    setLoading(true)
    productService.getBySlug(slug, signal).then((p) => {
      if (signal.aborted) return
      setProduct(p)
      setLoading(false)
      productService.getRelated(p.id, signal).then(r => { if (!signal.aborted) setRelated(r) }).catch(() => {})
      userService.getProductReviews(p.id, 0, 5, signal).then(r => { if (!signal.aborted) setReviews(r.content) }).catch(() => {})
      if (isAuthenticated) {
        userService.isFavorite(p.id, signal).then(f => { if (!signal.aborted) setIsFavorite(f) }).catch(() => {})
      }
    }).catch(() => {
      if (signal.aborted) return
      setLoading(false)
      navigate('/productos')
    })
  }, [slug, isAuthenticated, navigate])

  if (loading) return <div className="max-w-7xl mx-auto px-4 py-16"><Skeleton className="h-96" /></div>
  if (!product) return null

  const handleAddToCart = async () => {
    try { await addItem(product.id, quantity); toast('Agregado al carrito') }
    catch { toast('Error al agregar', 'error') }
  }

  const handleBuyNow = async () => {
    try { await addItem(product.id, quantity); toast('Producto agregado'); navigate('/carrito') }
    catch { toast('Error al agregar', 'error') }
  }

  const handleFavorite = async () => {
    if (!isAuthenticated) { navigate('/login'); return }
    const fav = await userService.toggleFavorite(product.id)
    setIsFavorite(fav)
    toast(fav ? 'Agregado a favoritos' : 'Eliminado de favoritos', fav ? 'success' : 'info')
  }

  const images = product.images?.length ? product.images : [{ id: 0, url: '', isPrimary: true, sortOrder: 0 }]

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <nav className="flex items-center gap-2 text-xs text-foreground-faint mb-8">
        <Link to="/" className="hover:text-[#C9A227]">Inicio</Link>
        <ChevronRight size={12} />
        <Link to="/productos" className="hover:text-[#C9A227]">Productos</Link>
        <ChevronRight size={12} />
        {product.categoryName && <><Link to={`/productos?category=${product.categoryId}`} className="hover:text-[#C9A227]">{product.categoryName}</Link><ChevronRight size={12} /></>}
        <span className="text-foreground">{product.name}</span>
      </nav>

      <div className="grid lg:grid-cols-2 gap-10">
        {/* Gallery */}
        <div>
          <div className="relative mb-4">
            <AnimatePresence mode="wait">
              <motion.div key={activeImage} initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                className="aspect-square rounded-xl overflow-hidden bg-background-warm border border-line/70">
                <SmartImage src={images[activeImage].url} alt={product.name || 'Joya'} eager
                  className="w-full h-full object-cover" />
              </motion.div>
            </AnimatePresence>
            <button type="button" onClick={() => setLightboxOpen(true)} aria-label={`Ampliar imagen de ${product.name || 'la joya'}`}
              className="absolute bottom-3 right-3 inline-flex h-10 w-10 items-center justify-center rounded-full border border-line/70 bg-surface/90 text-foreground-muted shadow-md backdrop-blur transition hover:border-[#C9A227] hover:text-[#C9A227]">
              <Maximize2 size={17} />
            </button>
          </div>
          {images.length > 1 && (
            <div className="grid grid-cols-4 gap-3">
              {images.map((img, i) => (
                <button key={img.id} onClick={() => setActiveImage(i)}
                  className={`aspect-square rounded-lg overflow-hidden border-2 transition ${i === activeImage ? 'border-[#C9A227]' : 'border-transparent opacity-60 hover:opacity-100'}`}>
                  <SmartImage src={img.url} alt={img.alt || ''} className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
          {product.isNew && <span className="mt-3 inline-block bg-[#6F8F72] text-white text-xs px-3 py-1 rounded-full">Nuevo</span>}
        </div>

        {/* Info */}
        <div>
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>
            {product.categoryName && <p className="text-sm tracking-widest text-[#C9A227] uppercase font-medium mb-2">{product.categoryName}</p>}
            <h1 className="font-serif text-3xl md:text-4xl font-medium text-foreground mb-4">{product.name}</h1>
            <Price price={product.price} comparePrice={product.comparePrice} size="lg" />
            {product.discountPercent > 0 && <p className="text-sm text-red-500 mt-1">Ahorra {product.discountPercent}%</p>}
            <p className="mt-4 text-xs text-foreground-faint">SKU: {product.sku} | Stock: {product.stock > 0 ? `${product.stock} disponibles` : 'Agotado'}</p>

            <div className="my-6 flex items-center gap-2">
              <div className="flex items-center gap-0.5">
                {[1,2,3,4,5].map(i => <Star key={i} size={16} className={i <= Math.round(product.averageRating || 0) ? 'text-[#C9A227] fill-[#C9A227]' : 'text-foreground/20'} />)}
              </div>
              <span className="text-xs text-foreground-faint">{(product.averageRating || 0).toFixed(1)} · {product.reviewCount || 0} opiniones</span>
            </div>

            <p className="text-foreground-faint leading-relaxed mb-6">{product.description}</p>

            {product.features && (
              <div className="mb-6">
                <h3 className="font-medium text-sm text-foreground mb-2">Características</h3>
                <ul className="text-sm text-foreground-faint space-y-1">
                  {product.features.split('|').filter(Boolean).map((f, i) => (
                    <li key={i} className="flex items-center gap-2"><ChevronRight size={12} className="text-[#C9A227]" /> {f.trim()}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className="grid grid-cols-2 gap-3 text-sm text-foreground-faint mb-6">
              {product.materialNames && product.materialNames.length > 0 && <div><span className="text-foreground-faint block text-xs">Material</span> {product.materialNames.join(', ')}</div>}
              {product.weight && <div><span className="text-foreground-faint block text-xs">Peso</span> {product.weight}</div>}
              {product.dimensions && <div><span className="text-foreground-faint block text-xs">Dimensiones</span> {product.dimensions}</div>}
              {product.size && <div><span className="text-foreground-faint block text-xs">Talla</span> {product.size}</div>}
              {product.color && <div><span className="text-foreground-faint block text-xs">Color</span> {product.color}</div>}
              {product.deliveryTime && <div><span className="text-foreground-faint block text-xs">Entrega</span> {product.deliveryTime}</div>}
            </div>

            {product.careInstructions && (
              <div className="mb-6 p-4 bg-background-warm rounded-lg">
                <h3 className="font-medium text-sm text-foreground mb-1">Cuidados</h3>
                <p className="text-xs text-foreground-faint">{product.careInstructions}</p>
              </div>
            )}

            {product.stock > 0 && (
              <div className="flex items-center gap-3 mb-4">
                <span className="text-sm text-foreground-faint">Cantidad</span>
                <div className="flex items-center border border-line rounded-lg">
                  <button onClick={() => setQuantity(q => Math.max(1, q - 1))} disabled={quantity <= 1} aria-label="Disminuir cantidad"
                    className="min-w-11 min-h-11 flex items-center justify-center text-foreground-faint hover:text-[#C9A227] disabled:opacity-30 disabled:cursor-not-allowed"><Minus size={14} /></button>
                  <span className="w-10 text-center text-sm">{quantity}</span>
                  <button onClick={() => setQuantity(q => Math.min(product.stock, q + 1))} disabled={quantity >= product.stock} aria-label="Aumentar cantidad"
                    className="min-w-11 min-h-11 flex items-center justify-center text-foreground-faint hover:text-[#C9A227] disabled:opacity-30 disabled:cursor-not-allowed"><Plus size={14} /></button>
                </div>
              </div>
            )}

            <div className="flex gap-3 flex-wrap">
              <motion.button whileTap={{ scale: 0.97 }} onClick={handleAddToCart} disabled={product.stock <= 0}
                className="flex-1 min-w-[180px] bg-[#C9A227] hover:bg-[#b8911f] text-white px-6 py-4 rounded-lg font-medium flex items-center justify-center gap-2 transition-colors disabled:opacity-50">
                <ShoppingCart size={18} /> Agregar al carrito
              </motion.button>
              <motion.button whileTap={{ scale: 0.97 }} onClick={handleBuyNow} disabled={product.stock <= 0}
                className="flex-1 min-w-[180px] border-2 border-[#C9A227] text-[#C9A227] hover:bg-[#C9A227] hover:text-white px-6 py-4 rounded-lg font-medium transition-all">
                Comprar ahora
              </motion.button>
              <button onClick={handleFavorite}
                className={`p-4 rounded-lg border-2 transition ${isFavorite ? 'bg-red-50 border-red-200 text-red-500' : 'border-line text-foreground-faint hover:text-red-500 hover:border-red-200'}`}>
                <Heart size={20} fill={isFavorite ? 'currentColor' : 'none'} />
              </button>
            </div>

            {product.stock <= 0 && <p className="text-sm text-red-500 mt-3">Producto agotado</p>}

            <div className="grid grid-cols-3 gap-3 mt-8 pt-6 border-t border-line/70">
              {[
                { icon: Truck, label: 'Envío seguro', sub: 'Nacional' },
                { icon: Shield, label: 'Garantía', sub: 'Calidad' },
                { icon: Award, label: 'Certificado', sub: 'Autenticidad' },
              ].map((b, i) => (
                <div key={i} className="text-center">
                  <b.icon size={20} className="mx-auto mb-1 text-[#C9A227]" />
                  <p className="text-xs font-medium text-foreground">{b.label}</p>
                  <p className="text-[10px] text-foreground-faint">{b.sub}</p>
                </div>
              ))}
            </div>
          </motion.div>
        </div>
      </div>

      {/* Reviews */}
      <section className="mt-16">
        <h2 className="font-serif text-2xl font-medium text-foreground mb-6">Opiniones de clientes</h2>
        {reviews.length === 0 ? (
          <p className="text-foreground-faint">Aún no hay opiniones para este producto.</p>
        ) : (
          <div className="bg-surface rounded-lg p-6 border border-line/70">
            {reviews.map(r => <ReviewCard key={r.id} review={r} />)}
          </div>
        )}
      </section>

      {/* Related */}
      {related.length > 0 && (
        <section className="mt-16">
          <h2 className="font-serif text-2xl font-medium text-foreground mb-6">Productos relacionados</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
            {related.map(p => <ProductCard key={p.id} product={p} />)}
          </div>
        </section>
      )}

      <ImageLightbox
        src={images[activeImage]?.url ?? null}
        alt={product.name || 'Joya'}
        isOpen={lightboxOpen}
        onClose={() => setLightboxOpen(false)}
        caption={product.name}
      />
    </div>
  )
}
