import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Package, ChevronRight } from 'lucide-react'
import { orderService } from '../../services/order.service'
import type { Order } from '../../types'
import UserNav from '../../components/UserNav'
import Button from '../../components/Button'
import Pagination from '../../components/Pagination'
import { formatPrice, formatDate, getStatusLabel, getStatusColor } from '../../utils/format'
import useDocumentTitle from '../../hooks/useDocumentTitle'

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)

  useDocumentTitle('Mis pedidos')

  useEffect(() => { load() }, [page])

  const load = async () => {
    setLoading(true)
    try {
      const data = await orderService.getUserOrders(page, 10)
      setOrders(data.content)
      setTotalPages(data.totalPages)
    } catch { setOrders([]) }
    finally { setLoading(false) }
  }

  if (loading && orders.length === 0) {
    return <div className="max-w-4xl mx-auto px-4 py-10"><div className="flex items-center justify-center py-20"><div className="w-10 h-10 border-2 border-[#C9A227]/30 border-t-[#C9A227] rounded-full animate-spin" /></div></div>
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="font-serif text-2xl font-medium text-foreground mb-6">Mis pedidos</h1>
      <UserNav />
      {orders.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center">
          <Package size={40} className="mx-auto mb-3 text-foreground/20" />
          <p className="text-foreground-faint mb-4">Aún no has realizado pedidos</p>
          <Link to="/productos"><Button>Empezar a comprar</Button></Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map(order => (
            <Link key={order.id} to={`/pedido-confirmado/${order.orderNumber}`}>
              <div className="bg-surface rounded-lg border border-line/70 p-5 hover:border-[#C9A227]/40 transition">
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <p className="font-medium text-foreground">Pedido #{order.orderNumber}</p>
                    <p className="text-xs text-foreground-faint">{formatDate(order.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`text-xs px-3 py-1 rounded-full ${getStatusColor(order.status)}`}>{getStatusLabel(order.status)}</span>
                    <ChevronRight size={18} className="text-foreground/20" />
                  </div>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-foreground-faint">{order.items.reduce((a, i) => a + i.quantity, 0)} artículos</span>
                  <span className="font-semibold text-foreground">{formatPrice(order.total)}</span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
      {totalPages > 1 && <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />}
    </div>
  )
}