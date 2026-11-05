import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import { ArrowRight, Gem, Gift, Quote, Calendar, Pencil, Sparkles, ShoppingCart, Send, Check } from 'lucide-react'
import type { Product, Material } from '../types'
import { productService } from '../services/product.service'
import { categoryService } from '../services/category.service'
import { materialService } from '../services/material.service'
import { useCart } from '../contexts/CartContext'
import { useToast } from '../contexts/ToastContext'
import { formatPrice } from '../utils/format'
import Skeleton from '../components/Skeleton'
import useDocumentTitle from '../hooks/useDocumentTitle'

const WHATSAPP = '573001234567'

const STONES = ['Diamante', 'Esmeralda', 'Zafiro azul', 'Rubí', 'Zirconia', 'Sin piedra']
const FINISHES = ['Oro brillante', 'Mate', 'Dos tonos']
const ENGRAVINGS = [
  { id: 'none', label: 'Sin grabado', hint: '' },
  { id: 'laser', label: 'Grabado láser', hint: 'Preciso y sutil, ideal para mensajes largos' },
  { id: 'letter', label: 'Puño de letra', hint: 'Letra manuscrita con el carácter de una nota personal' },
]

const stepsMeta = [
  { n: '01', label: 'Pieza base' },
  { n: '02', label: 'Material' },
  { n: '03', label: 'Grabado' },
  { n: '04', label: 'Detalles' },
]

export default function Personalize() {
  const { addItem } = useCart()
  const { toast } = useToast()
  const reduced = useReducedMotion()

  useDocumentTitle('Personalizar joya')

  const [available, setAvailable] = useState<Product[]>([])
  const [catLabels, setCatLabels] = useState<Record<number, string>>({})
  const [materials, setMaterials] = useState<Material[]>([])
  const [loading, setLoading] = useState(true)

  const [step, setStep] = useState(1)
  const [product, setProduct] = useState<Product | null>(null)
  const [material, setMaterial] = useState<Material | null>(null)
  const [engravingType, setEngravingType] = useState('none')
  const [engraving, setEngraving] = useState('')
  const [stone, setStone] = useState<string | null>(null)
  const [finish, setFinish] = useState('Oro brillante')
  const [giftBox, setGiftBox] = useState(true)
  const [note, setNote] = useState('')
  const [dueDate, setDueDate] = useState('')

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const [cats, mats] = await Promise.all([
          categoryService.getAll(),
          materialService.getAll(),
        ])
        if (cancelled) return
        const labels: Record<number, string> = {}
        cats.forEach(c => { labels[c.id] = c.name })
        setCatLabels(labels)
        setMaterials(mats)

        const custom = cats.filter(c => /personal|medida|regalo|historia/i.test(c.name)).map(c => c.id)
        const ids = custom.length ? custom : [7, 8]
        const results = await Promise.all(
          ids.map(id => productService.getAll({ category: String(id), size: 12 }).then(r => r.content).catch(() => []) as Promise<Product[]>)
        )
        if (cancelled) return
        const all = results.flat().filter((p, i, arr) => arr.findIndex(x => x.id === p.id) === i)
        setAvailable(all)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => { cancelled = true }
  }, [])

  const engravingMeta = ENGRAVINGS.find(e => e.id === engravingType)! as (typeof ENGRAVINGS)[number]

  const summary = useMemo(() => {
    const lines: Array<{ icon: string; label: string; value: string }> = []
    if (product) lines.push({ icon: 'gem', label: catLabels[product.categoryId ?? -1] || product.categoryName || 'Pieza', value: product.name })
    lines.push({ icon: 'ring', label: 'Material', value: material?.name || 'Por definir' })
    lines.push({ icon: 'pencil', label: engravingMeta.label, value: engravingType === 'none' ? '—' : engraving.trim() || 'Sin texto aún' })
    lines.push({ icon: 'sparkles', label: 'Piedra', value: stone || 'Sin piedra' })
    lines.push({ icon: 'hammer', label: 'Acabado', value: finish })
    if (giftBox) lines.push({ icon: 'gift', label: 'Estuche', value: 'Estuche premium incluido' })
    if (dueDate) lines.push({ icon: 'calendar', label: 'Fecha deseada', value: dueDate })
    if (note.trim()) lines.push({ icon: 'quote', label: 'Mensaje', value: note.trim() })
    return lines
  }, [product, catLabels, material, engravingType, engraving, stone, finish, giftBox, dueDate, note])

  const handleWhatsApp = () => {
    const head = product
      ? `¡Hola AURA! Quiero personalizar una pieza:\n• Pieza: ${product.name} (${catLabels[product.categoryId ?? -1] || product.categoryName || ''})\n• Precio base: ${formatPrice(product.price)}`
      : '¡Hola AURA! Quiero personalizar una pieza, pero aún no elijo la base.'
    const rest = [
      `• Material: ${material?.name || 'Por definir'}`,
      `• ${engravingMeta.label}: ${engravingType === 'none' ? 'Ninguno' : engraving.trim() || 'Por definir'}`,
      `• Piedra: ${stone || 'Sin piedra'}`,
      `• Acabado: ${finish}`,
      giftBox ? '• Estuche premium: Sí' : '',
      dueDate ? `• Fecha deseada: ${dueDate}` : '',
      note.trim() ? `• Mensaje para el taller: ${note.trim()}` : '',
    ].filter(Boolean).join('\n')
    const url = `https://wa.me/${WHATSAPP}?text=${encodeURIComponent(`${head}\n${rest}\nGracias!`)}`
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  const handleAddToCart = async () => {
    if (!product) return
    try { await addItem(product.id, 1); toast('Pieza base agregada al carrito') }
    catch { toast('Error al agregar', 'error') }
  }

  const canSubmit = !!product

  return (
    <div>
      {/* ===== Hero editorial ===== */}
      <section className="relative overflow-hidden bg-gradient-to-b from-background-warm to-background py-20 md:py-24">
        <div className="absolute -top-16 -right-16 w-72 h-72 rounded-full bg-[#C9A227]/10 blur-3xl" />
        <div className="absolute -bottom-20 -left-20 w-80 h-80 rounded-full bg-[#6F8F72]/10 blur-3xl" />
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.p initial={reduced ? false : { opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}
            className="text-sm font-medium tracking-[0.3em] text-[#C9A227] uppercase mb-4">Piezas a tu medida</motion.p>
          <motion.h1 initial={reduced ? false : { opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 }}
            className="font-serif text-4xl md:text-6xl font-medium text-foreground leading-[1.08] mb-6">
            Cuéntanos tu historia,<br />la forjamos en oro
          </motion.h1>
          <motion.p initial={reduced ? false : { opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
            className="text-foreground-muted max-w-2xl mx-auto leading-relaxed text-lg mb-8">
            Elige una pieza base, añade grabados, piedras y detalles, y nuestro atelier la convierte
            en una joya única — o en el regalo con más significado que hayas dado.
          </motion.p>
          <motion.div initial={reduced ? false : { opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }}
            className="flex flex-wrap justify-center gap-4">
            <a href="#configurador" className="inline-flex items-center gap-2 bg-foreground hover:bg-primary text-background px-8 py-3.5 rounded-full font-medium text-sm transition-colors">
              Empezar a personalizar <ArrowRight size={16} />
            </a>
            <Link to="/productos?category=8"
              className="inline-flex items-center gap-2 border border-line text-foreground-muted hover:border-[#C9A227] hover:text-[#C9A227] px-8 py-3.5 rounded-full font-medium text-sm transition-colors">
              <Gift size={16} /> Ver regalos
            </Link>
          </motion.div>

          <motion.div initial={reduced ? false : { opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.22 }}
            className="mt-14 grid grid-cols-2 md:grid-cols-4 gap-4 max-w-3xl mx-auto">
            {[
              { icon: Pencil, label: 'Grabados', desc: 'Nombres y fechas' },
              { icon: Gem, label: 'Piedras', desc: 'De tu elección' },
              { icon: Calendar, label: 'Fechas', desc: 'Para entregar a tiempo' },
              { icon: Quote, label: 'Dedicatorias', desc: 'Con tu mensaje' },
            ].map(f => (
              <div key={f.label} className="bg-surface/80 border border-line/70 rounded-2xl p-4 flex flex-col items-center text-center">
                <f.icon size={18} className="text-[#C9A227] mb-2" />
                <p className="text-sm font-medium text-foreground">{f.label}</p>
                <p className="text-xs text-foreground-faint">{f.desc}</p>
              </div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* ===== Configurador ===== */}
      <section id="configurador" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-20">
        <div className="grid lg:grid-cols-[1fr_360px] gap-10 items-start">
          {/* Left: steps */}
          <div>
            {/* Stepper */}
            <div className="flex items-center gap-2 md:gap-3 mb-8 overflow-x-auto pb-2">
              {stepsMeta.map((s, i) => {
                const active = step === i + 1
                const done = step > i + 1
                return (
                  <div key={s.n} className="flex items-center gap-2 md:gap-3 shrink-0">
                    <button onClick={() => setStep(i + 1)}
                      aria-label={`Paso ${s.n}, ${s.label}`}
                      className={`flex items-center gap-2 px-3.5 py-2 rounded-full border text-sm font-medium transition-colors ${active || done ? 'border-[#C9A227] text-[#C9A227]' : 'border-line text-foreground-faint hover:text-foreground'}`}>
                      {done ? <Check size={14} /> : <span className="text-xs">{s.n}</span>}
                      <span className="hidden sm:inline">{s.label}</span>
                    </button>
                    {i < stepsMeta.length - 1 && <div className="w-4 md:w-6 h-px bg-line hidden sm:block" />}
                  </div>
                )
              })}
            </div>

            {/* Step 1 — Pieza base */}
            {step === 1 && (
              <motion.div key="s1" initial={reduced ? false : { opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
                <div className="flex flex-col items-start gap-1 mb-5 sm:flex-row sm:items-center sm:justify-between">
                  <h2 className="font-serif text-2xl font-medium text-foreground">1. Elige tu pieza base</h2>
                  <span className="text-xs text-foreground-faint">Esta es la base; el grabado y los detalles la hacen tuya.</span>
                </div>
                {loading ? (
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4"><Skeleton className="h-64" count={6} /></div>
                ) : available.length === 0 ? (
                  <p className="text-foreground-faint bg-surface border border-line/70 rounded-xl p-10 text-center">
                    Aún no tenemos piezas para personalizar. Créalas desde el panel de administración o escríbenos por WhatsApp.
                  </p>
                ) : (
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    {available.map(p => {
                      const selected = product?.id === p.id
                      return (
                        <button key={p.id} onClick={() => setProduct(p)}
                          aria-pressed={selected}
                          className={`group text-left overflow-hidden rounded-xl border-2 transition-all ${selected ? 'border-[#C9A227] shadow-md' : 'border-line hover:border-[#C9A227]/50'}`}>
                          <div className="aspect-[4/5] bg-surface-muted overflow-hidden relative">
                            {p.images?.[0]?.url && (
                              <img src={p.images[0].url} alt={p.name} loading="lazy" decoding="async"
                                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-[1.05]" />
                            )}
                            {selected && (
                              <span className="absolute top-2.5 right-2.5 w-6 h-6 rounded-full bg-[#C9A227] text-white flex items-center justify-center"><Check size={13} /></span>
                            )}
                          </div>
                          <div className="p-3 bg-surface">
                            <p className="text-[11px] text-[#C9A227] font-medium uppercase tracking-wider mb-0.5 line-clamp-1">{catLabels[p.categoryId ?? -1] || p.categoryName}</p>
                            <p className="text-sm font-medium text-foreground line-clamp-1 mb-1">{p.name}</p>
                            <p className="text-xs text-foreground-faint">{formatPrice(p.price)} · Desde</p>
                          </div>
                        </button>
                      )
                    })}
                  </div>
                )}
                <div className="mt-6 flex justify-end">
                  <button onClick={() => setStep(2)} disabled={!product}
                    className="inline-flex items-center gap-2 bg-foreground disabled:opacity-40 disabled:cursor-not-allowed hover:bg-primary text-background px-6 py-3 rounded-full font-medium text-sm transition-colors">
                    Siguiente: Material <ArrowRight size={15} />
                  </button>
                </div>
              </motion.div>
            )}

            {/* Step 2 — Material */}
            {step === 2 && (
              <motion.div key="s2" initial={reduced ? false : { opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-2">2. Elige el material</h2>
                <p className="text-foreground-faint text-sm mb-6">Si aún no lo decides, nuestro taller te asesora sin costo.</p>
                {loading ? <Skeleton className="h-24" count={3} /> : (
                  <div className="flex flex-wrap gap-3">
                    <button onClick={() => setMaterial(null)}
                      aria-pressed={!material}
                      className={`px-5 py-3 rounded-full border text-sm font-medium transition-colors ${!material ? 'bg-foreground text-background border-foreground' : 'border-line text-foreground-muted hover:border-[#C9A227]'}`}>
                      Por definir con el taller
                    </button>
                    {materials.map(m => {
                      const selected = material?.id === m.id
                      return (
                        <button key={m.id} onClick={() => setMaterial(m)}
                          aria-pressed={selected}
                          className={`px-5 py-3 rounded-full border text-sm font-medium transition-colors ${selected ? 'bg-foreground text-background border-foreground' : 'border-line text-foreground-muted hover:border-[#C9A227]'}`}>
                          {m.name}
                        </button>
                      )
                    })}
                  </div>
                )}
                <div className="mt-7 flex items-center justify-between">
                  <button onClick={() => setStep(1)} className="text-sm text-foreground-faint hover:text-foreground transition-colors">← Atrás</button>
                  <button onClick={() => setStep(3)}
                    className="inline-flex items-center gap-2 bg-foreground hover:bg-primary text-background px-6 py-3 rounded-full font-medium text-sm transition-colors">
                    Siguiente: Grabado <ArrowRight size={15} />
                  </button>
                </div>
              </motion.div>
            )}

            {/* Step 3 — Grabado */}
            {step === 3 && (
              <motion.div key="s3" initial={reduced ? false : { opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-2">3. Personaliza con un grabado</h2>
                <p className="text-foreground-faint text-sm mb-6">Un nombre, una fecha o una palabra que lo diga todo.</p>
                <div className="grid sm:grid-cols-3 gap-3 mb-5">
                  {ENGRAVINGS.map(e => {
                    const selected = engravingType === e.id
                    return (
                      <button key={e.id} onClick={() => setEngravingType(e.id)}
                        aria-pressed={selected}
                        className={`text-left rounded-xl border-2 p-4 transition-all ${selected ? 'border-[#C9A227] bg-[#C9A227]/5' : 'border-line hover:border-[#C9A227]/50'}`}>
                        <p className="text-sm font-medium text-foreground mb-1">{e.label}</p>
                        <p className="text-xs text-foreground-faint leading-relaxed">{e.hint || '—'}</p>
                      </button>
                    )
                  })}
                </div>

                {engravingType !== 'none' && (
                  <div className="bg-surface border border-line/70 rounded-xl p-5">
                    <label htmlFor="engraving" className="block text-sm font-medium text-foreground mb-2">
                      Texto del grabado <span className="text-foreground-faint font-normal">(máx. 18 caracteres)</span>
                    </label>
                    <input id="engraving" value={engraving} maxLength={18}
                      onChange={e => setEngraving(e.target.value)}
                      placeholder="Amor · 14.02.19 · Papá"
                      className="w-full px-4 py-3 rounded-lg border border-line outline-none focus:border-[#C9A227] transition text-sm" />
                    <div className={`mt-4 rounded-lg border border-[#C9A227]/30 bg-[#C9A227]/5 px-4 py-3 min-h-12 flex items-center ${engravingType === 'letter' ? 'justify-center' : ''}`}>
                      <span className={`text-foreground ${engraving.trim() ? '' : 'opacity-40'} ${engravingType === 'letter' ? 'font-serif italic text-lg' : 'uppercase tracking-[0.25em] text-sm'}`}>
                        {engraving.trim() || 'Vista previa del grabado'}
                      </span>
                    </div>
                  </div>
                )}

                <div className="mt-7 flex items-center justify-between">
                  <button onClick={() => setStep(2)} className="text-sm text-foreground-faint hover:text-foreground transition-colors">← Atrás</button>
                  <button onClick={() => setStep(4)}
                    className="inline-flex items-center gap-2 bg-foreground hover:bg-primary text-background px-6 py-3 rounded-full font-medium text-sm transition-colors">
                    Siguiente: Detalles <ArrowRight size={15} />
                  </button>
                </div>
              </motion.div>
            )}

            {/* Step 4 — Detalles */}
            {step === 4 && (
              <motion.div key="s4" initial={reduced ? false : { opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-6">4. Últimos detalles</h2>

                <div className="mb-6">
                  <p className="text-sm font-medium text-foreground mb-3">Piedra central</p>
                  <div className="flex flex-wrap gap-2.5">
                    {STONES.map(st => (
                      <button key={st} onClick={() => setStone(st === 'Sin piedra' ? null : st)}
                        aria-pressed={stone === st || (st === 'Sin piedra' && !stone)}
                        className={`px-4 py-2.5 rounded-full border text-sm transition-colors ${stone === st || (st === 'Sin piedra' && !stone) ? 'bg-foreground text-background border-foreground' : 'border-line text-foreground-muted hover:border-[#C9A227]'}`}>
                        {st}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="mb-6">
                  <p className="text-sm font-medium text-foreground mb-3">Acabado</p>
                  <div className="flex flex-wrap gap-2.5">
                    {FINISHES.map(f => (
                      <button key={f} onClick={() => setFinish(f)}
                        aria-pressed={finish === f}
                        className={`px-4 py-2.5 rounded-full border text-sm transition-colors ${finish === f ? 'bg-foreground text-background border-foreground' : 'border-line text-foreground-muted hover:border-[#C9A227]'}`}>
                        {f}
                      </button>
                    ))}
                  </div>
                </div>

                <button onClick={() => setGiftBox(g => !g)}
                  aria-pressed={giftBox}
                  className={`w-full flex items-center gap-3 rounded-xl border-2 p-4 mb-6 transition-all text-left ${giftBox ? 'border-[#C9A227] bg-[#C9A227]/5' : 'border-line hover:border-[#C9A227]/50'}`}>
                  <span className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${giftBox ? 'bg-[#C9A227] text-white' : 'bg-surface-muted text-foreground-faint'}`}>
                    <Gift size={20} />
                  </span>
                  <span>
                    <span className="block text-sm font-medium text-foreground">Estuche premium de regalo</span>
                    <span className="block text-xs text-foreground-faint">Incluye dedicatoria manuscrita · sin costo adicional</span>
                  </span>
                  {giftBox && <Check size={18} className="text-[#C9A227] ml-auto" />}
                </button>

                <div className="grid sm:grid-cols-2 gap-4 mb-6">
                  <div>
                    <label htmlFor="dueDate" className="block text-sm font-medium text-foreground mb-2">Fecha deseada de entrega</label>
                    <input id="dueDate" type="date" value={dueDate} min={new Date().toISOString().split('T')[0]}
                      onChange={e => setDueDate(e.target.value)}
                      className="w-full px-4 py-3 rounded-lg border border-line outline-none focus:border-[#C9A227] transition text-sm" />
                  </div>
                  <div>
                    <label htmlFor="note" className="block text-sm font-medium text-foreground mb-2">Mensaje para el taller</label>
                    <input id="note" value={note} onChange={e => setNote(e.target.value)} maxLength={120}
                      placeholder="Es para mi esposa por nuestro aniversario..."
                      className="w-full px-4 py-3 rounded-lg border border-line outline-none focus:border-[#C9A227] transition text-sm" />
                  </div>
                </div>

                <div className="mt-7 flex items-center justify-between">
                  <button onClick={() => setStep(3)} className="text-sm text-foreground-faint hover:text-foreground transition-colors">← Atrás</button>
                  <button onClick={() => setStep(1)}
                    className="inline-flex items-center gap-2 text-sm text-foreground-faint hover:text-foreground transition-colors">
                    Volver al inicio
                  </button>
                </div>
              </motion.div>
            )}
          </div>

          {/* Right: summary */}
          <div className="lg:sticky lg:top-24">
            <motion.div initial={reduced ? false : { opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
              className="bg-surface border border-line/70 rounded-2xl overflow-hidden">
              <div className="p-5 border-b border-line/70">
                <h2 className="font-serif text-xl font-medium text-foreground mb-1">Tu pedido personalizado</h2>
                <p className="text-xs text-foreground-faint">Revisa tu selección antes de enviar.</p>
              </div>

              <div className="p-5">
                {!product ? (
                  <p className="text-sm text-foreground-faint bg-surface-muted rounded-lg p-4 text-center">Empieza eligiendo una pieza base en el paso 1.</p>
                ) : (
                  <div className="mb-4">
                    <div className="flex gap-3">
                      <div className="w-20 h-24 rounded-lg bg-surface-muted overflow-hidden shrink-0">
                        {product.images?.[0]?.url && <img src={product.images[0].url} alt={product.name} decoding="async" className="w-full h-full object-cover" />}
                      </div>
                      <div className="min-w-0">
                        <p className="text-[11px] text-[#C9A227] uppercase tracking-wider font-medium">{catLabels[product.categoryId ?? -1] || product.categoryName}</p>
                        <p className="font-serif text-lg text-foreground font-medium leading-snug">{product.name}</p>
                        <p className="text-sm text-foreground-faint mt-1">{formatPrice(product.price)} · precio base</p>
                      </div>
                    </div>
                  </div>
                )}

                <ul className="space-y-2.5 border-t border-line/70 pt-4 mb-5">
                  {summary.map((l, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm">
                      <span className="text-[#C9A227] mt-0.5">
                        {l.icon === 'gem' && <Gem size={14} />}
                        {l.icon === 'ring' && <Sparkles size={14} />}
                        {l.icon === 'pencil' && <Pencil size={14} />}
                        {l.icon === 'sparkles' && <Sparkles size={14} />}
                        {l.icon === 'hammer' && <Sparkles size={14} />}
                        {l.icon === 'gift' && <Gift size={14} />}
                        {l.icon === 'calendar' && <Calendar size={14} />}
                        {l.icon === 'quote' && <Quote size={14} />}
                      </span>
                      <span className="min-w-0">
                        <span className="text-foreground-faint">{l.label}: </span>
                        <span className="text-foreground font-medium">{l.value}</span>
                      </span>
                    </li>
                  ))}
                </ul>

                <div className="mb-5 text-xs text-foreground-faint leading-relaxed bg-surface-muted rounded-lg p-3">
                  El precio final depende de material, piedra y grabado. Nuestro atelier te envía una
                  cotización confirmada tras revisar tu solicitud.
                </div>

                <button onClick={handleAddToCart} disabled={!canSubmit}
                  className="w-full inline-flex items-center justify-center gap-2 bg-[#C9A227] hover:bg-[#b8911f] text-white py-3 rounded-lg font-medium text-sm transition-colors disabled:opacity-40 disabled:cursor-not-allowed mb-3">
                  <ShoppingCart size={16} /> Agregar la pieza base al carrito
                </button>
                <button onClick={handleWhatsApp} disabled={!canSubmit}
                  className="w-full inline-flex items-center justify-center gap-2 border-2 border-[#C9A227] text-[#C9A227] hover:bg-[#C9A227] hover:text-white py-3 rounded-lg font-medium text-sm transition-all disabled:opacity-40 disabled:cursor-not-allowed">
                  <Send size={16} /> Enviar solicitud por WhatsApp
                </button>
                <p className="mt-3 text-center text-[11px] text-foreground-faint">Respuesta del taller en menos de 24 horas hábiles.</p>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* ===== CTA final ===== */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        <motion.div initial={reduced ? false : { opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
          className="relative overflow-hidden rounded-3xl bg-ink text-white px-8 py-16 md:p-20 text-center">
          <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-[#C9A227]/60 to-transparent" />
          <div className="absolute inset-x-0 bottom-0 h-px bg-gradient-to-r from-transparent via-[#C9A227]/40 to-transparent" />
          <div className="absolute -top-10 -right-10 w-56 h-56 rounded-full bg-[#C9A227]/10 blur-3xl" />
          <p className="text-sm font-medium tracking-[0.3em] text-[#F5D06F] uppercase mb-4">El atelier AURA</p>
          <h2 className="font-serif text-3xl md:text-5xl font-medium leading-[1.12] mb-5">
            ¿Tienes una idea en mente?<br />Hagámosla realidad.
          </h2>
          <p className="text-white/70 max-w-xl mx-auto mb-9">
            Cuéntanos qué pieza imaginas, para quién es y cuándo la necesitas. Nuestros maestros joyeros te guiarán en cada paso.
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <a href="https://wa.me/573001234567?text=Hola%20AURA%2C%20quiero%20crear%20una%20pieza%20desde%20cero"
              target="_blank" rel="noopener noreferrer"
              className="inline-flex items-center gap-2 bg-foreground hover:bg-primary text-background px-8 py-3.5 rounded-full font-medium text-sm transition-colors">
              <Send size={16} /> Hablar con el taller
            </a>
            <Link to="/productos"
              className="inline-flex items-center gap-2 border border-white/30 text-white hover:border-white hover:bg-white/10 px-8 py-3.5 rounded-full font-medium text-sm transition-colors">
              Explorar la colección <ArrowRight size={16} />
            </Link>
          </div>
        </motion.div>
      </section>
    </div>
  )
}