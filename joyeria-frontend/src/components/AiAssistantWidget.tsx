import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Sparkles, X, Send, Plus } from 'lucide-react'
import SmartImage from './SmartImage'
import { aiService, aiAssistantEnabled } from '../services/ai.service'
import { useCart } from '../contexts/CartContext'
import { useToast } from '../contexts/ToastContext'
import { useAuth } from '../contexts/AuthContext'
import { formatPrice } from '../utils/format'
import { getErrorMessage } from '../utils/errors'
import type { AiConversationTurn, AiProductSuggestion } from '../types'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  text: string
  products?: AiProductSuggestion[]
  followUpQuestion?: string
  suggestions?: string[]
}

const GREETING: ChatMessage = {
  id: 'greeting',
  role: 'assistant',
  text: 'Hola, soy AURA. Cuéntame qué buscas: un regalo, un anillo de plata, algo por menos de 200.000… y te mostraré piezas reales del catálogo.',
}

const MAX_MESSAGE_LENGTH = 500

export default function AiAssistantWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([GREETING])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  const { addItem } = useCart()
  const { toast } = useToast()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages, loading, open])

  if (!aiAssistantEnabled) return null

  const send = async (raw: string) => {
    const text = raw.trim()
    if (!text || loading) return

    const userMessage: ChatMessage = { id: `u-${Date.now()}`, role: 'user', text }
    setMessages((prev) => [...prev, userMessage])
    setInput('')
    setLoading(true)

    const history: AiConversationTurn[] = messages
      .filter((message) => message.id !== GREETING.id)
      .slice(-8)
      .map((message) => ({ role: message.role, content: message.text }))

    try {
      const response = await aiService.ask(text, history)
      setMessages((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: 'assistant',
          text: response.message,
          products: response.products,
          followUpQuestion: response.followUpQuestion ?? undefined,
          suggestions: response.suggestions,
        },
      ])
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          id: `e-${Date.now()}`,
          role: 'assistant',
          text: getErrorMessage(error, 'No pudimos procesar tu consulta. Intenta de nuevo en un momento.'),
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

  const handleAdd = async (product: AiProductSuggestion) => {
    if (!isAuthenticated) {
      toast('Inicia sesión para agregar productos al carrito', 'info')
      navigate('/login')
      return
    }
    try {
      await addItem(product.id)
      toast(`${product.name} se agregó al carrito`, 'success')
    } catch {
      toast('No se pudo agregar el producto al carrito', 'error')
    }
  }

  return (
    <>
      {open && (
        <div
          role="dialog"
          aria-label="Asistente de compras AURA"
          className="fixed bottom-24 right-4 z-40 w-[min(22rem,calc(100vw-2rem))] h-[520px] max-h-[75vh] flex flex-col rounded-2xl border border-border bg-surface shadow-2xl overflow-hidden"
        >
          <header className="flex items-center justify-between gap-2 border-b border-border px-4 py-3">
            <div className="flex items-center gap-2">
              <Sparkles size={18} className="text-primary" />
              <span className="font-serif text-foreground">Asistente AURA</span>
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

          <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-3">
            {messages.map((message) => (
              <div key={message.id} className={message.role === 'user' ? 'flex justify-end' : 'flex justify-start'}>
                <div className="max-w-[85%]">
                  <p
                    className={
                      message.role === 'user'
                        ? 'rounded-2xl rounded-br-sm bg-primary px-3 py-2 text-sm text-white'
                        : 'rounded-2xl rounded-bl-sm bg-surface-muted px-3 py-2 text-sm text-foreground'
                    }
                  >
                    {message.text}
                  </p>
                  {message.products && message.products.length > 0 && (
                    <div className="mt-2 space-y-2">
                      {message.products.map((product) => (
                        <div key={product.id} className="flex gap-3 rounded-xl border border-border bg-surface p-2">
                          <SmartImage
                            src={product.image}
                            alt={product.name}
                            className="h-16 w-16 flex-shrink-0 rounded-lg object-cover"
                          />
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-medium text-foreground">{product.name}</p>
                            <p className="text-sm text-primary">{formatPrice(product.price)}</p>
                            {product.reason && (
                              <p className="mt-0.5 line-clamp-2 text-xs text-foreground-faint">{product.reason}</p>
                            )}
                            <div className="mt-1 flex items-center gap-2">
                              {product.slug && (
                                <Link
                                  to={`/productos/${product.slug}`}
                                  onClick={() => setOpen(false)}
                                  className="text-xs text-foreground-muted underline hover:text-foreground"
                                >
                                  Ver
                                </Link>
                              )}
                              <button
                                type="button"
                                onClick={() => handleAdd(product)}
                                disabled={!product.available}
                                aria-label={`Agregar ${product.name} al carrito`}
                                className="inline-flex items-center gap-1 rounded-full bg-primary px-2 py-1 text-xs text-white disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                <Plus size={12} /> Agregar
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                  {message.followUpQuestion && (
                    <p className="mt-1 text-xs text-foreground-muted">{message.followUpQuestion}</p>
                  )}
                  {message.suggestions && message.suggestions.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {message.suggestions.map((suggestion) => (
                        <button
                          key={suggestion}
                          type="button"
                          onClick={() => void send(suggestion)}
                          disabled={loading}
                          className="rounded-full border border-border px-3 py-1 text-xs text-foreground-muted hover:border-primary hover:text-foreground disabled:opacity-50"
                        >
                          {suggestion}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}
            {loading && (
              <div className="flex justify-start">
                <p className="rounded-2xl rounded-bl-sm bg-surface-muted px-3 py-2 text-sm text-foreground-faint">
                  Buscando productos…
                </p>
              </div>
            )}
          </div>

          <form onSubmit={handleSubmit} className="flex items-center gap-2 border-t border-border p-3">
            <input
              type="text"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              maxLength={MAX_MESSAGE_LENGTH}
              aria-label="Escribe tu consulta"
              placeholder="¿Qué estás buscando?"
              className="min-w-0 flex-1 rounded-full border border-border bg-surface-muted px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
            />
            <button
              type="submit"
              disabled={loading || !input.trim()}
              aria-label="Enviar consulta"
              className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-primary text-white disabled:opacity-50"
            >
              <Send size={16} />
            </button>
          </form>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        aria-label={open ? 'Cerrar asistente de compras' : 'Abrir asistente de compras'}
        className="fixed bottom-6 right-6 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-white shadow-lg transition-transform hover:scale-105"
      >
        {open ? <X size={22} /> : <Sparkles size={22} />}
      </button>
    </>
  )
}
