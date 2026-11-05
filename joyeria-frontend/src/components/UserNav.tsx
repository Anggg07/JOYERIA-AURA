import { Link, useLocation } from 'react-router-dom'
import { User, Package, MapPin, Heart } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'

export default function UserNav() {
  const location = useLocation()
  const { user } = useAuth()
  const links = [
    { label: 'Mi perfil', to: '/cuenta', icon: User },
    { label: 'Mis pedidos', to: '/cuenta/pedidos', icon: Package },
    { label: 'Direcciones', to: '/cuenta/direcciones', icon: MapPin },
    { label: 'Favoritos', to: '/cuenta/favoritos', icon: Heart },
  ]

  return (
    <div className="bg-surface rounded-lg border border-line/70 p-4 mb-8">
      <div className="flex items-center justify-between mb-4">
        <div>
          <p className="font-medium text-foreground">{user?.firstName} {user?.lastName}</p>
          <p className="text-xs text-foreground-faint">{user?.email}</p>
        </div>
      </div>
      <div className="flex gap-2 flex-wrap">
        {links.map(l => {
          const active = l.to === '/cuenta' ? location.pathname === '/cuenta' : location.pathname.startsWith(l.to)
          return (
            <Link key={l.to} to={l.to}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm transition ${active ? 'bg-[#C9A227]/10 text-[#C9A227] font-medium' : 'text-foreground-muted hover:bg-surface-muted'}`}>
              <l.icon size={16} /> {l.label}
            </Link>
          )
        })}
      </div>
    </div>
  )
}