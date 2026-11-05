import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CreditCard, ShieldAlert } from 'lucide-react'
import { paymentService } from '../services/misc.service'
import { useToast } from '../contexts/ToastContext'
import { getErrorMessage } from '../utils/errors'
import { formatPrice } from '../utils/format'
import Button from '../components/Button'
import type { PaymentStatus } from '../types'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function SandboxPaymentPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { toast } = useToast()

  useDocumentTitle('Pago')
  const reference = params.get('ref') || ''
  const orderNumber = params.get('order') || ''
  const guestToken = orderNumber ? (sessionStorage.getItem(`guestToken:${orderNumber}`) || undefined) : undefined
  const [status, setStatus] = useState<PaymentStatus | null>(null)
  const [paying, setPaying] = useState(false)

  useEffect(() => {
    if (!orderNumber) return
    paymentService.getStatus(orderNumber, guestToken).then(setStatus).catch(() => {})
  }, [orderNumber, guestToken])

  const pay = async () => {
    if (!reference) return
    setPaying(true)
    try {
      const result = await paymentService.confirmSandbox(reference, guestToken)
      toast('Pago confirmado')
      navigate(`/pedido-confirmado/${result.orderNumber}`)
    } catch (err) {
      toast(getErrorMessage(err, 'No se pudo confirmar el pago'), 'error')
    } finally {
      setPaying(false)
    }
  }

  if (!reference) {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        <h1 className="font-serif text-2xl text-foreground mb-4">Referencia de pago inválida</h1>
        <Button onClick={() => navigate('/cuenta/pedidos')}>Ver mis pedidos</Button>
      </div>
    )
  }

  return (
    <div className="max-w-lg mx-auto px-4 sm:px-6 py-16">
      <div className="bg-surface rounded-lg border border-line/70 p-8 text-center">
        <div className="w-16 h-16 rounded-full bg-[#C9A227]/10 flex items-center justify-center mx-auto mb-4">
          <CreditCard size={30} className="text-[#C9A227]" />
        </div>
        <h1 className="font-serif text-2xl font-medium text-foreground mb-2">Pasarela de pago (simulada)</h1>
        <p className="text-sm text-foreground-faint mb-6">
          Pedido <span className="font-medium text-[#C9A227]">#{orderNumber || '—'}</span>
        </p>

        {status && (
          <p className="text-3xl font-semibold text-foreground mb-6">{formatPrice(status.amount ?? 0)}</p>
        )}

        <div className="flex items-start gap-2 text-left bg-background-warm rounded-lg p-4 mb-6">
          <ShieldAlert size={18} className="text-amber-500 shrink-0 mt-0.5" />
          <p className="text-xs text-foreground-faint">
            Entorno de desarrollo: no se realiza ningún cobro real. Al confirmar, el pedido
            se marcará como pagado solo para probar el flujo.
          </p>
        </div>

        <Button className="w-full" size="lg" onClick={pay} loading={paying}>
          Pagar {status ? formatPrice(status.amount ?? 0) : ''}
        </Button>
        <button
          onClick={() => navigate('/cuenta/pedidos')}
          className="text-xs text-foreground-faint hover:text-foreground mt-4"
        >
          Cancelar y ver mis pedidos
        </button>
      </div>
    </div>
  )
}
