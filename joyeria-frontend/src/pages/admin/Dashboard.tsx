import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ShoppingCart, DollarSign, Package, Users, AlertTriangle, Clock, TrendingUp } from 'lucide-react'
import { adminService } from '../../services/admin.service'
import { orderService } from '../../services/order.service'
import type { DashboardStats, Order } from '../../types'
import Skeleton from '../../components/Skeleton'
import { formatPrice, formatDate, getStatusLabel, getStatusColor } from '../../utils/format'

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [recentOrders, setRecentOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([adminService.getStats(), orderService.getAllOrders(0, 6)])
      .then(([s, o]) => { setStats(s); setRecentOrders(o.content); setLoading(false) })
      .catch(() => setLoading(false))
  }, [])

  if (loading) {
    return <div><Skeleton className="h-40" /><Skeleton className="h-72 mt-6" /></div>
  }

  if (!stats) {
    return <p className="text-foreground-faint">No se pudieron cargar las estadísticas.</p>
  }

  const cards = [
    { label: 'Ventas totales', value: formatPrice(stats.totalRevenue), icon: DollarSign, color: 'text-[#C9A227] bg-[#C9A227]/10' },
    { label: 'Ventas hoy', value: formatPrice(stats.todayRevenue ?? 0), icon: TrendingUp, color: 'text-accent-green bg-accent-green/10' },
    { label: 'Ventas del mes', value: formatPrice(stats.monthRevenue ?? 0), icon: TrendingUp, color: 'text-blue-500 bg-blue-50' },
    { label: 'Pedidos', value: stats.totalOrders, icon: ShoppingCart, color: 'text-blue-500 bg-blue-50' },
    { label: 'Productos', value: stats.totalProducts, icon: Package, color: 'text-purple-500 bg-purple-50' },
    { label: 'Clientes', value: stats.totalCustomers ?? 0, icon: Users, color: 'text-accent-green bg-accent-green/10' },
  ]

  const statusEntries = stats.ordersByStatus ? Object.entries(stats.ordersByStatus) : []

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-serif text-2xl font-medium text-foreground">Dashboard</h1>
        <p className="text-sm text-foreground-faint">Resumen general de la tienda</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        {cards.map(c => (
          <div key={c.label} className="bg-surface rounded-lg border border-line/70 p-5">
            <div className={`w-10 h-10 rounded-lg flex items-center justify-center mb-3 ${c.color}`}>
              <c.icon size={20} />
            </div>
            <p className="text-2xl font-semibold text-foreground">{c.value}</p>
            <p className="text-sm text-foreground-faint">{c.label}</p>
          </div>
        ))}
      </div>

      <div className="grid lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-1 bg-surface rounded-lg border border-line/70 p-5">
          <h2 className="font-medium text-foreground mb-4">Pedidos por estado</h2>
          {statusEntries.length === 0 ? (
            <p className="text-sm text-foreground-faint">Sin datos aún.</p>
          ) : (
            <div className="space-y-3">
              {statusEntries.map(([status, count]) => (
                <div key={status} className="flex items-center justify-between text-sm">
                  <span className="text-foreground-muted">{getStatusLabel(status)}</span>
                  <div className="flex items-center gap-3">
                    <div className="w-24 h-1.5 bg-surface-muted rounded-full overflow-hidden">
                      <div className="h-full bg-[#C9A227] rounded-full"
                        style={{ width: `${Math.min(100, (count / Math.max(1, stats.totalOrders)) * 100)}%` }} />
                    </div>
                    <span className="font-medium text-foreground w-6 text-right">{count}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="lg:col-span-2 bg-surface rounded-lg border border-line/70 p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-medium text-foreground">Pedidos recientes</h2>
            <Link to="/admin/pedidos" className="text-sm text-[#C9A227]">Ver todos</Link>
          </div>
          {recentOrders.length === 0 ? (
            <p className="text-sm text-foreground-faint">No hay pedidos recientes.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-foreground-faint border-b border-line/70">
                    <th className="pb-3">Pedido</th>
                    <th className="pb-3">Cliente</th>
                    <th className="pb-3">Fecha</th>
                    <th className="pb-3 text-right">Total</th>
                    <th className="pb-3">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {recentOrders.map(o => (
                    <tr key={o.id} className="border-b border-line/40 last:border-0">
                      <td className="py-3 font-medium text-foreground">#{o.orderNumber}</td>
                      <td className="py-3 text-foreground-faint">{o.customerName || '-'}</td>
                      <td className="py-3 text-foreground-faint">{formatDate(o.createdAt)}</td>
                      <td className="py-3 text-right font-medium">{formatPrice(o.total)}</td>
                      <td className="py-3"><span className={`text-xs px-2.5 py-1 rounded-full ${getStatusColor(o.status)}`}>{getStatusLabel(o.status)}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {stats.topSellingProducts && stats.topSellingProducts.length > 0 && (
        <div className="bg-surface rounded-lg border border-line/70 p-5 mb-6">
          <h2 className="font-medium text-foreground mb-4">Más vendidos</h2>
          <div className="space-y-3">
            {stats.topSellingProducts.map(p => (
              <div key={p.id} className="flex items-center gap-3 text-sm">
                <div className="w-10 h-10 rounded bg-background-warm overflow-hidden shrink-0 flex items-center justify-center">
                  {p.imageUrl ? <img src={p.imageUrl} alt={p.name} className="w-full h-full object-cover" /> : <span className="text-foreground/20">J</span>}
                </div>
                <span className="flex-1 text-foreground truncate">{p.name}</span>
                <span className="text-foreground-faint whitespace-nowrap">{p.soldCount} vendidos</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {(stats.lowStockProducts || stats.outOfStockProducts) ? (
        <div className="bg-background-warm rounded-lg border border-line/70 p-5 flex flex-wrap gap-6 items-center">
          {typeof stats.lowStockProducts === 'number' && (
            <div className="flex items-center gap-2 text-sm"><AlertTriangle size={18} className="text-yellow-500" /> {stats.lowStockProducts} productos con stock bajo</div>
          )}
          {typeof stats.outOfStockProducts === 'number' && (
            <div className="flex items-center gap-2 text-sm"><Clock size={18} className="text-red-400" /> {stats.outOfStockProducts} productos agotados</div>
          )}
        </div>
      ) : null}
    </div>
  )
}