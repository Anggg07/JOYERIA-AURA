import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CreditCard, MapPin, ShoppingBag, Lock, Check, X } from 'lucide-react'
import { useCart } from '../contexts/CartContext'
import { useToast } from '../contexts/ToastContext'
import { useAuth } from '../contexts/AuthContext'
import { orderService } from '../services/order.service'
import { paymentService } from '../services/misc.service'
import { cartService } from '../services/cart.service'
import Button from '../components/Button'
import SmartImage from '../components/SmartImage'
import useDocumentTitle from '../hooks/useDocumentTitle'
import { formatPrice } from '../utils/format'
import { getErrorMessage } from '../utils/errors'
import { calculateShipping } from '../utils/shipping'
import type { CheckoutSummary } from '../types'

export default function Checkout() {
  const { cart } = useCart()
  const { toast } = useToast()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [loading, setLoading] = useState(false)
  const [coupon, setCoupon] = useState('')
  const [appliedCoupon, setAppliedCoupon] = useState<string | null>(null)
  const [summary, setSummary] = useState<CheckoutSummary | null>(null)
  const [shipping, setShipping] = useState<{ email: string; address: string; city: string; department: string; recipient: string; phone: string }>({ email: '', address: '', city: '', department: '', recipient: '', phone: '' })
  const [payment, setPayment] = useState<{ method: string }>({ method: 'card' })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const cartSubtotal = cart?.subtotal || 0

  useDocumentTitle('Finalizar compra')

  useEffect(() => {
    if (!cart || cart.items.length === 0) { setSummary(null); return }
    if (!isAuthenticated) { setSummary(null); return }
    let active = true
    cartService.getSummary(appliedCoupon || undefined)
      .then(s => { if (active) setSummary(s) })
      .catch(() => { if (active) setSummary(null) })
    return () => { active = false }
  }, [cart, appliedCoupon, isAuthenticated])

  const validateShipping = (): boolean => {
    const errors: Record<string, string> = {}
    if (!isAuthenticated && !shipping.email.trim()) errors.email = 'Ingresa tu correo electrónico'
    if (!shipping.address.trim()) errors.address = 'Ingresa la dirección de envío'
    if (!shipping.city.trim()) errors.city = 'Ingresa la ciudad'
    if (!shipping.recipient.trim()) errors.recipient = 'Ingresa el nombre del destinatario'
    if (!shipping.phone.trim()) {
      errors.phone = 'Ingresa un teléfono de contacto'
    } else if (!/^[+]?[\d\s().-]{7,20}$/.test(shipping.phone.trim())) {
      errors.phone = 'Ingresa un teléfono válido'
    }
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const updateShipping = (field: 'email' | 'address' | 'city' | 'department' | 'recipient' | 'phone', value: string) => {
    setShipping(prev => ({ ...prev, [field]: value }))
    if (fieldErrors[field]) setFieldErrors(prev => { const next = { ...prev }; delete next[field]; return next })
  }

  const applyCoupon = async () => {
    const code = coupon.trim()
    if (!code) return
    try {
      const s = await cartService.getSummary(code)
      setSummary(s)
      setAppliedCoupon(s.couponCode)
      toast('¡Cupón aplicado!')
    } catch (err) {
      toast(getErrorMessage(err, 'Cupón inválido'), 'error')
    }
  }

  const removeCoupon = () => {
    setAppliedCoupon(null)
    setCoupon('')
  }

  const handlePay = async () => {
    if (!validateShipping()) {
      setStep(1)
      toast('Completa los datos de envío antes de pagar', 'error')
      return
    }
    setLoading(true)
    try {
      let order
      if (!isAuthenticated) {
        order = await orderService.createGuestOrder({
          email: shipping.email,
          shippingAddress: shipping.address, shippingCity: shipping.city,
          shippingDepartment: shipping.department, recipientName: shipping.recipient,
          phone: shipping.phone, paymentMethod: payment.method === 'card' ? 'card' : 'pse',
          couponCode: appliedCoupon || undefined, notes: '',
          items: (cart?.items || []).map(i => ({ productId: i.productId, quantity: i.quantity })),
        })
        if (order.guestToken) sessionStorage.setItem(`guestToken:${order.orderNumber}`, order.guestToken)
      } else {
        order = await orderService.createOrder({
          shippingAddress: shipping.address, shippingCity: shipping.city,
          shippingDepartment: shipping.department, recipientName: shipping.recipient,
          phone: shipping.phone, paymentMethod: payment.method === 'card' ? 'card' : 'pse',
          couponCode: appliedCoupon || undefined, notes: ''
        })
      }

      const checkout = await paymentService.createCheckout({
        orderId: order.id, paymentMethod: payment.method, guestToken: order.guestToken,
      })

      if (checkout.paymentStatus === 'APPROVED') {
        navigate(`/pedido-confirmado/${order.orderNumber}`)
      } else if (checkout.provider === 'sandbox') {
        navigate(`/pago/sandbox?ref=${encodeURIComponent(checkout.preferenceId ?? '')}&order=${order.orderNumber}`)
      } else if (checkout.checkoutUrl) {
        window.location.href = checkout.checkoutUrl
      } else {
        toast('No se pudo iniciar el pago. Intenta de nuevo.', 'error')
      }
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'Error al procesar el pago'), 'error')
    } finally { setLoading(false) }
  }

  const subtotal = summary?.subtotal ?? cartSubtotal
  const discount = summary?.discount ?? 0
  const shippingCost = summary?.shippingCost ?? calculateShipping(cartSubtotal)
  const total = summary?.total ?? subtotal - discount + shippingCost


  if (!cart || cart.items.length === 0) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center">
        <h1 className="font-serif text-2xl text-foreground mb-4">No tienes productos en el carrito</h1>
        <Button onClick={() => navigate('/productos')}>Ir a la tienda</Button>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="font-serif text-3xl font-medium text-foreground mb-8">Finalizar compra</h1>

      <div className="flex items-center gap-4 mb-8">
        {[1, 2, 3].map(s => (
          <div key={s} className="flex items-center gap-4">
            <button onClick={() => s < step && setStep(s)} disabled={s >= step}
              className={`w-8 h-8 rounded-full flex items-center justify-center text-sm transition ${step >= s ? 'bg-[#C9A227] text-white' : 'bg-surface-elevated text-foreground-faint'} ${s >= step ? 'cursor-default' : 'hover:opacity-80'}`}>
              {step > s ? <Check size={16} /> : s}
            </button>
            <span className={`text-sm hidden md:block ${step >= s ? 'text-foreground font-medium' : 'text-foreground-faint'}`}>
              {s === 1 ? 'Envío' : s === 2 ? 'Pago' : 'Confirmación'}
            </span>
            {s < 3 && <div className="w-10 h-0.5 bg-surface-elevated" />}
          </div>
        ))}
      </div>

      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          {step === 1 && (
            <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="bg-surface rounded-lg border border-line/70 p-6">
              <div className="flex items-center gap-2 mb-4"><MapPin size={18} className="text-[#C9A227]" /><h2 className="font-medium text-foreground">Datos de envío</h2></div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {!isAuthenticated && (
                  <div className="md:col-span-2"><label className="block text-xs font-medium text-foreground-faint mb-1">Correo electrónico *</label>
                    <input type="email" value={shipping.email} onChange={e => updateShipping('email', e.target.value)} required aria-invalid={!!fieldErrors.email} className={`w-full px-3 py-2.5 border rounded-lg text-sm outline-none focus:border-[#C9A227] ${fieldErrors.email ? 'border-red-400' : 'border-line'}`} placeholder="tu@correo.com" />
                    {fieldErrors.email && <p className="text-xs text-red-500 mt-1">{fieldErrors.email}</p>}</div>
                )}
                <div className="md:col-span-2"><label className="block text-xs font-medium text-foreground-faint mb-1">Dirección *</label>
                  <input value={shipping.address} onChange={e => updateShipping('address', e.target.value)} required aria-invalid={!!fieldErrors.address} className={`w-full px-3 py-2.5 border rounded-lg text-sm outline-none focus:border-[#C9A227] ${fieldErrors.address ? 'border-red-400' : 'border-line'}`} placeholder="Calle 123 # 45-67, Apto 301" />
                  {fieldErrors.address && <p className="text-xs text-red-500 mt-1">{fieldErrors.address}</p>}</div>
                <div><label className="block text-xs font-medium text-foreground-faint mb-1">Ciudad *</label>
                  <input value={shipping.city} onChange={e => updateShipping('city', e.target.value)} required aria-invalid={!!fieldErrors.city} className={`w-full px-3 py-2.5 border rounded-lg text-sm outline-none focus:border-[#C9A227] ${fieldErrors.city ? 'border-red-400' : 'border-line'}`} placeholder="Bogotá" />
                  {fieldErrors.city && <p className="text-xs text-red-500 mt-1">{fieldErrors.city}</p>}</div>
                <div><label className="block text-xs font-medium text-foreground-faint mb-1">Departamento</label>
                  <input value={shipping.department} onChange={e => updateShipping('department', e.target.value)} className="w-full px-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227]" placeholder="Cundinamarca" /></div>
                <div><label className="block text-xs font-medium text-foreground-faint mb-1">Destinatario *</label>
                  <input value={shipping.recipient} onChange={e => updateShipping('recipient', e.target.value)} required aria-invalid={!!fieldErrors.recipient} className={`w-full px-3 py-2.5 border rounded-lg text-sm outline-none focus:border-[#C9A227] ${fieldErrors.recipient ? 'border-red-400' : 'border-line'}`} placeholder="María García" />
                  {fieldErrors.recipient && <p className="text-xs text-red-500 mt-1">{fieldErrors.recipient}</p>}</div>
                <div><label className="block text-xs font-medium text-foreground-faint mb-1">Teléfono *</label>
                  <input value={shipping.phone} onChange={e => updateShipping('phone', e.target.value)} required aria-invalid={!!fieldErrors.phone} className={`w-full px-3 py-2.5 border rounded-lg text-sm outline-none focus:border-[#C9A227] ${fieldErrors.phone ? 'border-red-400' : 'border-line'}`} placeholder="+57 300 000 0000" />
                  {fieldErrors.phone && <p className="text-xs text-red-500 mt-1">{fieldErrors.phone}</p>}</div>
              </div>
              <Button className="mt-6 w-full" size="lg" onClick={() => { if (validateShipping()) setStep(2) }}>Continuar al pago</Button>
            </motion.div>
          )}

          {step === 2 && (
            <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="bg-surface rounded-lg border border-line/70 p-6">
              <div className="flex items-center gap-2 mb-4"><CreditCard size={18} className="text-[#C9A227]" /><h2 className="font-medium text-foreground">Método de pago</h2></div>
              <div className="grid grid-cols-2 gap-3 mb-6">
                {['card', 'pse'].map(m => (
                  <button key={m} onClick={() => setPayment({ ...payment, method: m })}
                    className={`p-4 rounded-lg border-2 text-center transition ${payment.method === m ? 'border-[#C9A227] bg-[#C9A227]/5' : 'border-line hover:border-line'}`}>
                    <p className="font-medium text-sm text-foreground">{m === 'card' ? 'Tarjeta de crédito/débito' : 'PSE (Débito)'}</p>
                    <p className="text-xs text-foreground-faint mt-1">{m === 'card' ? 'Visa, Mastercard, Amex' : 'Transferencia bancaria'}</p>
                  </button>
                ))}
              </div>
              {payment.method === 'card' ? (
                <div className="p-6 bg-background-warm rounded-lg text-center">
                  <p className="text-sm font-medium text-foreground mb-1">Pago seguro con tarjeta</p>
                  <p className="text-xs text-foreground-faint flex items-center justify-center gap-1.5">
                    <Lock size={12} /> Serás redirigido a la pasarela de pago. No almacenamos ni transmitimos datos de tarjetas.
                  </p>
                </div>
              ) : (
                <div className="p-6 bg-background-warm rounded-lg text-center">
                  <p className="text-sm font-medium text-foreground mb-1">PSE (Débito)</p>
                  <p className="text-xs text-foreground-faint">Serás redirigido a tu banco para completar la transferencia.</p>
                </div>
              )}
              <div className="flex gap-3 mt-6">
                <Button variant="outline" onClick={() => setStep(1)}>Volver</Button>
                <Button size="lg" className="flex-1" onClick={handlePay} loading={loading}>
                  Pagar {formatPrice(total)}
                </Button>
              </div>
            </motion.div>
          )}

          {step === 3 && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-surface rounded-lg border border-line/70 p-8 text-center">
              <div className="w-16 h-16 rounded-full bg-accent-green/10 flex items-center justify-center mx-auto mb-4">
                <Check size={32} className="text-accent-green" />
              </div>
              <h2 className="font-serif text-2xl font-medium text-foreground mb-2">¡Gracias por tu compra!</h2>
              <p className="text-foreground-faint mb-6">Tu pedido ha sido confirmado y está siendo procesado.</p>
              <Button onClick={() => navigate('/cuenta/pedidos')}>Ver mis pedidos</Button>
            </motion.div>
          )}
        </div>

        <div className="lg:col-span-1">
          <div className="bg-surface rounded-lg border border-line/70 p-5 sticky top-24">
            <div className="flex items-center gap-2 mb-4"><ShoppingBag size={16} className="text-[#C9A227]" /><h3 className="font-medium text-sm text-foreground">Tu pedido</h3></div>
            <div className="space-y-3 max-h-60 overflow-y-auto mb-4">
              {cart.items.map(item => (
                <div key={item.id} className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded bg-background-warm flex items-center justify-center text-[10px] text-foreground/20 shrink-0">
                    <SmartImage src={item.productImage} alt={item.productName} className="w-full h-full object-cover rounded" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-medium text-foreground truncate">{item.productName}</p>
                    <p className="text-xs text-foreground-faint">x{item.quantity}</p>
                  </div>
                  <span className="text-xs font-medium">{formatPrice(item.subtotal)}</span>
                </div>
              ))}
            </div>
            <div className="border-t border-line/70 pt-3 mb-4">
              {appliedCoupon ? (
                <div className="flex items-center justify-between text-xs mb-3">
                  <span className="text-accent-green">Cupón {appliedCoupon} aplicado</span>
                  <button onClick={removeCoupon} className="flex items-center gap-1 text-foreground-faint hover:text-red-500">
                    <X size={12} /> Quitar
                  </button>
                </div>
              ) : (
                <div className="flex gap-2 mb-3">
                  <input value={coupon} onChange={e => setCoupon(e.target.value)} placeholder="Código de cupón"
                    className="flex-1 px-3 py-2 border border-line rounded-lg text-xs outline-none focus:border-[#C9A227]" />
                  <button onClick={applyCoupon} className="text-xs text-[#C9A227] font-medium">Aplicar</button>
                </div>
              )}
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span className="text-foreground-faint">Subtotal</span><span>{formatPrice(subtotal)}</span></div>
              {discount > 0 && <div className="flex justify-between"><span className="text-foreground-faint">Descuento</span><span className="text-accent-green">-{formatPrice(discount)}</span></div>}
              <div className="flex justify-between"><span className="text-foreground-faint">Envío</span><span>{shippingCost === 0 ? <span className="text-accent-green">Gratis</span> : formatPrice(shippingCost)}</span></div>
              <div className="border-t border-line/70 pt-2 flex justify-between font-semibold text-foreground">
                <span>Total</span><span className="text-[#C9A227]">{formatPrice(total)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
