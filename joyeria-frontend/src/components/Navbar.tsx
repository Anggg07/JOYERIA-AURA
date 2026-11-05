import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion'
import { Menu, X, Search, ShoppingCart, Heart, User, LogOut, ChevronDown, ArrowRight, Gift } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'
import { categoryService } from '../services/category.service'
import { materialService } from '../services/material.service'
import { productService } from '../services/product.service'
import type { Category, Material } from '../types'
import Logo from './Logo'
import SmartImage from './SmartImage'
import ThemeToggle from './ThemeToggle'

const NAV_LINKS = [
  { label: 'Inicio', to: '/' },
  { label: 'Colecciones', to: '/productos?sort=createdAt&dir=desc' },
  { label: 'Nosotros', to: '/nosotros' },
  { label: 'Personalizados', to: '/personaliza' },
]

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth()
  const { itemCount } = useCart()
  const [scrolled, setScrolled] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [megaOpen, setMegaOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [categories, setCategories] = useState<Category[]>([])
  const [materials, setMaterials] = useState<Material[]>([])
  const [promoImage, setPromoImage] = useState<string | undefined>(undefined)
  const [shopAccordion, setShopAccordion] = useState(false)
  const reduced = useReducedMotion()
  const navigate = useNavigate()

  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 12)
    window.addEventListener('scroll', handler, { passive: true })
    return () => window.removeEventListener('scroll', handler)
  }, [])

  useEffect(() => {
    categoryService.getAll().then(setCategories).catch(() => {})
    materialService.getAll().then(setMaterials).catch(() => {})
    productService.getFeatured()
      .then(list => {
        const url = list.find(p => p.images?.[0]?.url)?.images?.[0]?.url
        setPromoImage(url)
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [mobileOpen])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchQuery.trim()) {
      navigate(`/productos?q=${encodeURIComponent(searchQuery.trim())}`)
      setSearchQuery('')
    }
  }

  const closeAll = () => { setMobileOpen(false); setMegaOpen(false) }

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 bg-background transition-all duration-300 ${scrolled ? 'border-b border-line/70 bg-background/95 backdrop-blur-md' : 'border-b border-transparent'}`}>
      <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
        <div className="grid h-[76px] grid-cols-[1fr_auto_1fr] items-center gap-4">

          {/* Left — menu (mobile) + primary navigation */}
          <div className="flex min-w-0 items-center">
            <button
              onClick={() => setMobileOpen(v => !v)}
              aria-label={mobileOpen ? 'Cerrar menú' : 'Abrir menú'}
              aria-expanded={mobileOpen}
              className="-ml-2 inline-flex h-11 w-11 items-center justify-center text-foreground transition-colors hover:text-gold xl:hidden">
              {mobileOpen ? <X size={22} /> : <Menu size={22} />}
            </button>

            <nav className="hidden min-w-0 items-center gap-8 xl:flex" aria-label="Principal">
              <Link to="/" className="text-[13px] font-medium text-foreground transition-colors hover:text-gold">
                Inicio
              </Link>

              <div className="relative" onMouseLeave={() => setMegaOpen(false)}>
                <button
                  type="button"
                  onMouseEnter={() => setMegaOpen(true)}
                  onClick={() => setMegaOpen(v => !v)}
                  aria-expanded={megaOpen}
                  className="inline-flex items-center gap-1 text-[13px] font-medium text-foreground transition-colors hover:text-gold">
                  Joyas
                  <ChevronDown size={14} className={`transition-transform duration-300 ${megaOpen ? 'rotate-180' : ''}`} />
                </button>
                <AnimatePresence>
                  {megaOpen && (
                    <motion.div
                      initial={{ opacity: 0, y: reduced ? 0 : 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: reduced ? 0 : 6 }}
                      transition={{ duration: 0.18 }}
                      className="absolute left-0 top-full pt-4">
                      <div className="grid w-[840px] grid-cols-[1.05fr_1fr_1.15fr] gap-x-10 gap-y-8 border border-line/70 bg-surface p-8 shadow-xl">
                        {/* Columna 1 — Categorías */}
                        <div>
                          <p className="mb-4 text-[11px] font-medium uppercase tracking-[0.22em] text-gold">Categorías</p>
                          <ul className="flex flex-col gap-2.5">
                            {categories.map(c => (
                              <li key={c.id}>
                                <Link to={`/productos?category=${c.id}`} onClick={closeAll}
                                  className="block text-[15px] leading-snug text-foreground-muted transition-colors hover:text-gold focus-visible:text-gold">
                                  {c.name}
                                </Link>
                              </li>
                            ))}
                          </ul>
                        </div>

                        {/* Columna 2 — Materiales */}
                        <div>
                          <p className="mb-4 text-[11px] font-medium uppercase tracking-[0.22em] text-gold">Materiales</p>
                          <ul className="flex flex-col gap-2.5">
                            {materials.map(m => (
                              <li key={m.id}>
                                <Link to={`/productos?material=${m.id}`} onClick={closeAll}
                                  className="block text-[15px] leading-snug text-foreground-muted transition-colors hover:text-gold focus-visible:text-gold">
                                  {m.name}
                                </Link>
                              </li>
                            ))}
                          </ul>
                        </div>

                        {/* Columna 3 — Destacado */}
                        <div className="flex flex-col">
                          <p className="mb-4 text-[11px] font-medium uppercase tracking-[0.22em] text-gold">Destacado</p>
                          <Link to="/productos" onClick={closeAll} className="group flex flex-1 flex-col">
                            <div className="aspect-[4/3] w-full overflow-hidden bg-surface-muted">
                              <SmartImage
                                src={promoImage}
                                alt="Piezas destacadas de AURA"
                                className="h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-[1.05]"
                              />
                            </div>
                            <h3 className="mt-4 font-serif text-lg font-medium leading-snug text-foreground">Piezas que perduran</h3>
                            <p className="mt-1.5 text-[13px] leading-relaxed text-foreground-muted">
                              Joyas hechas a mano para acompañar tus historias.
                            </p>
                            <span className="mt-3 inline-flex items-center gap-1.5 text-[12px] font-medium uppercase tracking-[0.18em] text-foreground transition-colors group-hover:text-gold">
                              Ver todo el catálogo
                              <ArrowRight size={13} className="transition-transform duration-300 group-hover:translate-x-1" />
                            </span>
                          </Link>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              {NAV_LINKS.slice(1).map(l => (
                <Link key={l.label} to={l.to} className="text-[13px] font-medium text-foreground transition-colors hover:text-gold">
                  {l.label}
                </Link>
              ))}
            </nav>
          </div>

          {/* Center — wordmark */}
          <Link to="/" aria-label="AURA — Inicio" className="justify-self-center" onClick={closeAll}>
            <Logo compact className="h-9 w-auto sm:h-10" />
          </Link>

          {/* Right — actions */}
          <div className="flex items-center justify-end gap-0.5 sm:gap-1">
            <Link to="/gift-finder" aria-label="Encuentra el regalo"
              className="hidden h-11 w-11 items-center justify-center text-foreground-muted transition-colors hover:text-gold lg:inline-flex">
              <Gift size={19} />
            </Link>

            <form onSubmit={handleSearch} className="hidden items-center border-b border-line/70 focus-within:border-gold md:flex">
              <Search size={15} className="mr-2 text-foreground-faint" />
              <input
                type="search"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="Buscar"
                aria-label="Buscar productos"
                className="w-28 bg-transparent py-1 text-sm text-foreground outline-none placeholder:text-foreground-faint lg:w-40" />
            </form>

            <ThemeToggle className="hidden lg:flex" />

            {isAuthenticated && (
              <Link to="/cuenta/favoritos" className="relative hidden h-11 w-11 items-center justify-center text-foreground-muted transition-colors hover:text-gold sm:inline-flex" aria-label="Favoritos">
                <Heart size={19} />
              </Link>
            )}

            <Link to="/carrito" className="relative inline-flex h-11 w-11 items-center justify-center text-foreground transition-colors hover:text-gold" aria-label="Carrito">
              <ShoppingCart size={20} />
              {itemCount > 0 && (
                <span className="absolute right-1 top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-gold px-1 text-[10px] font-semibold text-white">
                  {itemCount > 9 ? '9+' : itemCount}
                </span>
              )}
            </Link>

            {isAuthenticated ? (
              <div className="group relative hidden xl:block">
                <button className="inline-flex h-11 items-center gap-1.5 px-2 text-foreground-muted transition-colors hover:text-gold" aria-label="Cuenta">
                  <User size={19} /> <ChevronDown size={14} />
                </button>
                <div className="invisible absolute right-0 top-full w-52 pt-2 opacity-0 transition-all group-hover:visible group-hover:opacity-100">
                  <div className="border border-line/70 bg-surface py-2 shadow-lg">
                    <p className="border-b border-line/70 px-4 py-2 text-sm font-medium text-foreground">{user?.firstName} {user?.lastName}</p>
                    <Link to="/cuenta" className="block px-4 py-2 text-sm text-foreground-muted hover:bg-surface-muted">Mi cuenta</Link>
                    <Link to="/cuenta/pedidos" className="block px-4 py-2 text-sm text-foreground-muted hover:bg-surface-muted">Mis pedidos</Link>
                    <Link to="/cuenta/favoritos" className="block px-4 py-2 text-sm text-foreground-muted hover:bg-surface-muted">Mis favoritos</Link>
                    {isAdmin && <Link to="/admin" className="block px-4 py-2 text-sm text-gold hover:bg-surface-muted">Panel admin</Link>}
                    <button onClick={() => logout()} className="flex w-full items-center gap-1.5 px-4 py-2 text-sm text-foreground-muted hover:bg-surface-muted">
                      <LogOut size={14} /> Cerrar sesión
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <Link to="/login" aria-label="Iniciar sesión"
                className="hidden h-11 w-11 items-center justify-center text-foreground transition-colors hover:text-gold xl:inline-flex">
                <User size={19} />
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* ===== Mobile drawer ===== */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0, y: reduced ? 0 : -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: reduced ? 0 : -8 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 top-[76px] z-40 overflow-y-auto bg-background xl:hidden">
            <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
              <form onSubmit={(e) => { handleSearch(e); setMobileOpen(false) }} className="mb-6 flex items-center border-b border-line focus-within:border-gold">
                <Search size={17} className="mr-3 text-foreground-faint" />
                <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)} placeholder="Buscar piezas"
                  className="w-full bg-transparent py-2.5 outline-none placeholder:text-foreground-faint" />
              </form>

              <nav className="flex flex-col" aria-label="Menú">
                <Link to="/" onClick={closeAll} className="border-b border-line/60 py-3.5 font-serif text-lg text-foreground">Inicio</Link>

                <button type="button" onClick={() => setShopAccordion(v => !v)} aria-expanded={shopAccordion}
                  className="flex items-center justify-between border-b border-line/70 py-3 text-left font-serif text-lg">
                  Joyas
                  <ChevronDown size={16} className={`text-gold transition-transform duration-300 ${shopAccordion ? 'rotate-180' : ''}`} />
                </button>
                <AnimatePresence initial={false}>
                  {shopAccordion && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.25 }}
                      className="overflow-hidden">
                      <div className="grid grid-cols-1 gap-x-6 gap-y-6 py-5 sm:grid-cols-2">
                        <div>
                          <p className="mb-3 text-[11px] font-medium uppercase tracking-[0.22em] text-gold">Categorías</p>
                          <div className="flex flex-col">
                            {categories.map(c => (
                              <Link key={c.id} to={`/productos?category=${c.id}`} onClick={closeAll}
                                className="block py-2.5 text-[15px] leading-snug text-foreground-muted transition-colors hover:text-gold focus-visible:text-gold">{c.name}</Link>
                            ))}
                          </div>
                        </div>
                        <div>
                          <p className="mb-3 text-[11px] font-medium uppercase tracking-[0.22em] text-gold">Materiales</p>
                          <div className="flex flex-col">
                            {materials.map(m => (
                              <Link key={m.id} to={`/productos?material=${m.id}`} onClick={closeAll}
                                className="block py-2.5 text-[15px] leading-snug text-foreground-muted transition-colors hover:text-gold focus-visible:text-gold">{m.name}</Link>
                            ))}
                          </div>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>

              {NAV_LINKS.slice(1).map(l => (
                <Link key={l.label} to={l.to} onClick={closeAll}
                  className="border-b border-line/60 py-4 font-serif text-lg text-foreground transition-colors hover:text-gold">
                  {l.label}
                </Link>
              ))}
              <Link to="/gift-finder" onClick={closeAll}
                className="border-b border-line/60 py-4 font-serif text-lg text-foreground transition-colors hover:text-gold">
                Encuentra el regalo
              </Link>
                <Link to="/soporte" onClick={closeAll}
                  className="border-b border-line/60 py-4 font-serif text-lg text-foreground transition-colors hover:text-gold">
                  Soporte
                </Link>
              </nav>

              <div className="mt-6 flex items-center justify-between">
                <span className="text-[11px] font-medium uppercase tracking-[0.22em] text-foreground-faint">Tema</span>
                <ThemeToggle />
              </div>

              <div className="mt-6 space-y-1 border-t border-line/70 pt-6">
                {isAuthenticated ? (
                  <>
                    <Link to="/cuenta" onClick={closeAll} className="block py-2 text-sm font-medium text-foreground-muted">Mi cuenta</Link>
                    <Link to="/cuenta/pedidos" onClick={closeAll} className="block py-2 text-sm text-foreground-muted">Mis pedidos</Link>
                    <Link to="/cuenta/favoritos" onClick={closeAll} className="block py-2 text-sm text-foreground-muted">Mis favoritos</Link>
                    {isAdmin && <Link to="/admin" onClick={closeAll} className="block py-2 text-sm text-gold">Panel admin</Link>}
                    <button onClick={() => { logout(); closeAll() }} className="flex items-center gap-1.5 py-2 text-sm text-foreground-muted">
                      <LogOut size={15} /> Cerrar sesión
                    </button>
                  </>
                ) : (
                  <Link to="/login" onClick={closeAll} className="block py-2 text-sm font-medium text-gold">Iniciar sesión</Link>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  )
}
