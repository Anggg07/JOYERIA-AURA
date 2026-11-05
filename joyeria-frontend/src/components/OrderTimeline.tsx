import { Package, CreditCard, Wrench, Truck, Home, XCircle } from 'lucide-react'

const STEPS = [
  { key: 'PENDING', label: 'Pedido realizado', icon: Package },
  { key: 'PAID', label: 'Pago confirmado', icon: CreditCard },
  { key: 'PREPARING', label: 'En preparación', icon: Wrench },
  { key: 'SHIPPED', label: 'Enviado', icon: Truck },
  { key: 'DELIVERED', label: 'Entregado', icon: Home },
]

export default function OrderTimeline({ status }: { status: string }) {
  if (status === 'CANCELLED') {
    return (
      <div className="flex items-center gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        <XCircle size={18} className="shrink-0" />
        <span>Este pedido fue cancelado.</span>
      </div>
    )
  }

  const currentIndex = Math.max(0, STEPS.findIndex(s => s.key === status))

  return (
    <div className="-mx-1 overflow-x-auto px-1">
      <ol className="flex items-center w-full min-w-[520px]">
      {STEPS.map((step, i) => {
        const done = i < currentIndex || status === 'DELIVERED'
        const active = i === currentIndex && status !== 'DELIVERED'
        const reached = i <= currentIndex
        const Icon = step.icon
        return (
          <li key={step.key} className="flex-1 flex flex-col items-center relative">
            {i > 0 && (
              <span className={`absolute top-4 right-1/2 w-full h-0.5 ${reached ? 'bg-[#C9A227]' : 'bg-line'}`} aria-hidden="true" />
            )}
            <span
              className={`relative z-10 w-8 h-8 rounded-full flex items-center justify-center transition ${
                done || active ? 'bg-[#C9A227] text-white' : 'bg-surface-elevated text-foreground-faint'
              }`}
            >
              <Icon size={15} />
            </span>
            <span className={`mt-2 text-[11px] text-center leading-tight ${reached ? 'text-foreground font-medium' : 'text-foreground-faint'}`}>
              {step.label}
            </span>
          </li>
        )
      })}
      </ol>
    </div>
  )
}
