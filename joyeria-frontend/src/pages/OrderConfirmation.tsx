import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CheckCircle, MapPin, CreditCard, ArrowRight, Clock } from 'lucide-react'
import { orderService } from '../services/order.service'
import { useCart } from '../contexts/CartContext'
import type { Order } from '../types'
import Button from '../components/Button'
import OrderTimeline from '../components/OrderTimeline'
import { formatPrice, formatDate, getStatusLabel, getStatusColor } from '../utils/format'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function OrderConfirmation() {
  const { orderNumber } = useParams()
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const { refreshCart } = useCart()

  useDocumentTitle('Pedido confirmado')

  const loadOrder = useCallback(async () => {
    if (!orderNumber) return null
    try {
      const token = sessionStorage.getItem(`guestToken:${orderNumber}`) || undefined
      const o = token
        ? await orderService.getGuestOrder(orderNumber, token)
        : await orderService.getByOrderNumber(orderNumber)
      setOrder(o)
      return o
    } catch {
      return null
    } finally {
      setLoading(false)
    }
  }, [orderNumber])

  useEffect(() => {
    let active = true
    let timer: number | undefined
    let attempts = 0
    const run = async () => {
      const o = await loadOrder()
      attempts += 1
      if (active && o && o.status === 'PENDING' && attempts < 20) {
        timer = window.setTimeout(run, 3000)
      }
    }
    run()
    refreshCart()
    return () => {
      active = false
      if (timer) window.clearTimeout(timer)
    }
  }, [loadOrder, refreshCart])

  if (loading) {
    return <div className="max-w-3xl mx-auto px-4 py-20 text-center"><div className="w-10 h-10 border-2 border-[#C9A227]/30 border-t-[#C9A227] rounded-full animate-spin mx-auto" /></div>
  }

  if (!order) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <h1 className="font-serif text-2xl font-medium text-foreground mb-4">Pedido no encontrado</h1>
        <Link to="/cuenta/pedidos"><Button>Ver mis pedidos</Button></Link>
      </div>
    )
  }

  const isPaid = order.status !== 'PENDING'

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="text-center mb-10">
        {isPaid ? (
          <>
            <div className="w-20 h-20 rounded-full bg-accent-green/10 flex items-center justify-center mx-auto mb-5">
              <CheckCircle size={40} className="text-accent-green" />
            </div>
            <h1 className="font-serif text-3xl font-medium text-foreground mb-2">¡Gracias por tu compra!</h1>
            <p className="text-foreground-faint">Tu pedido <span className="font-medium text-[#C9A227]">#{order.orderNumber}</span> fue confirmado.</p>
            <p className="text-sm text-foreground-faint mt-2">Guarda tu número de pedido para hacer seguimiento.</p>
          </>
        ) : (
          <>
            <div className="w-20 h-20 rounded-full bg-amber-500/10 flex items-center justify-center mx-auto mb-5">
              <Clock size={40} className="text-amber-500" />
            </div>
            <h1 className="font-serif text-3xl font-medium text-foreground mb-2">Estamos confirmando tu pago</h1>
            <p className="text-foreground-faint">Pedido <span className="font-medium text-[#C9A227]">#{order.orderNumber}</span></p>
            <p className="text-sm text-foreground-faint mt-2">
              En cuanto la pasarela confirme el pago, el estado se actualizará automáticamente.
            </p>
          </>
        )}
      </motion.div>

      <div className="bg-surface rounded-lg border border-line/70 p-6 mb-6">
        <h2 className="font-medium text-foreground mb-5 text-sm">Estado del pedido</h2>
        <OrderTimeline status={order.status} />
      </div>

      <div className="bg-surface rounded-lg border border-line/70 p-6 mb-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="font-medium text-foreground">Resumen del pedido</h2>
          <span className={`text-xs px-3 py-1 rounded-full ${getStatusColor(order.status)}`}>{getStatusLabel(order.status)}</span>
        </div>
        <div className="space-y-4">
          {order.items.map(item => (
            <div key={item.id} className="flex items-center gap-4">
              <div className="w-14 h-14 rounded bg-background-warm flex items-center justify-center overflow-hidden shrink-0">
                {item.image ? <img src={item.image} alt={item.name} className="w-full h-full object-cover" /> : <span className="text-foreground/20 text-lg">J</span>}
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium text-foreground">{item.name}</p>
                <p className="text-xs text-foreground-faint">Cantidad: {item.quantity}</p>
              </div>
              <span className="text-sm font-medium">{formatPrice(item.subtotal)}</span>
            </div>
          ))}
        </div>
        <div className="border-t border-line/70 mt-5 pt-4 space-y-2 text-sm">
          <div className="flex justify-between"><span className="text-foreground-faint">Subtotal</span><span>{formatPrice(order.subtotal)}</span></div>
          {order.discount > 0 && <div className="flex justify-between"><span className="text-foreground-faint">Descuento</span><span className="text-accent-green">-{formatPrice(order.discount)}</span></div>}
          <div className="flex justify-between"><span className="text-foreground-faint">Envío</span><span>{order.shippingCost === 0 ? <span className="text-accent-green">Gratis</span> : formatPrice(order.shippingCost)}</span></div>
          <div className="flex justify-between font-semibold text-base pt-2 border-t border-line/70 text-foreground">
            <span>Total</span><span className="text-[#C9A227]">{formatPrice(order.total)}</span>
          </div>
        </div>
      </div>

      <div className="grid md:grid-cols-2 gap-6 mb-8">
        <div className="bg-surface rounded-lg border border-line/70 p-5">
          <div className="flex items-center gap-2 mb-3"><MapPin size={16} className="text-[#C9A227]" /><h3 className="text-sm font-medium text-foreground">Dirección de envío</h3></div>
          <p className="text-sm text-foreground-faint">{order.shippingAddress}</p>
          <p className="text-sm text-foreground-faint">{order.shippingCity}{order.shippingDepartment ? `, ${order.shippingDepartment}` : ''}</p>
          {order.recipientName && <p className="text-sm text-foreground-faint mt-1">{order.recipientName}</p>}
        </div>
        <div className="bg-surface rounded-lg border border-line/70 p-5">
          <div className="flex items-center gap-2 mb-3"><CreditCard size={16} className="text-[#C9A227]" /><h3 className="text-sm font-medium text-foreground">Pago</h3></div>
          <p className="text-sm text-foreground-faint">Método: {order.paymentMethod === 'card' ? 'Tarjeta' : 'PSE'}</p>
          <p className="text-sm text-foreground-faint">Fecha: {formatDate(order.createdAt)}</p>
          {order.trackingNumber && <p className="text-sm text-foreground-faint mt-1">Guía de envío: {order.trackingNumber}</p>}
        </div>
      </div>

      <div className="flex flex-wrap gap-3 justify-center">
        <Link to="/cuenta/pedidos"><Button variant="outline">Ver mis pedidos</Button></Link>
        <Link to="/productos"><Button>Seguir comprando <ArrowRight size={18} /></Button></Link>
      </div>
    </div>
  )
}