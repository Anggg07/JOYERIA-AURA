import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Gem } from 'lucide-react'
import { adminService } from '../../services/admin.service'
import { materialService } from '../../services/material.service'
import { useToast } from '../../contexts/ToastContext'
import type { Material } from '../../types'
import Button from '../../components/Button'
import Modal from '../../components/Modal'
import Skeleton from '../../components/Skeleton'
import { getErrorMessage } from '../../utils/errors'

const empty: { name: string; description: string; image: string; active: boolean } = { name: '', description: '', image: '', active: true }

export default function AdminMaterials() {
  const [materials, setMaterials] = useState<Material[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState<Material | null>(null)
  const [form, setForm] = useState<typeof empty>(empty)
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  const load = async () => {
    setLoading(true)
    try { setMaterials(await materialService.getAll()) } catch { setMaterials([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openNew = () => { setEditing(null); setForm(empty); setModal(true) }
  const openEdit = (m: Material) => {
    setEditing(m)
    setForm({ name: m.name, description: m.description || '', image: m.image || '', active: m.active })
    setModal(true)
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editing) await adminService.updateMaterial(editing.id, form)
      else await adminService.createMaterial(form)
      toast(editing ? 'Material actualizado' : 'Material creado')
      setModal(false); load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al guardar')
      toast(message || 'Error al guardar', 'error')
    } finally { setSaving(false) }
  }

  const remove = async (id: number) => {
    if (!window.confirm('¿Eliminar este material?')) return
    try { await adminService.deleteMaterial(id); toast('Material eliminado', 'info'); load() }
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
          <h1 className="font-serif text-2xl font-medium text-foreground">Materiales</h1>
          <p className="text-sm text-foreground-faint">{materials.length} materiales</p>
        </div>
        <Button onClick={openNew}><Plus size={16} /> Nuevo material</Button>
      </div>

      {loading ? (
        <Skeleton className="h-72" />
      ) : materials.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center text-foreground-faint">
          <Gem size={40} className="mx-auto mb-3 text-foreground/20" />
          Aún no hay materiales.
        </div>
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Nombre</th>
                  <th className="p-4">Slug</th>
                  <th className="p-4">Descripción</th>
                  <th className="p-4">Estado</th>
                  <th className="p-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {materials.map(m => (
                  <tr key={m.id} className="border-b border-line/40 last:border-0 hover:bg-surface-muted/50">
                    <td className="p-4 font-medium text-foreground">{m.name}</td>
                    <td className="p-4 text-foreground-faint">{m.slug}</td>
                    <td className="p-4 text-foreground-faint max-w-xs truncate">{m.description || '-'}</td>
                    <td className="p-4">
                      <span className={`text-xs px-2.5 py-1 rounded-full ${m.active ? 'bg-accent-green/15 text-accent-dark' : 'bg-surface-muted text-foreground-faint'}`}>
                        {m.active ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex gap-1 justify-end">
                        <button onClick={() => openEdit(m)} className="p-2 text-foreground-faint hover:text-[#C9A227]"><Pencil size={15} /></button>
                        <button onClick={() => remove(m.id)} className="p-2 text-foreground-faint hover:text-red-500"><Trash2 size={15} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal isOpen={modal} onClose={() => setModal(false)} title={editing ? 'Editar material' : 'Nuevo material'}>
        <form onSubmit={save} className="space-y-4">
          <div>
            <label className={label}>Nombre *</label>
            <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required className={input} />
          </div>
          <div>
            <label className={label}>Descripción</label>
            <textarea value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} className={`${input} resize-none`} />
          </div>
          <div>
            <label className={label}>Imagen (URL)</label>
            <input value={form.image || ''} onChange={e => setForm({ ...form, image: e.target.value })} className={input} />
          </div>
          <label className="flex items-center gap-2 text-sm text-foreground-muted">
            <input type="checkbox" checked={!!form.active} onChange={e => setForm({ ...form, active: e.target.checked })} className="accent-[#C9A227]" />
            Material activo
          </label>
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={() => setModal(false)}>Cancelar</Button>
            <Button type="submit" loading={saving} className="flex-1">{editing ? 'Guardar cambios' : 'Crear material'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}