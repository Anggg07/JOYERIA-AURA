import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import { ArrowRight, Shield, Truck, Award, Star, Hammer, Gem, Sparkles, Gift } from 'lucide-react'
import { productService } from '../services/product.service'
import { categoryService } from '../services/category.service'
import { materialService } from '../services/material.service'
import { newsletterService } from '../services/misc.service'
import type { Product, Category, Review, Material } from '../types'
import Showcase from '../components/Showcase'
import QuickViewModal from '../components/QuickViewModal'
import SmartImage from '../components/SmartImage'
import { useCart } from '../contexts/CartContext'
import { useToast } from '../contexts/ToastContext'
import { userService } from '../services/user.service'
import EditorialCategories from '../components/EditorialCategories'
import useDocumentTitle from '../hooks/useDocumentTitle'
import { getErrorMessage } from '../utils/errors'

const materialData: Array<{ name: string; desc: string; color: string }> = [
  { name: 'Oro 18K', desc: 'El estándar de excelencia en joyería fina.', color: '#D4AF37' },
  { name: 'Plata 925', desc: 'Brillantez y durabilidad.', color: '#C0C0C0' },
  { name: 'Oro Blanco', desc: 'Acabado elegante y moderno.', color: '#E5E5E0' },
  { name: 'Oro Rosa', desc: 'Tonos cálidos y románticos.', color: '#E8B4B8' },
  { name: 'Piedras Preciosas', desc: 'Diamantes, esmeraldas y más.', color: '#6F8F72' },
  { name: 'Perlas', desc: 'Perlas cultivadas de alta calidad.', color: '#F5F5F0' },
]

const storyTraits = [
  { icon: Gem, title: 'Selección de gemas', desc: 'Diamantes y gemas de origen trazable, elegidos a mano piedra por piedra.' },
  { icon: Hammer, title: 'Orfebrería a mano', desc: 'Cada pieza se modela, suelda y pule en nuestro taller, sin atajos.' },
  { icon: Sparkles, title: 'Acabado de autor', desc: 'Un último pulido artesanal que da a la pieza su brillo inconfundible.' },
]

const customTraits = [
  { icon: Sparkles, title: 'Diseño a la medida', desc: 'Del boceto a la pieza final, con la asesoría de nuestros maestros joyeros.' },
  { icon: Gem, title: 'Piedras y metales a tu elección', desc: 'Selecciona oro, plata o gemas y define cada detalle de tu creación.' },
  { icon: Gift, title: 'Grabado y entrega con historia', desc: 'Personaliza con nombres, fechas y una dedicatoria en su estuche.' },
]

/** Boceto a lápiz de un anillo con gema, usado en la sección de piezas a medida. */
function RingSketch({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 200 220" className={className} fill="none" role="img" aria-label="Boceto a lápiz de un anillo con gema">
      <g stroke="#29251F" strokeWidth="0.9" fill="none">
        <line x1="100" y1="14" x2="100" y2="206" strokeOpacity="0.25" strokeDasharray="3 4" />
        <line x1="18" y1="120" x2="182" y2="120" strokeDasharray="3 4" strokeOpacity="0.25" />
        <path d="M100 34 L124 66 L100 98 L76 66 Z" strokeOpacity="0.6" />
        <path d="M100 34 L100 98 M76 66 L124 66 M88 50 L112 50 M88 82 L112 82" strokeOpacity="0.35" />
        <path d="M76 66 L64 96 M124 66 L136 96" strokeOpacity="0.5" />
        <path d="M64 96 C64 152 136 152 136 96" />
        <path d="M72 100 C72 146 128 146 128 100" strokeOpacity="0.4" />
        <g strokeOpacity="0.22">
          <line x1="68" y1="150" x2="78" y2="138" />
          <line x1="76" y1="156" x2="86" y2="144" />
          <line x1="86" y1="160" x2="96" y2="148" />
          <line x1="114" y1="148" x2="124" y2="160" />
          <line x1="122" y1="140" x2="132" y2="152" />
        </g>
      </g>
      <path d="M100 34 L124 66" stroke="#C9A227" strokeWidth="1.1" />
    </svg>
  )
}

export default function Home() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryImages, setCategoryImages] = useState<Record<string, string>>({})
  const [materials, setMaterials] = useState<Material[]>([])
  const [reviews, setReviews] = useState<Review[]>([])
  const [loading, setLoading] = useState(true)
  const [quickView, setQuickView] = useState<Product | null>(null)
  const [newsletterEmail, setNewsletterEmail] = useState('')
  const [subscribing, setSubscribing] = useState(false)
  const reduced = useReducedMotion()
  const { addItem } = useCart()
  const { toast } = useToast()

  useDocumentTitle()

  useEffect(() => {
    Promise.all([
      productService.getAll({ size: 50 }).catch(() => ({ content: [] as Product[] })),
      categoryService.getAll().catch(() => [] as Category[]),
      materialService.getAll().catch(() => [] as Material[]),
      userService.getRecentReviews().catch(() => [] as Review[]),
    ])
      .then(([res, cats, mats, revs]) => {
        setProducts(res.content)
        const map: Record<string, string> = {}
        for (const p of res.content) {
          const key = String(p.categoryId ?? '')
          const url = p.images?.[0]?.url
          if (key && url && !(key in map)) map[key] = url
        }
        setCategoryImages(map)
        setCategories(cats)
        setMaterials(mats)
        setReviews(revs)
      })
      .finally(() => setLoading(false))
  }, [])

  const handleAddToCart = async (id: number, qty = 1) => {
    try { await addItem(id, qty); toast('Agregado al carrito') }
    catch { toast('Error al agregar', 'error') }
  }

  const handleSubscribe = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newsletterEmail.trim()) return
    setSubscribing(true)
    try {
      const message = await newsletterService.subscribe(newsletterEmail.trim())
      toast(message || '¡Gracias por suscribirte!')
      setNewsletterEmail('')
    } catch (err) {
      toast(getErrorMessage(err, 'No pudimos completar tu suscripción'), 'error')
    } finally {
      setSubscribing(false)
    }
  }

  const fadeUp = { hidden: { opacity: 0, y: 26 }, visible: { opacity: 1, y: 0, transition: { duration: 0.6 } } }
  const stagger = { visible: { transition: { staggerChildren: 0.1 } } }

  const withImage = products.filter(p => p.images?.[0]?.url)
  const heroImage = withImage[0]?.images?.[0]?.url
  const craftImages = [withImage[0], withImage[1], withImage[2], withImage[3]]
    .map(p => p?.images?.[0]?.url)
    .filter((url): url is string => Boolean(url))
  const customPiece = withImage[4] ?? withImage[0]

  return (
    <div>
      {/* ===== HERO — editorial a dos columnas ===== */}
      <section className="bg-background">
        <div className="mx-auto max-w-[1240px] px-4 py-10 sm:px-6 lg:px-8 lg:py-16">
          <div className="grid items-stretch gap-8 lg:grid-cols-[58fr_42fr] lg:gap-14">
            <motion.div
              initial={reduced ? false : { opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.8 }}
              className="relative h-[340px] overflow-hidden bg-surface-muted sm:h-[400px] lg:h-[440px]">
              <SmartImage
                src={heroImage}
                alt="Pieza de joyería AURA"
                className="h-full w-full object-cover"
              />
            </motion.div>

            <div className="flex flex-col justify-center">
              <motion.p
                initial={reduced ? false : { opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6 }}
                className="text-[11px] font-medium uppercase tracking-[0.32em] text-gold">
                Joyería Artesanal
              </motion.p>
              <motion.h1
                initial={reduced ? false : { opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.7, delay: 0.05 }}
                className="mt-5 font-serif text-[34px] font-normal leading-[1.12] text-foreground sm:text-[42px] lg:text-[48px]">
                Un legado<br />de brillo y<br />sofisticación
              </motion.h1>
              <motion.p
                initial={reduced ? false : { opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.7, delay: 0.12 }}
                className="mt-6 max-w-md leading-relaxed text-foreground-muted">
                Piezas creadas con dedicación y diseñadas para acompañarte en las historias que merecen perdurar.
              </motion.p>
              <motion.div
                initial={reduced ? false : { opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.7, delay: 0.18 }}
                className="mt-9">
                <Link
                  to="/productos"
                  className="group inline-flex items-center gap-2.5 bg-[#29251F] px-7 py-3.5 text-[11px] font-medium uppercase tracking-[0.22em] text-[#F8F5EC] transition-colors duration-300 hover:bg-[#3A342B] focus-visible:bg-[#3A342B] active:bg-[#1F1C17]">
                  Descubrir colección
                  <ArrowRight size={14} className="transition-transform duration-300 group-hover:translate-x-1" />
                </Link>
              </motion.div>
            </div>
          </div>
        </div>
      </section>

      {/* ===== Trust strip ===== */}
      <section className="border-y border-line/60 bg-background">
        <div className="mx-auto grid max-w-7xl grid-cols-2 gap-x-6 gap-y-4 px-4 py-6 md:grid-cols-4 sm:px-6 lg:px-8">
          {[
            { icon: Truck, title: 'Envío sin costo', desc: 'En compras +$500.000' },
            { icon: Shield, title: 'Compra protegida', desc: 'Pago seguro' },
            { icon: Award, title: 'Garantía de por vida', desc: 'En todas las piezas' },
            { icon: Star, title: 'Asesoría cercana', desc: 'Soporte dedicado' },
          ].map((item, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 10 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.08 }}
              className="flex items-center justify-center gap-3">
              <item.icon size={18} className="shrink-0 text-gold" />
              <div>
                <p className="text-[13px] font-medium tracking-wide text-foreground">{item.title}</p>
                <p className="text-[11px] text-foreground-faint">{item.desc}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ===== Carrusel editorial — historias de marca ===== */}
      {products.length > 0 && (
        <section id="historias" className="scroll-mt-[96px] bg-background py-20 sm:py-24">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <motion.div
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true }}
              variants={fadeUp}
              className="mb-12 text-center">
              <p className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">Prendas que trascienden</p>
              <h2 className="mt-3 font-serif text-3xl font-medium text-foreground md:text-4xl">Carrusel de historias de marca</h2>
            </motion.div>
            <Showcase products={products.slice(0, 12)} onQuickView={(p) => setQuickView(p)} />
          </div>
        </section>
      )}

      {/* ===== Categorías — editorial ===== */}
      <EditorialCategories categories={categories} images={categoryImages} loading={loading} />

      {/* ===== Craftsmanship — saber hacer ===== */}
      <section id="saber-hacer" className="scroll-mt-[88px] bg-background py-24 sm:py-28">
        <div className="mx-auto grid max-w-7xl items-center gap-14 px-4 sm:px-6 lg:grid-cols-2 lg:gap-20 lg:px-8">
          <motion.div
            initial={reduced ? false : { opacity: 0, y: 24 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.7 }}
            className="relative order-2 lg:order-1">
            <div className="grid grid-cols-2 gap-4 sm:gap-6">
              <div className="aspect-[3/4] overflow-hidden bg-surface-muted">
                <SmartImage src={craftImages[0]} alt="Detalle de orfebrería" className="h-full w-full object-cover" />
              </div>
              <div className="mt-12 aspect-[3/4] overflow-hidden bg-surface-muted">
                <SmartImage src={craftImages[1]} alt="Selección de gemas en el taller" className="h-full w-full object-cover" />
              </div>
            </div>
            <div className="absolute -bottom-6 left-6 border border-line/60 bg-background px-5 py-4">
              <p className="font-serif text-2xl font-medium text-foreground">12</p>
              <p className="text-[10px] uppercase tracking-[0.22em] text-gold">manos expertas</p>
            </div>
          </motion.div>

          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            variants={stagger}
            className="order-1 lg:order-2">
            <motion.p variants={fadeUp} className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">
              Nuestro saber hacer
            </motion.p>
            <motion.h2 variants={fadeUp} className="mt-4 font-serif text-4xl font-medium leading-[1.12] text-foreground md:text-5xl">
              El arte de crear piezas que perduran
            </motion.h2>
            <motion.p variants={fadeUp} className="mt-6 max-w-lg leading-relaxed text-foreground-muted">
              En el taller de AURA cada joya comienza como una idea y termina en tus manos. La
              selección de gemas, la fundición del metal y el pulido final son oficios que se
              aprenden con los años y se firman con orgullo.
            </motion.p>
            <motion.ul variants={fadeUp} className="mt-9 space-y-5">
              {storyTraits.map(t => (
                <li key={t.title} className="flex items-start gap-4">
                  <span className="mt-1 inline-flex h-9 w-9 shrink-0 items-center justify-center border border-gold/40 text-gold">
                    <t.icon size={16} />
                  </span>
                  <div>
                    <p className="text-[15px] font-medium text-foreground">{t.title}</p>
                    <p className="text-[13px] text-foreground-faint">{t.desc}</p>
                  </div>
                </li>
              ))}
            </motion.ul>
            <motion.div variants={fadeUp} className="mt-10">
              <Link
                to="/productos"
                className="group inline-flex items-center gap-2 text-[12px] font-medium uppercase tracking-[0.2em] text-foreground transition-colors hover:text-gold">
                Conoce nuestra historia
                <ArrowRight size={15} className="transition-transform duration-300 group-hover:translate-x-1" />
              </Link>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* ===== Piezas a medida ===== */}
      <section id="personalizado" className="scroll-mt-[88px] bg-background-warm py-24 sm:py-28">
        <div className="mx-auto grid max-w-7xl items-center gap-14 px-4 sm:px-6 lg:grid-cols-2 lg:gap-20 lg:px-8">
          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            variants={stagger}>
            <motion.p variants={fadeUp} className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">
              Personalizado
            </motion.p>
            <motion.h2 variants={fadeUp} className="mt-4 font-serif text-4xl font-medium leading-[1.12] text-foreground md:text-5xl">
              Joyería sobre medida
            </motion.h2>
            <motion.p variants={fadeUp} className="mt-6 max-w-lg leading-relaxed text-foreground-muted">
              Diseñamos piezas únicas que reflejan tu historia, con atención a cada detalle.
              Del boceto a lápiz a la pieza terminada, elegimos contigo los metales y las gemas.
            </motion.p>
            <motion.ul variants={fadeUp} className="mt-9 space-y-5">
              {customTraits.map(t => (
                <li key={t.title} className="flex items-start gap-4">
                  <span className="mt-1 inline-flex h-9 w-9 shrink-0 items-center justify-center border border-gold/40 text-gold">
                    <t.icon size={16} />
                  </span>
                  <div>
                    <p className="text-[15px] font-medium text-foreground">{t.title}</p>
                    <p className="text-[13px] text-foreground-faint">{t.desc}</p>
                  </div>
                </li>
              ))}
            </motion.ul>
            <motion.div variants={fadeUp} className="mt-10">
              <Link
                to="/personaliza"
                className="group inline-flex items-center gap-2 bg-[#29251F] px-8 py-3.5 text-[12px] font-medium uppercase tracking-[0.2em] text-[#F8F5EC] transition-colors duration-300 hover:bg-[#3A342B] focus-visible:bg-[#3A342B] active:bg-[#1F1C17]">
                Crea tu pieza única
                <ArrowRight size={15} className="transition-transform duration-300 group-hover:translate-x-1" />
              </Link>
            </motion.div>
          </motion.div>

          <motion.div
            initial={reduced ? false : { opacity: 0, y: 24 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.7 }}
            className="relative pr-6 pb-10 sm:pr-10 sm:pb-14">
            <div className="aspect-[4/5] overflow-hidden bg-surface-muted">
              <SmartImage src={customPiece?.images?.[0]?.url} alt="Pieza terminada hecha a medida" className="h-full w-full object-cover" />
            </div>
            <div className="absolute -bottom-2 right-0 w-36 border border-line/60 bg-background p-4 sm:w-44">
              <RingSketch className="w-full" />
              <p className="mt-2 text-center text-[10px] uppercase tracking-[0.22em] text-gold">Boceto a lápiz</p>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ===== Materiales ===== */}
      <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <motion.div initial="hidden" whileInView="visible" viewport={{ once: true }} variants={fadeUp} className="mb-12 text-center">
          <p className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">Materiales</p>
          <h2 className="mt-3 font-serif text-4xl font-medium text-foreground">Materiales que inspiran</h2>
        </motion.div>
        <motion.div
          variants={stagger}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
          className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          {materialData.map((m, i) => {
            const match = materials.find(x => x.name.toLowerCase().replace(/ /g, '') === m.name.toLowerCase().replace(/ /g, ''))
            const inner = (
              <div className="flex h-full flex-col items-center justify-center border border-line/70 bg-surface p-6 text-center transition-colors duration-300 hover:border-gold/50">
                <div className="relative mb-3 h-12 w-12 rounded-full border" style={{ borderColor: m.color }}>
                  <div className="absolute inset-1.5 rounded-full" style={{ background: m.color, opacity: 0.15 }} />
                </div>
                <h4 className="font-serif text-sm font-medium text-foreground">{m.name}</h4>
                <p className="mt-1 text-xs text-foreground-faint">{m.desc}</p>
              </div>
            )
            return (
              <motion.div key={i} variants={fadeUp} className={match ? 'cursor-pointer' : ''}>
                {match ? <Link to={`/productos?material=${match.id}`}>{inner}</Link> : inner}
              </motion.div>
            )
          })}
        </motion.div>
      </section>

      {/* ===== Opiniones ===== */}
      {reviews.length > 0 && (
        <section className="bg-background-warm py-24">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <motion.div
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true }}
              variants={fadeUp}
              className="mb-12 text-center">
              <p className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">Opiniones</p>
              <h2 className="mt-3 font-serif text-4xl font-medium text-foreground">Lo que dicen nuestros clientes</h2>
            </motion.div>
            <div className="grid gap-6 md:grid-cols-3">
              {reviews.slice(0, 3).map((r, i) => (
                <motion.div
                  key={r.id}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="border border-line/60 bg-background p-7">
                  <div className="mb-3 flex gap-1">
                    {[1, 2, 3, 4, 5].map(s => (
                      <Star key={s} size={14} className={s <= r.rating ? 'fill-gold text-gold' : 'text-foreground/20'} />
                    ))}
                  </div>
                  {r.title && <p className="mb-2 text-sm font-medium text-foreground">{r.title}</p>}
                  <p className="mb-3 line-clamp-3 text-sm text-foreground-muted">{r.comment}</p>
                  <p className="text-xs font-medium uppercase tracking-[0.15em] text-foreground-faint">{r.userName}</p>
                </motion.div>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ===== Newsletter ===== */}
      <section className="bg-ink py-20">
        <div className="mx-auto max-w-2xl px-4 text-center text-white">
          <p className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">El círculo AURA</p>
          <h2 className="mt-4 font-serif text-3xl font-medium">Únete al círculo</h2>
          <p className="mb-8 mt-4 text-white/70">
            Ofertas exclusivas, adelantos de colección y contenido especial directamente en tu correo.
          </p>
          <form onSubmit={handleSubscribe} className="mx-auto flex max-w-md border border-white/25">
            <input
              type="email"
              value={newsletterEmail}
              onChange={e => setNewsletterEmail(e.target.value)}
              placeholder="Tu email"
              required
              className="flex-1 bg-transparent px-5 py-3 text-white outline-none placeholder:text-white/50"
            />
            <button
              type="submit"
              disabled={subscribing}
              className="bg-white px-7 py-3 text-[12px] font-medium uppercase tracking-[0.2em] text-ink transition-colors hover:bg-gold hover:text-white disabled:opacity-60 min-h-11">
              {subscribing ? 'Enviando…' : 'Suscribir'}
            </button>
          </form>
        </div>
      </section>

      <QuickViewModal product={quickView} onClose={() => setQuickView(null)} onAddToCart={handleAddToCart} />
    </div>
  )
}
