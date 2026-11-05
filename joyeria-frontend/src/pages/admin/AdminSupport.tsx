import { useEffect, useState } from 'react'
import { LifeBuoy, Eye, Send } from 'lucide-react'
import { adminService } from '../../services/admin.service'
import { useToast } from '../../contexts/ToastContext'
import type { SupportTicket } from '../../types'
import Skeleton from '../../components/Skeleton'
import Modal from '../../components/Modal'
import Button from '../../components/Button'
import Pagination from '../../components/Pagination'
import { formatDate, getStatusLabel, getStatusColor } from '../../utils/format'
import { getErrorMessage } from '../../utils/errors'

const statuses = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

export default function AdminSupport() {
  const [tickets, setTickets] = useState<SupportTicket[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState<SupportTicket | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [reply, setReply] = useState('')
  const [sending, setSending] = useState(false)
  const [updating, setUpdating] = useState(false)
  const { toast } = useToast()

  useEffect(() => { load() }, [page])

  const load = async () => {
    setLoading(true)
    try {
      const data = await adminService.getTickets(page, 20)
      setTickets(data.content); setTotalPages(data.totalPages)
    } catch { setTickets([]); setTotalPages(0) }
    finally { setLoading(false) }
  }

  const openDetail = async (id: number) => {
    setDetailLoading(true)
    setSelected(null)
    setReply('')
    try {
      setSelected(await adminService.getTicket(id))
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'No se pudo cargar el ticket'), 'error')
    } finally { setDetailLoading(false) }
  }

  const changeStatus = async (id: number, status: string) => {
    setUpdating(true)
    try {
      const updated = await adminService.updateTicketStatus(id, status)
      toast('Estado actualizado')
      setSelected(updated)
      load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al actualizar')
      toast(message || 'Error al actualizar', 'error')
    } finally { setUpdating(false) }
  }

  const sendReply = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selected || !reply.trim()) return
    setSending(true)
    try {
      const updated = await adminService.replyToTicket(selected.id, reply.trim())
      setSelected(updated)
      setReply('')
      toast('Respuesta enviada')
      load()
    } catch (err: unknown) {
      toast(getErrorMessage(err, 'No se pudo enviar la respuesta'), 'error')
    } finally { setSending(false) }
  }

  return (
    <div>
      <h1 className="font-serif text-2xl font-medium text-foreground mb-6">Tickets de soporte</h1>

      {loading ? (
        <Skeleton className="h-80" />
      ) : tickets.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center text-foreground-faint">
          <LifeBuoy size={40} className="mx-auto mb-3 text-foreground/20" />
          No hay tickets de soporte.
        </div>
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Ticket</th>
                  <th className="p-4">Cliente</th>
                  <th className="p-4">Categoría</th>
                  <th className="p-4">Asunto</th>
                  <th className="p-4">Fecha</th>
                  <th className="p-4">Estado</th>
                  <th className="p-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map(t => (
                  <tr key={t.id} className="border-b border-line/40 last:border-0 align-top hover:bg-surface-muted/50">
                    <td className="p-4 font-medium text-foreground">#{t.ticketNumber}</td>
                    <td className="p-4">
                      <p className="text-foreground-muted">{t.customerName || '-'}</p>
                      {t.customerEmail && <p className="text-xs text-foreground-faint">{t.customerEmail}</p>}
                    </td>
                    <td className="p-4 text-foreground-faint">{t.category}</td>
                    <td className="p-4">
                      <p className="font-medium text-foreground mb-1">{t.subject}</p>
                      <p className="text-foreground-faint text-xs line-clamp-2">{t.message}</p>
                    </td>
                    <td className="p-4 text-foreground-faint whitespace-nowrap text-xs">{formatDate(t.createdAt)}</td>
                    <td className="p-4">
                      <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-medium ${getStatusColor(t.status)}`}>
                        {getStatusLabel(t.status)}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex justify-end">
                        <button onClick={() => openDetail(t.id)} className="p-2 text-foreground-faint hover:text-[#C9A227]" title="Ver conversación">
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

      <Modal isOpen={!!selected || detailLoading} onClose={() => { setSelected(null); setDetailLoading(false) }} title="Conversación" maxWidth="max-w-2xl">
        {detailLoading || !selected ? (
          <Skeleton className="h-64" />
        ) : (
          <div className="space-y-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="font-serif text-lg text-foreground">{selected.subject}</p>
                <p className="text-xs text-foreground-faint">#{selected.ticketNumber} · {formatDate(selected.createdAt)}</p>
                <p className="text-xs text-foreground-faint mt-1">{selected.customerName} {selected.customerEmail ? `· ${selected.customerEmail}` : ''}</p>
              </div>
              <select value={selected.status} disabled={updating} onChange={e => changeStatus(selected.id, e.target.value)}
                className={`px-2 py-1 rounded-lg border border-line text-xs outline-none bg-surface ${getStatusColor(selected.status)}`}>
                {statuses.map(s => <option key={s} value={s}>{getStatusLabel(s)}</option>)}
              </select>
            </div>

            <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
              <div className="bg-background-warm rounded-lg p-4">
                <p className="text-xs font-medium text-foreground-faint mb-1">{selected.customerName || 'Cliente'}</p>
                <p className="text-sm text-foreground whitespace-pre-wrap">{selected.message}</p>
              </div>
              {(selected.responses || []).map(r => (
                <div key={r.id} className={`rounded-lg p-4 ${r.staff ? 'bg-[#C9A227]/10 ml-6' : 'bg-background-warm mr-6'}`}>
                  <p className="text-xs font-medium text-foreground-faint mb-1">
                    {r.staff ? `Soporte · ${r.authorName}` : r.authorName} · {formatDate(r.createdAt)}
                  </p>
                  <p className="text-sm text-foreground whitespace-pre-wrap">{r.message}</p>
                </div>
              ))}
            </div>

            <form onSubmit={sendReply} className="border-t border-line/70 pt-4 space-y-3">
              <textarea value={reply} onChange={e => setReply(e.target.value)} rows={3} required
                placeholder="Escribe una respuesta al cliente..."
                className="w-full px-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition resize-none" />
              <div className="flex justify-end">
                <Button type="submit" loading={sending} disabled={!reply.trim()}>
                  Enviar respuesta <Send size={16} />
                </Button>
              </div>
            </form>
          </div>
        )}
      </Modal>
    </div>
  )
}
