import { useEffect, useState } from 'react'
import { Eye } from 'lucide-react'
import { orderService } from '../../services/order.service'
import { useToast } from '../../contexts/ToastContext'
import type { Order, OrderStatus } from '../../types'
import Skeleton from '../../components/Skeleton'
import Modal from '../../components/Modal'
import Button from '../../components/Button'
import Pagination from '../../components/Pagination'
import { formatPrice, formatDate, getStatusLabel } from '../../utils/format'
import { getErrorMessage } from '../../utils/errors'

const statuses: OrderStatus[] = ['PENDING', 'PAID', 'PREPARING', 'SHIPPED', 'DELIVERED', 'CANCELLED']

const transitions: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['PAID', 'CANCELLED'],
  PAID: ['PREPARING', 'CANCELLED'],
  PREPARING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
}

const statusColor: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  PAID: 'bg-blue-100 text-blue-700',
  PREPARING: 'bg-purple-100 text-purple-700',
  SHIPPED: 'bg-indigo-100 text-indigo-700',
  DELIVERED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
}

export default function AdminOrders() {
  const [orders, setOrders] = useState<Order[]>([])
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState<Order | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [updating, setUpdating] = useState(false)
  const { toast } = useToast()

  useEffect(() => { load() }, [page, filter])

  const load = async () => {
    setLoading(true)
    try {
      const data = await orderService.getAllOrders(page, 20, filter || undefined)
      setOrders(data.content); setTotalPages(data.totalPages)
    } catch { setOrders([]); setTotalPages(0) }
    finally { setLoading(false) }
  }

  const openDetail = async (id: number) => {
    setDetailLoading(true)
    setSelected(null)
    try {
      setSelected(await orderService.getAdminOrder(id))
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'No se pudo cargar el pedido'), 'error')
    } finally { setDetailLoading(false) }
  }

  const changeStatus = async (id: number, status: OrderStatus) => {
    setUpdating(true)
    try {
      const updated = await orderService.updateStatus(id, status)
      toast('Estado actualizado')
      setSelected(updated)
      load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al actualizar')
      toast(message || 'Error al actualizar', 'error')
    } finally { setUpdating(false) }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-serif text-2xl font-medium text-foreground">Pedidos</h1>
          <p className="text-sm text-foreground-faint">{orders.length} pedidos en esta página</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button onClick={() => setFilter('')}
            className={`px-4 py-2 rounded-lg text-sm transition ${filter === '' ? 'bg-[#C9A227] text-white' : 'bg-surface border border-line text-foreground-muted hover:border-[#C9A227]'}`}>
            Todos
          </button>
          {statuses.map(s => (
            <button key={s} onClick={() => setFilter(s)}
              className={`px-4 py-2 rounded-lg text-sm transition ${filter === s ? 'bg-[#C9A227] text-white' : 'bg-surface border border-line text-foreground-muted hover:border-[#C9A227]'}`}>
              {getStatusLabel(s)}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <Skeleton className="h-80" />
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Pedido</th>
                  <th className="p-4">Cliente</th>
                  <th className="p-4">Fecha</th>
                  <th className="p-4">Artículos</th>
                  <th className="p-4 text-right">Total</th>
                  <th className="p-4">Estado</th>
                  <th className="p-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr><td colSpan={7} className="p-8 text-center text-foreground-faint">No hay pedidos.</td></tr>
                ) : orders.map(o => (
                  <tr key={o.id} className="border-b border-line/40 last:border-0 hover:bg-surface-muted/50">
                    <td className="p-4 font-medium text-foreground">#{o.orderNumber}</td>
                    <td className="p-4 text-foreground-faint">{o.customerName || '-'}</td>
                    <td className="p-4 text-foreground-faint">{formatDate(o.createdAt)}</td>
                    <td className="p-4 text-foreground-faint">{o.items.reduce((a, i) => a + i.quantity, 0)}</td>
                    <td className="p-4 text-right font-medium">{formatPrice(o.total)}</td>
                    <td className="p-4">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${statusColor[o.status] || 'bg-surface-muted text-foreground-muted'}`}>
                        {getStatusLabel(o.status)}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex justify-end">
                        <button onClick={() => openDetail(o.id)} className="p-2 text-foreground-faint hover:text-[#C9A227]" title="Ver detalle">
                          <Eye size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && <div className="p-4 border-t border-line/70"><Pagination page={page} totalPages={totalPages} onPageChange={setPage} /></div>}
        </div>
      )}

      <Modal isOpen={!!selected || detailLoading} onClose={() => { setSelected(null); setDetailLoading(false) }} title="Detalle del pedido" maxWidth="max-w-3xl">
        {detailLoading || !selected ? (
          <Skeleton className="h-64" />
        ) : (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-serif text-lg text-foreground">#{selected.orderNumber}</p>
                <p className="text-xs text-foreground-faint">{formatDate(selected.createdAt)}</p>
              </div>
              <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusColor[selected.status] || 'bg-surface-muted'}`}>
                {getStatusLabel(selected.status)}
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div className="bg-background-warm rounded-lg p-4">
                <p className="text-xs font-medium text-foreground-faint mb-2">Cliente</p>
                <p className="text-foreground">{selected.customerName || selected.recipientName || '-'}</p>
                <p className="text-foreground-faint">{selected.customerEmail || '-'}</p>
                {selected.phone && <p className="text-foreground-faint">{selected.phone}</p>}
              </div>
              <div className="bg-background-warm rounded-lg p-4">
                <p className="text-xs font-medium text-foreground-faint mb-2">Envío</p>
                <p className="text-foreground">{selected.shippingAddress}</p>
                <p className="text-foreground-faint">
                  {[selected.shippingCity, selected.shippingDepartment].filter(Boolean).join(', ')}
                </p>
                {selected.trackingNumber && <p className="text-foreground-faint">Guía: {selected.trackingNumber}</p>}
              </div>
              <div className="bg-background-warm rounded-lg p-4 md:col-span-2">
                <p className="text-xs font-medium text-foreground-faint mb-2">Pago</p>
                <div className="flex flex-wrap gap-x-6 gap-y-1 text-foreground-faint">
                  <span>Método: <span className="text-foreground">{selected.paymentMethod || '-'}</span></span>
                  <span>Estado pago: <span className="text-foreground">{selected.paymentStatus || '-'}</span></span>
                  {selected.couponCode && <span>Cupón: <span className="text-foreground">{selected.couponCode}</span></span>}
                </div>
              </div>
            </div>

            <div className="border border-line/70 rounded-lg overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-foreground-faint bg-background-warm border-b border-line/70">
                    <th className="p-3">Producto</th>
                    <th className="p-3 text-center">Cant.</th>
                    <th className="p-3 text-right">Precio</th>
                    <th className="p-3 text-right">Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {selected.items.map(item => (
                    <tr key={item.id} className="border-b border-line/40 last:border-0">
                      <td className="p-3">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded bg-background-warm overflow-hidden shrink-0">
                            {item.image ? <img src={item.image} alt={item.name} className="w-full h-full object-cover" /> : <div className="w-full h-full flex items-center justify-center text-foreground/20">J</div>}
                          </div>
                          <div>
                            <p className="text-foreground">{item.name}</p>
                            {item.sku && <p className="text-xs text-foreground-faint">{item.sku}</p>}
                          </div>
                        </div>
                      </td>
                      <td className="p-3 text-center">{item.quantity}</td>
                      <td className="p-3 text-right">{formatPrice(item.price)}</td>
                      <td className="p-3 text-right font-medium">{formatPrice(item.subtotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-end">
              <div className="w-full max-w-xs space-y-1 text-sm">
                <div className="flex justify-between text-foreground-faint"><span>Subtotal</span><span>{formatPrice(selected.subtotal)}</span></div>
                {selected.discount > 0 && <div className="flex justify-between text-foreground-faint"><span>Descuento</span><span>-{formatPrice(selected.discount)}</span></div>}
                <div className="flex justify-between text-foreground-faint"><span>Envío</span><span>{formatPrice(selected.shippingCost)}</span></div>
                <div className="flex justify-between font-serif text-lg text-foreground border-t border-line/70 pt-2"><span>Total</span><span>{formatPrice(selected.total)}</span></div>
              </div>
            </div>

            <div className="border-t border-line/70 pt-4">
              <p className="text-xs font-medium text-foreground-faint mb-3">Cambiar estado</p>
              {transitions[selected.status].length === 0 ? (
                <p className="text-sm text-foreground-faint">Este pedido no admite más cambios de estado.</p>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {transitions[selected.status].map(next => (
                    <Button key={next} variant={next === 'CANCELLED' ? 'danger' : 'primary'}
                      loading={updating} onClick={() => changeStatus(selected.id, next)}>
                      {next === 'CANCELLED' ? 'Cancelar pedido' : `Marcar como ${getStatusLabel(next)}`}
                    </Button>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
