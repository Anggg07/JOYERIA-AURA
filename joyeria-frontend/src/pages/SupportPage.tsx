import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { LifeBuoy, Send, CheckCircle, MessageSquare, SendHorizonal } from 'lucide-react'
import { userService } from '../services/user.service'
import { useToast } from '../contexts/ToastContext'
import { useAuth } from '../contexts/AuthContext'
import type { SupportTicket } from '../types'
import Button from '../components/Button'
import { formatDate, getStatusLabel, getStatusColor } from '../utils/format'
import { getErrorMessage } from '../utils/errors'
import useDocumentTitle from '../hooks/useDocumentTitle'

const categories = ['Consulta de producto', 'Estado de pedido', 'Devolución o cambio', 'Pago y facturación', 'Otro']

export default function SupportPage() {
  const { isAuthenticated } = useAuth()
  const [ticket, setTicket] = useState({ subject: '', message: '', category: categories[0] })
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)
  const [tickets, setTickets] = useState<SupportTicket[]>([])
  const [openTicket, setOpenTicket] = useState<SupportTicket | null>(null)
  const [reply, setReply] = useState('')
  const [sending, setSending] = useState(false)
  const { toast } = useToast()

  useDocumentTitle('Soporte')

  useEffect(() => {
    if (!isAuthenticated) { setTickets([]); return }
    userService.getSupportTickets()
      .then(d => setTickets(d.content))
      .catch(() => setTickets([]))
  }, [isAuthenticated, sent])

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await userService.createSupportTicket(ticket)
      setSent(true)
      toast('Ticket creado: te contactaremos pronto')
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al enviar el ticket')
      toast(message || 'Error al enviar el ticket', 'error')
    } finally { setLoading(false) }
  }

  const openThread = async (t: SupportTicket) => {
    try {
      setOpenTicket(await userService.getSupportTicket(t.ticketNumber))
      setReply('')
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'No se pudo abrir el ticket'), 'error')
    }
  }

  const sendReply = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!openTicket || !reply.trim()) return
    setSending(true)
    try {
      const updated = await userService.replyToSupportTicket(openTicket.ticketNumber, reply.trim())
      setOpenTicket(updated)
      setReply('')
      toast('Respuesta enviada')
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'No se pudo enviar la respuesta'), 'error')
    } finally { setSending(false) }
  }

  const input = 'w-full px-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition'

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="text-center mb-10">
        <div className="w-16 h-16 rounded-full bg-[#C9A227]/10 flex items-center justify-center mx-auto mb-4">
          <LifeBuoy size={32} className="text-[#C9A227]" />
        </div>
        <h1 className="font-serif text-3xl font-medium text-foreground mb-2">Centro de soporte</h1>
        <p className="text-foreground-faint">Cuéntanos qué necesitas y te ayudaremos a la brevedad</p>
      </div>

      {sent ? (
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}
          className="bg-surface rounded-lg border border-line/70 p-10 text-center">
          <CheckCircle size={48} className="mx-auto mb-4 text-accent-green" />
          <h2 className="font-serif text-xl font-medium text-foreground mb-2">¡Ticket enviado!</h2>
          <p className="text-foreground-faint mb-6">Nuestro equipo responderá tu solicitud lo antes posible.</p>
          <Button variant="outline" onClick={() => { setSent(false); setTicket({ subject: '', message: '', category: categories[0] }) }}>
            Enviar otro ticket
          </Button>
        </motion.div>
      ) : (
        <form onSubmit={submit} className="bg-surface rounded-lg border border-line/70 p-6 space-y-4">
          <div>
            <label className="block text-xs font-medium text-foreground-faint mb-1">Categoría</label>
            <select value={ticket.category} onChange={e => setTicket({ ...ticket, category: e.target.value })} className={input}>
              {categories.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground-faint mb-1">Asunto *</label>
            <input value={ticket.subject} onChange={e => setTicket({ ...ticket, subject: e.target.value })} required className={input} placeholder="Breve resumen de tu consulta" />
          </div>
          <div>
            <label className="block text-xs font-medium text-foreground-faint mb-1">Mensaje *</label>
            <textarea value={ticket.message} onChange={e => setTicket({ ...ticket, message: e.target.value })} required rows={6} className={`${input} resize-none`} placeholder="Describe tu situación con el mayor detalle posible..." />
          </div>
          <Button type="submit" size="lg" className="w-full" loading={loading}>
            Enviar ticket <Send size={18} />
          </Button>
          <p className="text-xs text-foreground-faint text-center">
            También puedes escribirnos a <span className="text-[#C9A227]">soporte@aurajoyeria.com</span>
          </p>
        </form>
      )}

      {isAuthenticated && (
        <div className="mt-12">
          <h2 className="font-serif text-xl font-medium text-foreground mb-4">Mis tickets</h2>
          {tickets.length === 0 ? (
            <p className="text-sm text-foreground-faint">Aún no has creado tickets.</p>
          ) : (
            <div className="space-y-3">
              {tickets.map(t => (
                <div key={t.id} className="bg-surface rounded-lg border border-line/70 overflow-hidden">
                  <button onClick={() => (openTicket?.id === t.id ? setOpenTicket(null) : openThread(t))}
                    className="w-full flex items-center justify-between gap-4 p-4 text-left">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">{t.subject}</p>
                      <p className="text-xs text-foreground-faint">#{t.ticketNumber} · {formatDate(t.createdAt)}</p>
                    </div>
                    <span className={`shrink-0 px-2.5 py-1 rounded-full text-xs font-medium ${getStatusColor(t.status)}`}>
                      {getStatusLabel(t.status)}
                    </span>
                  </button>

                  {openTicket?.id === t.id && (
                    <div className="border-t border-line/70 p-4 space-y-3">
                      <div className="bg-background-warm rounded-lg p-3">
                        <p className="text-xs text-foreground-faint mb-1">Tú · {formatDate(openTicket.createdAt)}</p>
                        <p className="text-sm text-foreground whitespace-pre-wrap">{openTicket.message}</p>
                      </div>
                      {(openTicket.responses || []).map(r => (
                        <div key={r.id} className={`rounded-lg p-3 ${r.staff ? 'bg-[#C9A227]/10 ml-6' : 'bg-background-warm mr-6'}`}>
                          <p className="text-xs text-foreground-faint mb-1">
                            {r.staff ? `Soporte · ${r.authorName}` : 'Tú'} · {formatDate(r.createdAt)}
                          </p>
                          <p className="text-sm text-foreground whitespace-pre-wrap">{r.message}</p>
                        </div>
                      ))}
                      {openTicket.status !== 'CLOSED' && (
                        <form onSubmit={sendReply} className="flex items-end gap-2 pt-1">
                          <textarea value={reply} onChange={e => setReply(e.target.value)} rows={2} required
                            placeholder="Escribe una respuesta..."
                            className={`${input} resize-none`} />
                          <Button type="submit" loading={sending} disabled={!reply.trim()}>
                            <SendHorizonal size={16} />
                          </Button>
                        </form>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {!isAuthenticated && (
        <p className="mt-10 text-sm text-foreground-faint text-center flex items-center justify-center gap-2">
          <MessageSquare size={16} /> Inicia sesión para hacer seguimiento de tus tickets y ver las respuestas.
        </p>
      )}
    </div>
  )
}
