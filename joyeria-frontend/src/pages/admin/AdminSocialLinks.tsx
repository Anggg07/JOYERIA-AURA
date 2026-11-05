import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Link2 } from 'lucide-react'
import { adminService } from '../../services/admin.service'
import { useToast } from '../../contexts/ToastContext'
import type { SocialLink } from '../../types'
import Button from '../../components/Button'
import Modal from '../../components/Modal'
import Skeleton from '../../components/Skeleton'
import { getErrorMessage } from '../../utils/errors'

const empty: { name: string; url: string; icon: string; active: boolean; sortOrder: number } = { name: '', url: '', icon: 'instagram', active: true, sortOrder: 0 }

const iconOptions = ['instagram', 'facebook', 'twitter', 'youtube', 'linkedin', 'tiktok', 'whatsapp']

export default function AdminSocialLinks() {
  const [links, setLinks] = useState<SocialLink[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState<SocialLink | null>(null)
  const [form, setForm] = useState<typeof empty>(empty)
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  const load = async () => {
    setLoading(true)
    try { setLinks(await adminService.getSocialLinks()) } catch { setLinks([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openNew = () => { setEditing(null); setForm(empty); setModal(true) }
  const openEdit = (l: SocialLink) => {
    setEditing(l)
    setForm({ name: l.name, url: l.url, icon: l.icon || 'instagram', active: l.active, sortOrder: l.sortOrder })
    setModal(true)
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editing) await adminService.updateSocialLink(editing.id, form)
      else await adminService.createSocialLink(form)
      toast(editing ? 'Enlace actualizado' : 'Enlace creado')
      setModal(false); load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al guardar')
      toast(message || 'Error al guardar', 'error')
    } finally { setSaving(false) }
  }

  const remove = async (id: number) => {
    if (!window.confirm('¿Eliminar este enlace?')) return
    try { await adminService.deleteSocialLink(id); toast('Enlace eliminado', 'info'); load() }
    catch (err: unknown) {
      const message = getErrorMessage(err, 'No se pudo eliminar')
      toast(message || 'No se pudo eliminar', 'error')
    }
  }

  const input = 'w-full px-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition'
  const label = 'block text-xs font-medium text-foreground-faint mb-1'

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-serif text-2xl font-medium text-foreground">Redes sociales</h1>
          <p className="text-sm text-foreground-faint">{links.length} enlaces</p>
        </div>
        <Button onClick={openNew}><Plus size={16} /> Nuevo enlace</Button>
      </div>

      {loading ? (
        <Skeleton className="h-56" />
      ) : links.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center text-foreground-faint">
          <Link2 size={40} className="mx-auto mb-3 text-foreground/20" />
          No hay enlaces configurados.
        </div>
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Nombre</th>
                  <th className="p-4">Icono</th>
                  <th className="p-4">URL</th>
                  <th className="p-4">Orden</th>
                  <th className="p-4">Estado</th>
                  <th className="p-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {links.map(l => (
                  <tr key={l.id} className="border-b border-line/40 last:border-0 hover:bg-surface-muted/50">
                    <td className="p-4 font-medium text-foreground">{l.name}</td>
                    <td className="p-4 text-foreground-faint">{l.icon || '-'}</td>
                    <td className="p-4 text-foreground-faint max-w-xs truncate">{l.url}</td>
                    <td className="p-4 text-foreground-faint">{l.sortOrder}</td>
                    <td className="p-4">
                      <span className={`text-xs px-2.5 py-1 rounded-full ${l.active ? 'bg-green-100 text-green-700' : 'bg-surface-muted text-foreground-faint'}`}>
                        {l.active ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex gap-1 justify-end">
                        <button onClick={() => openEdit(l)} className="p-2 text-foreground-faint hover:text-[#C9A227]"><Pencil size={15} /></button>
                        <button onClick={() => remove(l.id)} className="p-2 text-foreground-faint hover:text-red-500"><Trash2 size={15} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal isOpen={modal} onClose={() => setModal(false)} title={editing ? 'Editar enlace' : 'Nuevo enlace'}>
        <form onSubmit={save} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={label}>Nombre *</label>
              <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required className={input} placeholder="Instagram" />
            </div>
            <div>
              <label className={label}>Icono</label>
              <select value={form.icon} onChange={e => setForm({ ...form, icon: e.target.value })} className={input}>
                {iconOptions.map(io => <option key={io} value={io}>{io}</option>)}
              </select>
            </div>
            <div className="col-span-2">
              <label className={label}>URL *</label>
              <input type="url" value={form.url} onChange={e => setForm({ ...form, url: e.target.value })} required className={input} placeholder="https://instagram.com/aurajoyeria" />
            </div>
            <div>
              <label className={label}>Orden</label>
              <input type="number" value={form.sortOrder ?? 0} onChange={e => setForm({ ...form, sortOrder: Number(e.target.value) })} className={input} />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-foreground-muted">
            <input type="checkbox" checked={!!form.active} onChange={e => setForm({ ...form, active: e.target.checked })} className="accent-[#C9A227]" />
            Enlace activo
          </label>
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={() => setModal(false)}>Cancelar</Button>
            <Button type="submit" loading={saving} className="flex-1">{editing ? 'Guardar cambios' : 'Crear enlace'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}