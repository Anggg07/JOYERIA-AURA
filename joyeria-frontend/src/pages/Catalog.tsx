import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'
import { SlidersHorizontal, X, Check } from 'lucide-react'
import { productService } from '../services/product.service'
import { categoryService } from '../services/category.service'
import { materialService } from '../services/material.service'
import useAbortableEffect from '../hooks/useAbortableEffect'
import type { Product, Category, Material, PagedResponse } from '../types'
import ProductCard from '../components/ProductCard'
import QuickViewModal from '../components/QuickViewModal'
import { ProductCardSkeleton } from '../components/Skeleton'
import Pagination from '../components/Pagination'
import { useCart } from '../contexts/CartContext'
import { useToast } from '../contexts/ToastContext'
import useDocumentTitle from '../hooks/useDocumentTitle'

const COLOR_OPTIONS = ['Dorado', 'Plateado', 'Blanco', 'Azul', 'Rosa', 'Verde']

export default function Catalog() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [materials, setMaterials] = useState<Material[]>([])
  const [meta, setMeta] = useState<{ page: number; totalPages: number; totalElements: number }>({ page: 0, totalPages: 1, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [quickView, setQuickView] = useState<Product | null>(null)
  const reduced = useReducedMotion()
  const { addItem } = useCart()
  const { toast } = useToast()

  const query = searchParams.get('q') || ''
  const category = searchParams.get('category') || ''
  const material = searchParams.get('material') || ''
  const color = searchParams.get('color') || ''
  const inStock = searchParams.get('inStock') === 'true'
  const sort = searchParams.get('sort') || 'createdAt'
  const dir = searchParams.get('dir') || 'desc'
  const page = parseInt(searchParams.get('page') || '0')
  const minPrice = searchParams.get('minPrice') || ''
  const maxPrice = searchParams.get('maxPrice') || ''

  const [priceMin, setPriceMin] = useState(minPrice)
  const [priceMax, setPriceMax] = useState(maxPrice)

  useDocumentTitle(query ? `Resultados: ${query}` : 'Productos')

  useEffect(() => {
    if (priceMin === minPrice && priceMax === maxPrice) return
    const t = setTimeout(() => {
      setSearchParams(prev => {
        const params = new URLSearchParams(prev)
        if (priceMin) params.set('minPrice', priceMin); else params.delete('minPrice')
        if (priceMax) params.set('maxPrice', priceMax); else params.delete('maxPrice')
        params.set('page', '0')
        return params
      })
    }, 400)
    return () => clearTimeout(t)
  }, [priceMin, priceMax, minPrice, maxPrice, setSearchParams])

  useEffect(() => {
    categoryService.getAll().then(setCategories).catch(() => {})
    materialService.getAll().then(setMaterials).catch(() => {})
  }, [])

  useAbortableEffect((signal) => {
    setLoading(true)
    const params: Record<string, string | number | boolean> = { page, size: 12, sort, direction: dir }
    if (query) params.q = query
    if (category) params.categoryId = category
    if (material) params.materialId = material
    if (color) params.color = color
    if (inStock) params.inStock = true
    if (minPrice) params.minPrice = minPrice
    if (maxPrice) params.maxPrice = maxPrice

    const fetcher = query ? () => productService.search(query, params, signal) : () => productService.getAll(params, signal)
    fetcher().then((res: PagedResponse<Product>) => {
      if (signal.aborted) return
      setProducts(res.content)
      setMeta({ page: res.page, totalPages: res.totalPages, totalElements: res.totalElements })
    }).catch(() => {}).finally(() => { if (!signal.aborted) setLoading(false) })
  }, [query, category, material, color, inStock, sort, dir, page, minPrice, maxPrice])

  const updateParams = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams)
    if (value) params.set(key, value)
    else params.delete(key)
    params.set('page', '0')
    setSearchParams(params)
  }

  const toggleInStock = () => updateParams('inStock', inStock ? '' : 'true')

  const clearFilters = () => {
    setPriceMin('')
    setPriceMax('')
    setSearchParams(query ? { q: query } : {})
  }

  const handleAddToCart = useCallback(async (id: number, qty = 1) => {
    try { await addItem(id, qty); toast('Agregado al carrito') }
    catch { toast('Error al agregar', 'error') }
  }, [addItem, toast])

  const hasFilters = category || material || color || inStock || minPrice || maxPrice
  const activeChips = [
    category && { key: 'category', label: categories.find(c => c.id.toString() === category)?.name || 'Categoría' },
    material && { key: 'material', label: materials.find(m => m.id.toString() === material)?.name || 'Material' },
    color && { key: 'color', label: color },
    inStock && { key: 'inStock', label: 'En stock' },
    minPrice && { key: 'minPrice', label: `Desde ${new Intl.NumberFormat('es-CO').format(Number(minPrice))}` },
    maxPrice && { key: 'maxPrice', label: `Hasta ${new Intl.NumberFormat('es-CO').format(Number(maxPrice))}` },
  ].filter(Boolean) as Array<{ key: string; label: string }>

  const filterContent = (
    <>
      {hasFilters && (
        <div className="flex flex-wrap items-center gap-1.5 mb-5">
          {activeChips.map(chip => (
            <span key={chip.key} className="bg-[#C9A227]/10 text-primary text-[11px] px-2.5 py-1 rounded-full flex items-center gap-1">
              {chip.label}
              <button onClick={() => updateParams(chip.key, '')} aria-label="Quitar filtro"><X size={11} /></button>
            </span>
          ))}
        </div>
      )}
      <div className="space-y-6">
        <div>
          <h4 className="font-medium text-sm text-foreground mb-3">Categoría</h4>
          <div className="space-y-1">
            <button onClick={() => updateParams('category', '')}
              className={`block w-full text-left text-sm px-3 py-2 rounded-lg transition-colors ${!category ? 'bg-foreground text-background font-medium' : 'text-foreground-muted hover:bg-surface-muted'}`}>
              Todas
            </button>
            {categories.map(c => (
              <button key={c.id} onClick={() => updateParams('category', c.id.toString())}
                className={`block w-full text-left text-sm px-3 py-2 rounded-lg transition-colors ${category === c.id.toString() ? 'bg-foreground text-background font-medium' : 'text-foreground-muted hover:bg-surface-muted'}`}>
                {c.name}
              </button>
            ))}
          </div>
        </div>

        <div>
          <h4 className="font-medium text-sm text-foreground mb-3">Material</h4>
          <div className="space-y-1">
            <button onClick={() => updateParams('material', '')}
              className={`block w-full text-left text-sm px-3 py-2 rounded-lg transition-colors ${!material ? 'bg-primary/15 text-primary font-medium' : 'text-foreground-muted hover:bg-surface-muted'}`}>
              Todos
            </button>
            {materials.map(m => (
              <button key={m.id} onClick={() => updateParams('material', m.id.toString())}
                className={`block w-full text-left text-sm px-3 py-2 rounded-lg transition-colors ${material === m.id.toString() ? 'bg-primary/15 text-primary font-medium' : 'text-foreground-muted hover:bg-surface-muted'}`}>
                {m.name}
              </button>
            ))}
          </div>
        </div>

        <div>
          <h4 className="font-medium text-sm text-foreground mb-3">Color</h4>
          <div className="flex flex-wrap gap-2">
            {COLOR_OPTIONS.map(c => (
              <button key={c} onClick={() => updateParams('color', color === c ? '' : c)}
                className={`px-3 py-1.5 rounded-full border text-xs transition-colors ${color === c ? 'border-[#C9A227] bg-[#C9A227]/10 text-primary font-medium' : 'border-line text-foreground-muted hover:border-line'}`}>
                {c}
              </button>
            ))}
          </div>
        </div>

        <div>
          <h4 className="font-medium text-sm text-foreground mb-2">Disponibilidad</h4>
          <button onClick={toggleInStock}
            className={`flex items-center gap-2.5 w-full px-3 py-2.5 rounded-lg border text-sm transition-colors ${inStock ? 'border-accent-green bg-accent-green/10 text-accent-dark font-medium' : 'border-line text-foreground-muted hover:border-line'}`}>
            <span className={`w-4 h-4 rounded border flex items-center justify-center transition ${inStock ? 'bg-[#6F8F72] border-accent-green' : 'border-line'}`}>
              {inStock && <Check size={12} className="text-white" />}
            </span>
            Solo en stock
          </button>
        </div>

        <div>
          <h4 className="font-medium text-sm text-foreground mb-3">Precio</h4>
          <div className="flex gap-2">
            <input type="number" placeholder="Mín" value={priceMin} onChange={e => setPriceMin(e.target.value)}
              className="w-full border border-line rounded-lg px-3 py-2 text-sm outline-none focus:border-[#C9A227]" />
            <input type="number" placeholder="Máx" value={priceMax} onChange={e => setPriceMax(e.target.value)}
              className="w-full border border-line rounded-lg px-3 py-2 text-sm outline-none focus:border-[#C9A227]" />
          </div>
        </div>

        {hasFilters && (
          <button onClick={clearFilters} className="text-sm text-[#C9A227] hover:underline w-full text-center py-2 border-t border-line/70">Limpiar filtros</button>
        )}
      </div>
    </>
  )

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <p className="text-[11px] uppercase tracking-[0.2em] text-[#C9A227] font-medium mb-1">Colección</p>
          <h1 className="font-serif text-3xl font-medium text-foreground">{query ? `Resultados para "${query}"` : 'Productos'}</h1>
          <p className="text-sm text-foreground-faint mt-1">{meta.totalElements} productos</p>
        </div>
        <button onClick={() => setFiltersOpen(true)}
          className="flex items-center gap-2 text-sm text-foreground-muted hover:text-[#C9A227] border border-line px-4 py-2 rounded-lg lg:hidden">
          <SlidersHorizontal size={16} /> Filtros
          {activeChips.length > 0 && <span className="w-4 h-4 rounded-full bg-[#C9A227] text-white text-[10px] flex items-center justify-center">{activeChips.length}</span>}
        </button>
      </div>

      <div className="flex gap-8">
        {/* Desktop sidebar */}
        <aside className="hidden lg:block w-64 shrink-0 sticky top-24 self-start max-h-[calc(100vh-7rem)] overflow-y-auto pr-1">
          {filterContent}
        </aside>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 mb-6">
            <select value={`${sort}-${dir}`} onChange={e => { const [s, d] = e.target.value.split('-'); updateParams('sort', s); const p = new URLSearchParams(searchParams); p.set('dir', d); p.set('page', '0'); setSearchParams(p) }}
              className="border border-line rounded-lg px-3 py-2 text-sm outline-none focus:border-[#C9A227] bg-background">
              <option value="createdAt-desc">Más recientes</option>
              <option value="price-asc">Menor precio</option>
              <option value="price-desc">Mayor precio</option>
              <option value="name-asc">Nombre A-Z</option>
              <option value="soldCount-desc">Más vendidos</option>
            </select>
            <p className="text-xs text-foreground-faint hidden sm:block">{meta.totalElements} piezas · {meta.totalPages} páginas</p>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 md:gap-6">
              {Array.from({ length: 6 }).map((_, i) => <ProductCardSkeleton key={i} />)}
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-20">
              <p className="text-lg text-foreground-faint">No se encontraron productos con estos filtros</p>
              <button onClick={clearFilters} className="mt-3 text-sm text-[#C9A227] hover:underline">Ver todos los productos</button>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4 md:gap-6">
                {products.map(p => (
                  <ProductCard key={p.id} product={p}
                    onAddToCart={handleAddToCart}
                    onQuickView={setQuickView}
                  />
                ))}
              </div>
              <Pagination page={meta.page} totalPages={meta.totalPages}
                onPageChange={p => { const params = new URLSearchParams(searchParams); params.set('page', p.toString()); setSearchParams(params) }} />
            </>
          )}
        </div>
      </div>

      {/* Mobile filter drawer */}
      <AnimatePresence>
        {filtersOpen && (
          <>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setFiltersOpen(false)}
              className="fixed inset-0 bg-black/40 z-[90] lg:hidden" />
            <motion.aside initial={{ x: reduced ? 0 : '-100%' }} animate={{ x: 0 }} exit={{ x: reduced ? 0 : '-100%' }} transition={{ type: reduced ? 'tween' : 'spring', damping: 28, stiffness: 300 }}
              className="fixed left-0 top-0 bottom-0 w-[85%] max-w-sm bg-surface z-[95] p-6 overflow-y-auto lg:hidden">
              <div className="flex items-center justify-between mb-6">
                <h3 className="font-serif font-medium text-lg">Filtros</h3>
                <button onClick={() => setFiltersOpen(false)} aria-label="Cerrar filtros"><X size={20} /></button>
              </div>
              {filterContent}
              <button onClick={() => setFiltersOpen(false)}
                className="w-full bg-foreground text-background text-sm font-medium py-3 rounded-lg mt-6">Ver resultados</button>
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      <QuickViewModal product={quickView} onClose={() => setQuickView(null)} onAddToCart={handleAddToCart} />
    </div>
  )
}