import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import {
  ArrowRight, CalendarHeart, Check, Gem, Gift, Palette, RotateCcw, ShoppingCart, Sparkles, User, Wallet,
} from 'lucide-react'
import type { GiftFinderRequest, GiftRecommendation, Material } from '../types'
import { giftService } from '../services/gift.service'
import { materialService } from '../services/material.service'
import { useCart } from '../contexts/CartContext'
import { useToast } from '../contexts/ToastContext'
import { useAuth } from '../contexts/AuthContext'
import { formatPrice } from '../utils/format'
import { getErrorMessage } from '../utils/errors'
import SmartImage from '../components/SmartImage'
import useDocumentTitle from '../hooks/useDocumentTitle'

interface Option {
  value: string
  label: string
}

interface Criteria {
  recipient: string | null
  occasion: string | null
  style: string | null
  budget: string | null
  material: string | null
}

const EMPTY_CRITERIA: Criteria = { recipient: null, occasion: null, style: null, budget: null, material: null }

const RECIPIENTS: Option[] = [
  { value: 'pareja', label: 'Mi pareja' },
  { value: 'madre', label: 'Mi madre' },
  { value: 'padre', label: 'Mi padre' },
  { value: 'amiga', label: 'Una amiga' },
  { value: 'amigo', label: 'Un amigo' },
  { value: 'familia', label: 'Un familiar' },
]

const OCCASIONS: Option[] = [
  { value: 'aniversario', label: 'Aniversario' },
  { value: 'cumpleanos', label: 'Cumpleaños' },
  { value: 'boda', label: 'Boda o compromiso' },
  { value: 'graduacion', label: 'Graduación' },
  { value: 'navidad', label: 'Navidad' },
  { value: 'san valentin', label: 'San Valentín' },
  { value: 'dia de la madre', label: 'Día de la madre' },
  { value: 'dia del padre', label: 'Día del padre' },
]

const STYLES: Option[] = [
  { value: 'elegante', label: 'Elegante' },
  { value: 'casual', label: 'Casual / diario' },
  { value: 'moderno', label: 'Moderno' },
  { value: 'clasico', label: 'Clásico' },
  { value: 'minimalista', label: 'Minimalista' },
]

const BUDGETS: Array<Option & { min?: number; max?: number }> = [
  { value: 'lt500k', label: 'Hasta $500.000', max: 500000 },
  { value: '500k-1500k', label: '$500.000 – $1.500.000', min: 500000, max: 1500000 },
  { value: '1500k-3000k', label: '$1.500.000 – $3.000.000', min: 1500000, max: 3000000 },
  { value: 'gt3000k', label: 'Más de $3.000.000', min: 3000000 },
]

function parseParams(params: URLSearchParams): { criteria: Criteria; hasAny: boolean } {
  const criteria: Criteria = {
    recipient: params.get('recipient'),
    occasion: params.get('occasion'),
    style: params.get('style'),
    budget: params.get('budget'),
    material: params.get('material'),
  }
  return { criteria, hasAny: Object.values(criteria).some(Boolean) }
}

function toPayload(criteria: Criteria): GiftFinderRequest {
  const payload: GiftFinderRequest = {}
  if (criteria.recipient) payload.recipient = criteria.recipient
  if (criteria.occasion) payload.occasion = criteria.occasion
  if (criteria.style) payload.style = criteria.style
  if (criteria.material) payload.material = criteria.material
  const budget = BUDGETS.find(b => b.value === criteria.budget)
  if (budget?.min != null) payload.budgetMin = budget.min
  if (budget?.max != null) payload.budgetMax = budget.max
  return payload
}

function criteriaSummary(criteria: Criteria): string[] {
  const chips: string[] = []
  const recipient = RECIPIENTS.find(o => o.value === criteria.recipient)
  const occasion = OCCASIONS.find(o => o.value === criteria.occasion)
  const style = STYLES.find(o => o.value === criteria.style)
  const budget = BUDGETS.find(o => o.value === criteria.budget)
  if (recipient) chips.push(recipient.label)
  if (occasion) chips.push(occasion.label)
  if (style) chips.push(style.label)
  if (budget) chips.push(budget.label)
  if (criteria.material) chips.push(criteria.material)
  return chips
}

export default function GiftFinder() {
  useDocumentTitle('Encuentra el regalo perfecto')
  const reduced = useReducedMotion()
  const { addItem } = useCart()
  const { toast } = useToast()
  const { isAuthenticated } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()

  const [initial] = useState(() => parseParams(searchParams))
  const [criteria, setCriteria] = useState<Criteria>(initial.criteria)
  const [step, setStep] = useState(0)
  const [materials, setMaterials] = useState<Material[]>([])
  const [recommendations, setRecommendations] = useState<GiftRecommendation[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    materialService.getAll().then(setMaterials).catch(() => setMaterials([]))
  }, [])

  const update = useCallback((key: keyof Criteria, value: string | null) => {
    setCriteria(prev => ({ ...prev, [key]: value }))
  }, [])

  const runSearch = useCallback(async (current: Criteria) => {
    const next = new URLSearchParams()
    if (current.recipient) next.set('recipient', current.recipient)
    if (current.occasion) next.set('occasion', current.occasion)
    if (current.style) next.set('style', current.style)
    if (current.budget) next.set('budget', current.budget)
    if (current.material) next.set('material', current.material)
    setSearchParams(next, { replace: true })

    setLoading(true)
    setError(null)
    try {
      const res = await giftService.recommend(toPayload(current))
      setRecommendations(res.recommendations)
    } catch (err) {
      setError(getErrorMessage(err, 'No pudimos buscar regalos. Intenta de nuevo.'))
      setRecommendations([])
    } finally {
      setLoading(false)
    }
  }, [setSearchParams])

  useEffect(() => {
    if (initial.hasAny) void runSearch(initial.criteria)
  }, [initial, runSearch])

  const steps = useMemo(() => [
    { key: 'recipient' as const, title: '¿Para quién es el regalo?', icon: User, options: RECIPIENTS },
    { key: 'occasion' as const, title: '¿Cuál es la ocasión?', icon: CalendarHeart, options: OCCASIONS },
    { key: 'style' as const, title: '¿Qué estilo le gusta?', icon: Palette, options: STYLES },
    { key: 'budget' as const, title: '¿Cuál es tu presupuesto?', icon: Wallet, options: BUDGETS },
    {
      key: 'material' as const,
      title: '¿Prefiere algún material?',
      icon: Gem,
      options: materials.map(m => ({ value: m.name, label: m.name })),
    },
  ], [materials])

  const current = steps[step]
  const isLastStep = step === steps.length - 1
  const progress = ((step + 1) / steps.length) * 100

  const goNext = () => {
    if (isLastStep) {
      void runSearch(criteria)
    } else {
      setStep(s => Math.min(steps.length - 1, s + 1))
    }
  }

  const skipStep = () => {
    const nextCriteria = { ...criteria, [current.key]: null }
    setCriteria(nextCriteria)
    if (isLastStep) {
      void runSearch(nextCriteria)
    } else {
      setStep(s => Math.min(steps.length - 1, s + 1))
    }
  }

  const reset = () => {
    setCriteria(EMPTY_CRITERIA)
    setStep(0)
    setRecommendations(null)
    setError(null)
    setSearchParams({}, { replace: true })
  }

  const handleAdd = async (product: GiftRecommendation) => {
    if (!isAuthenticated) {
      toast('Inicia sesión para agregar productos al carrito', 'info')
      return
    }
    try {
      await addItem(product.id, 1)
      toast(`${product.name} agregado al carrito`, 'success')
    } catch {
      toast('No se pudo agregar el producto al carrito', 'error')
    }
  }

  const chips = useMemo(() => criteriaSummary(criteria), [criteria])
  const hasResults = recommendations !== null

  return (
    <div>
      <section className="relative overflow-hidden bg-gradient-to-b from-background-warm to-background py-16 md:py-20">
        <div className="absolute -top-16 -right-16 w-72 h-72 rounded-full bg-[#C9A227]/10 blur-3xl" />
        <div className="relative max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.p initial={reduced ? false : { opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
            className="text-sm font-medium tracking-[0.3em] text-[#C9A227] uppercase mb-4">Smart Gift Finder</motion.p>
          <motion.h1 initial={reduced ? false : { opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 }}
            className="font-serif text-4xl md:text-5xl font-medium text-foreground leading-tight mb-5">
            Encuentra el regalo perfecto
          </motion.h1>
          <motion.p initial={reduced ? false : { opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
            className="text-foreground-muted max-w-xl mx-auto">
            Responde unas preguntas rápidas y te mostraremos joyas reales de nuestro catálogo
            que encajan con esa persona y ese momento. Si no lo tienes claro, también funciona.
          </motion.p>
        </div>
      </section>

      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
        {!hasResults && (
          <div className="bg-surface border border-line/70 rounded-2xl p-6 sm:p-8">
            <div className="flex items-center gap-4 mb-6">
              <div className="flex-1">
                <div className="flex items-center justify-between text-xs text-foreground-faint mb-2">
                  <span>Paso {step + 1} de {steps.length}</span>
                  <span>{chips.length ? `${chips.length} de ${steps.length} criterios` : 'Sin filtros'}</span>
                </div>
                <div className="h-1.5 bg-surface-muted rounded-full overflow-hidden">
                  <div className="h-full bg-[#C9A227] rounded-full transition-all" style={{ width: `${progress}%` }} />
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 mb-2 text-[#C9A227]">
              <current.icon size={18} />
              <h2 className="font-serif text-2xl font-medium text-foreground">{current.title}</h2>
            </div>
            <p className="text-sm text-foreground-faint mb-6">Elige una opción o dinos que aún no lo decides.</p>

            <div className="flex flex-wrap gap-3 mb-10">
              {current.options.map(option => {
                const selected = criteria[current.key] === option.value
                return (
                  <button key={option.value} type="button" onClick={() => update(current.key, selected ? null : option.value)}
                    aria-pressed={selected}
                    className={`inline-flex items-center gap-2 px-5 py-3 rounded-full border text-sm font-medium transition-colors ${selected ? 'bg-foreground text-background border-foreground' : 'border-line text-foreground-muted hover:border-[#C9A227]'}`}>
                    {selected && <Check size={14} />}
                    {option.label}
                  </button>
                )
              })}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-4">
              <button type="button" onClick={() => setStep(s => Math.max(0, s - 1))} disabled={step === 0}
                className="text-sm text-foreground-faint hover:text-foreground transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                ← Atrás
              </button>
              <div className="flex items-center gap-3">
                <button type="button" onClick={skipStep}
                  className="text-sm text-foreground-faint hover:text-foreground transition-colors">
                  No estoy seguro
                </button>
                <button type="button" onClick={goNext}
                  className="inline-flex items-center gap-2 bg-foreground hover:bg-primary text-background px-6 py-3 rounded-full font-medium text-sm transition-colors">
                  {isLastStep ? 'Buscar regalos ideales' : 'Continuar'} <ArrowRight size={15} />
                </button>
              </div>
            </div>
          </div>
        )}

        {hasResults && (
          <div>
            <div className="flex flex-wrap items-start justify-between gap-4 mb-8">
              <div>
                <h2 className="font-serif text-3xl font-medium text-foreground mb-2">Regalos recomendados para ti</h2>
                {chips.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {chips.map(chip => (
                      <span key={chip} className="text-xs bg-[#C9A227]/10 text-[#8A6D15] px-3 py-1 rounded-full">{chip}</span>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-foreground-faint">Selección basada en lo más popular del catálogo.</p>
                )}
              </div>
              <button type="button" onClick={reset}
                className="inline-flex items-center gap-2 text-sm font-medium text-[#C9A227] hover:underline">
                <RotateCcw size={14} /> Empezar de nuevo
              </button>
            </div>

            {loading && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-80 bg-surface-muted rounded-2xl animate-pulse" />
                ))}
              </div>
            )}

            {!loading && error && (
              <p className="text-center text-red-500 bg-surface border border-line/70 rounded-xl p-10">{error}</p>
            )}

            {!loading && !error && recommendations.length === 0 && (
              <div className="text-center bg-surface border border-line/70 rounded-xl p-12">
                <Gift size={28} className="text-[#C9A227] mx-auto mb-3" />
                <p className="text-foreground font-medium mb-1">No encontramos coincidencias exactas</p>
                <p className="text-sm text-foreground-faint mb-5">Prueba ampliando el presupuesto o quitando algún filtro.</p>
                <button type="button" onClick={reset} className="text-sm font-medium text-[#C9A227] hover:underline">Ajustar criterios</button>
              </div>
            )}

            {!loading && !error && recommendations.length > 0 && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {recommendations.map(product => (
                  <motion.article key={product.id}
                    initial={reduced ? false : { opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
                    className="group flex flex-col bg-surface rounded-2xl overflow-hidden border border-line/70 hover:border-[#C9A227]/40 transition-colors">
                    <Link to={`/productos/${product.slug}`} className="relative block aspect-[4/5] bg-surface-muted overflow-hidden">
                      <SmartImage src={product.image || ''} alt={product.name}
                        className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-[1.04]" />
                      {product.relevance != null && (
                        <span className="absolute top-3 left-3 inline-flex items-center gap-1 bg-ink/85 text-white text-[11px] font-medium px-2.5 py-1 rounded-full">
                          <Sparkles size={11} className="text-[#F5D06F]" /> {product.relevance}% coincidencia
                        </span>
                      )}
                      {!product.available && (
                        <span className="absolute top-3 right-3 bg-surface/90 text-foreground-faint text-[11px] font-medium px-2.5 py-1 rounded-full">Agotado</span>
                      )}
                    </Link>
                    <div className="p-4 flex flex-col flex-1">
                      {product.categoryName && (
                        <p className="text-[11px] text-[#C9A227] font-medium uppercase tracking-wider mb-1">{product.categoryName}</p>
                      )}
                      <Link to={`/productos/${product.slug}`}>
                        <h3 className="font-serif text-lg text-foreground font-medium leading-snug line-clamp-2 group-hover:text-primary transition-colors">{product.name}</h3>
                      </Link>
                      <p className="text-sm text-foreground-muted mt-1.5">
                        {formatPrice(product.price)}
                        {product.material && <span className="text-foreground-faint"> · {product.material}</span>}
                      </p>
                      {product.explanation && (
                        <p className="text-xs text-foreground-faint leading-relaxed mt-3">{product.explanation}</p>
                      )}
                      <div className="mt-auto pt-4 flex items-center gap-2">
                        {product.available ? (
                          <button type="button" onClick={() => handleAdd(product)}
                            aria-label={`Agregar ${product.name} al carrito`}
                            className="flex-1 inline-flex items-center justify-center gap-1.5 bg-foreground hover:bg-primary text-background text-xs font-medium py-2.5 rounded-lg transition-colors">
                            <ShoppingCart size={14} /> Agregar
                          </button>
                        ) : (
                          <span className="flex-1 text-center text-xs text-foreground-faint py-2.5 border border-line/70 rounded-lg">Sin stock</span>
                        )}
                        <Link to={`/productos/${product.slug}`} aria-label={`Ver ${product.name}`}
                          className="inline-flex items-center gap-1 text-xs font-medium text-[#C9A227] hover:underline px-1">
                          Ver <ArrowRight size={13} />
                        </Link>
                      </div>
                    </div>
                  </motion.article>
                ))}
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  )
}
