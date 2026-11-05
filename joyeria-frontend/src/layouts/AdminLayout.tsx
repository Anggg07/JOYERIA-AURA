import { useEffect, useState } from 'react'
import { Outlet, Link, useLocation } from 'react-router-dom'
import { LayoutDashboard, Package, FolderTree, Gem, ShoppingCart, Users, Star, LifeBuoy, Ticket, Link2, Menu, X } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import useDocumentTitle from '../hooks/useDocumentTitle'
import AdminAiAssistant from '../components/admin/AdminAiAssistant'

const menu = [
  { label: 'Dashboard', to: '/admin', icon: LayoutDashboard },
  { label: 'Productos', to: '/admin/productos', icon: Package },
  { label: 'Categorías', to: '/admin/categorias', icon: FolderTree },
  { label: 'Materiales', to: '/admin/materiales', icon: Gem },
  { label: 'Pedidos', to: '/admin/pedidos', icon: ShoppingCart },
  { label: 'Usuarios', to: '/admin/usuarios', icon: Users },
  { label: 'Reseñas', to: '/admin/resenas', icon: Star },
  { label: 'Soporte', to: '/admin/soporte', icon: LifeBuoy },
  { label: 'Cupones', to: '/admin/cupones', icon: Ticket },
  { label: 'Redes Sociales', to: '/admin/redes-sociales', icon: Link2 },
]

export default function AdminLayout() {
  const location = useLocation()
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)

  useDocumentTitle('Panel Admin')

  useEffect(() => { setOpen(false) }, [location.pathname])

  useEffect(() => {
    if (!open) return
    document.body.style.overflow = 'hidden'
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false) }
    document.addEventListener('keydown', onKey)
    return () => { document.body.style.overflow = ''; document.removeEventListener('keydown', onKey) }
  }, [open])

  return (
    <div className="min-h-screen flex bg-background">
      {/* Mobile top bar */}
      <header className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-surface border-b border-line flex items-center gap-3 px-4">
        <button onClick={() => setOpen(true)} aria-label="Abrir menú" className="p-1.5 -ml-1.5 text-foreground">
          <Menu size={22} />
        </button>
        <Link to="/admin" className="font-serif text-lg font-medium text-foreground">
          <span className="text-primary">J</span>oyería <span className="text-xs font-sans text-foreground-faint">Admin</span>
        </Link>
      </header>

      {/* Overlay */}
      {open && (
        <div className="lg:hidden fixed inset-0 z-40 bg-black/40" onClick={() => setOpen(false)} aria-hidden />
      )}

      <aside className={`fixed lg:static inset-y-0 left-0 z-50 w-64 bg-surface border-r border-line flex flex-col shrink-0 transition-transform duration-300 lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="px-5 py-5 border-b border-line/70 flex items-center justify-between">
          <Link to="/" className="font-serif text-lg font-medium text-foreground">
            <span className="text-primary">J</span>oyería <span className="text-xs font-sans text-foreground-faint">Admin</span>
          </Link>
          <button onClick={() => setOpen(false)} aria-label="Cerrar menú" className="lg:hidden p-1 text-foreground-faint hover:text-foreground">
            <X size={20} />
          </button>
        </div>
        <nav className="flex-1 py-4 px-3 space-y-0.5 overflow-y-auto">
          {menu.map(item => {
            const active = item.to === '/admin' ? location.pathname === '/admin' : location.pathname.startsWith(item.to)
            return (
              <Link key={item.to} to={item.to}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition ${active ? 'bg-primary/10 text-primary font-medium' : 'text-foreground-muted hover:bg-surface-muted'}`}>
                <item.icon size={18} /> {item.label}
              </Link>
            )
          })}
        </nav>
        <div className="p-4 border-t border-line/70">
          <div className="text-sm font-medium text-foreground mb-1">{user?.firstName}</div>
          <div className="text-xs text-foreground-faint mb-3">{user?.email}</div>
          <button onClick={logout} className="text-xs text-red-500 hover:text-red-600">Cerrar sesión</button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto">
        <div className="p-4 sm:p-6 lg:p-8 pt-20 lg:pt-8">
          <Outlet />
        </div>
      </main>

      <AdminAiAssistant />
    </div>
  )
}
