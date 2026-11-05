import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Sparkles, X, Send, Check, AlertTriangle, ShieldCheck } from 'lucide-react'
import { aiAdminService, aiAdminEnabled } from '../../services/aiAdmin.service'
import { adminService } from '../../services/admin.service'
import { orderService } from '../../services/order.service'
import { useToast } from '../../contexts/ToastContext'
import { getErrorMessage } from '../../utils/errors'
import type { AiAdminAction } from '../../types'

const MAX_MESSAGE_LENGTH = 600

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  text: string
  suggestedPrompts?: string[]
  action?: AiAdminAction | null
  actionState?: 'pending' | 'done' | 'error' | 'cancelled'
}

const GREETING: ChatMessage = {
  id: 'greeting',
  role: 'assistant',
  text: 'Hola, soy AURA Admin. Te ayudo a revisar ventas, pedidos, inventario y clientes. '
    + 'Si te propongo un cambio, solo se aplica cuando lo confirmes.',
  suggestedPrompts: [
    '¿Cómo van las ventas de hoy?',
    '¿Qué pedidos están pendientes?',
    '¿Qué productos tienen stock bajo?',
    '¿Cuáles son los productos más vendidos?',
  ],
}

export default function AdminAiAssistant() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([GREETING])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [executingId, setExecutingId] = useState<string | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const { toast } = useToast()

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages, loading, open])

  useEffect(() => {
    if (!open) return
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') setOpen(false) }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  if (!aiAdminEnabled) return null

  const send = async (raw: string) => {
    const text = raw.trim()
    if (!text || loading) return

    setMessages((prev) => [...prev, { id: `u-${Date.now()}`, role: 'user', text }])
    setInput('')
    setLoading(true)

    const history = messages
      .filter((message) => message.id !== GREETING.id)
      .slice(-8)
      .map((message) => ({ role: message.role, content: message.text }))

    try {
      const response = await aiAdminService.ask(text, history)
      setMessages((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: 'assistant',
          text: response.message,
          suggestedPrompts: response.suggestedPrompts,
          action: response.action ?? null,
          actionState: response.action ? 'pending' : undefined,
        },
      ])
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          id: `e-${Date.now()}`,
          role: 'assistant',
          text: getErrorMessage(error, 'No se pudo procesar la consulta. Intenta de nuevo.'),
        },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    void send(input)
  }

  const setActionState = (id: string, state: ChatMessage['actionState']) => {
    setMessages((prev) => prev.map((message) => (message.id === id ? { ...message, actionState: state } : message)))
  }

  const actionId = (value: string | undefined): number | null => {
    const parsed = Number(value)
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null
  }

  const confirmAction = async (message: ChatMessage) => {
    const action = message.action
    if (!action) return

    const orderId = actionId(action.parameters.orderId)
    const userId = actionId(action.parameters.userId)
    const reviewId = actionId(action.parameters.reviewId)

    const invalid =
      (action.type === 'UPDATE_ORDER_STATUS' && (orderId === null || !action.parameters.status)) ||
      (action.type === 'TOGGLE_USER_ACTIVE' && userId === null) ||
      (action.type === 'MODERATE_REVIEW' && (reviewId === null || !action.parameters.status))

    if (invalid) {
      toast('La acción no tiene todos los datos necesarios.', 'error')
      setActionState(message.id, 'error')
      return
    }

    setExecutingId(message.id)
    try {
      if (action.type === 'UPDATE_ORDER_STATUS' && orderId !== null) {
        await orderService.updateStatus(orderId, action.parameters.status)
      } else if (action.type === 'TOGGLE_USER_ACTIVE' && userId !== null) {
        await adminService.toggleUserActive(userId)
      } else if (action.type === 'MODERATE_REVIEW' && reviewId !== null) {
        await adminService.moderateReview(reviewId, action.parameters.status)
      }
      setActionState(message.id, 'done')
      toast('Acción aplicada correctamente', 'success')
      setMessages((prev) => [
        ...prev,
        { id: `ok-${Date.now()}`, role: 'assistant', text: 'Listo, apliqué la acción que confirmaste.' },
      ])
    } catch (error) {
      setActionState(message.id, 'error')
      toast(getErrorMessage(error, 'No se pudo aplicar la acción'), 'error')
    } finally {
      setExecutingId(null)
    }
  }

  const actionLabel = (action: AiAdminAction): string => {
    switch (action.type) {
      case 'UPDATE_ORDER_STATUS':
        return `Cambiar el pedido #${action.parameters.orderId} a ${action.parameters.status}`
      case 'TOGGLE_USER_ACTIVE':
        return `Activar / desactivar al usuario #${action.parameters.userId}`
      case 'MODERATE_REVIEW':
        return `Moderar la reseña #${action.parameters.reviewId} como ${action.parameters.status}`
      default:
        return 'Acción propuesta'
    }
  }

  return (
    <>
      {open && (
        <div
          role="dialog"
          aria-label="Asistente del panel de administración"
          className="fixed bottom-24 right-4 z-50 flex h-[560px] max-h-[78vh] w-[min(24rem,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-line bg-surface shadow-2xl"
        >
          <header className="flex items-center justify-between gap-2 border-b border-line px-4 py-3">
            <div className="flex items-center gap-2">
              <Sparkles size={18} className="text-primary" />
              <div>
                <p className="font-serif text-foreground">AURA Admin</p>
                <p className="flex items-center gap-1 text-[11px] text-foreground-faint">
                  <ShieldCheck size={12} /> Solo lectura · pide confirmación para actuar
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="Cerrar asistente"
              className="text-foreground-faint hover:text-foreground"
            >
              <X size={18} />
            </button>
          </header>

          <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto p-4">
            {messages.map((message) => (
              <div key={message.id} className={message.role === 'user' ? 'flex justify-end' : 'flex justify-start'}>
                <div className="max-w-[88%]">
                  <p
                    className={
                      message.role === 'user'
                        ? 'rounded-2xl rounded-br-sm bg-primary px-3 py-2 text-sm text-white'
                        : 'rounded-2xl rounded-bl-sm bg-surface-muted px-3 py-2 text-sm text-foreground'
                    }
                  >
                    {message.text}
                  </p>

                  {message.suggestedPrompts && message.suggestedPrompts.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {message.suggestedPrompts.map((prompt) => (
                        <button
                          key={prompt}
                          type="button"
                          onClick={() => void send(prompt)}
                          className="rounded-full border border-line px-2.5 py-1 text-xs text-foreground-muted transition hover:border-primary hover:text-foreground"
                        >
                          {prompt}
                        </button>
                      ))}
                    </div>
                  )}

                  {message.action && (
                    <div className="mt-2 rounded-xl border border-[#C9A227]/40 bg-[#C9A227]/5 p-3">
                      <p className="flex items-center gap-1.5 text-xs font-medium text-foreground">
                        <AlertTriangle size={14} className="text-[#C9A227]" /> Acción pendiente de confirmación
                      </p>
                      <p className="mt-1 text-sm text-foreground-muted">{actionLabel(message.action)}</p>
                      {message.action.summary && <p className="mt-0.5 text-xs text-foreground-faint">{message.action.summary}</p>}
                      {message.actionState === 'pending' || message.actionState === undefined ? (
                        <div className="mt-3 flex items-center gap-2">
                          <button
                            type="button"
                            disabled={executingId === message.id}
                            onClick={() => void confirmAction(message)}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-[#29251F] px-3 py-1.5 text-xs font-medium text-[#F8F5EC] transition hover:bg-[#3A342B] disabled:opacity-60"
                          >
                            <Check size={13} /> {executingId === message.id ? 'Aplicando…' : 'Confirmar'}
                          </button>
                          <button
                            type="button"
                            onClick={() => setActionState(message.id, 'cancelled')}
                            className="text-xs text-foreground-faint hover:text-foreground"
                          >
                            Cancelar
                          </button>
                        </div>
                      ) : (
                        <p className={`mt-2 text-xs ${message.actionState === 'done' ? 'text-accent-green' : 'text-foreground-faint'}`}>
                          {message.actionState === 'done'
                            ? 'Acción aplicada.'
                            : message.actionState === 'error'
                              ? 'No se pudo aplicar.'
                              : 'Acción cancelada.'}
                        </p>
                      )}
                    </div>
                  )}

                  {message.suggestedPrompts && message.suggestedPrompts.length > 0 && message.action && (
                    <p className="mt-2 flex items-center gap-1 text-[11px] text-foreground-faint">
                      <AlertTriangle size={12} /> Nada se cambia hasta que confirmes.
                    </p>
                  )}
                </div>
              </div>
            ))}

            {loading && (
              <div className="flex justify-start">
                <p className="rounded-2xl rounded-bl-sm bg-surface-muted px-3 py-2 text-sm text-foreground-faint">
                  Consultando datos de la tienda…
                </p>
              </div>
            )}
          </div>

          <form onSubmit={handleSubmit} className="border-t border-line p-3">
            <div className="flex items-center gap-2">
              <input
                value={input}
                onChange={(event) => setInput(event.target.value)}
                maxLength={MAX_MESSAGE_LENGTH}
                aria-label="Escribe tu consulta para el asistente"
                placeholder="Pregúntale algo al panel…"
                className="min-w-0 flex-1 rounded-full border border-line bg-surface-muted px-3 py-2 text-sm text-foreground outline-none focus:border-[#C9A227]"
              />
              <button
                type="submit"
                disabled={loading || !input.trim()}
                aria-label="Enviar consulta"
                className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-[#29251F] text-[#F8F5EC] disabled:opacity-50"
              >
                <Send size={16} />
              </button>
            </div>
          </form>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        aria-label={open ? 'Cerrar asistente del panel' : 'Abrir asistente del panel'}
        className="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-[#29251F] text-[#F8F5EC] shadow-lg transition-transform hover:scale-105"
      >
        {open ? <X size={22} /> : <Sparkles size={22} />}
      </button>
    </>
  )
}
