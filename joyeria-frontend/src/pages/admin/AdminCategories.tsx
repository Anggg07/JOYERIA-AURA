import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, FolderTree } from 'lucide-react'
import { adminService } from '../../services/admin.service'
import { categoryService } from '../../services/category.service'
import { useToast } from '../../contexts/ToastContext'
import type { Category } from '../../types'
import Button from '../../components/Button'
import Modal from '../../components/Modal'
import Skeleton from '../../components/Skeleton'
import { getErrorMessage } from '../../utils/errors'

const empty: { name: string; description: string; displayOrder: number; image: string; active: boolean } = { name: '', description: '', displayOrder: 0, image: '', active: true }

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState<Category | null>(null)
  const [form, setForm] = useState<typeof empty>(empty)
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  const load = async () => {
    setLoading(true)
    try { setCategories(await categoryService.getAll()) } catch { setCategories([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openNew = () => { setEditing(null); setForm(empty); setModal(true) }
  const openEdit = (c: Category) => {
    setEditing(c)
    setForm({ name: c.name, description: c.description || '', displayOrder: c.displayOrder || 0, image: c.image || '', active: c.active })
    setModal(true)
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editing) await adminService.updateCategory(editing.id, form)
      else await adminService.createCategory(form)
      toast(editing ? 'Categoría actualizada' : 'Categoría creada')
      setModal(false); load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al guardar')
      toast(message || 'Error al guardar', 'error')
    } finally { setSaving(false) }
  }

  const remove = async (id: number) => {
    if (!window.confirm('¿Eliminar esta categoría?')) return
    try { await adminService.deleteCategory(id); toast('Categoría eliminada', 'info'); load() }
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
          <h1 className="font-serif text-2xl font-medium text-foreground">Categorías</h1>
          <p className="text-sm text-foreground-faint">{categories.length} categorías</p>
        </div>
        <Button onClick={openNew}><Plus size={16} /> Nueva categoría</Button>
      </div>

      {loading ? (
        <Skeleton className="h-72" />
      ) : categories.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center text-foreground-faint">
          <FolderTree size={40} className="mx-auto mb-3 text-foreground/20" />
          Aún no hay categorías.
        </div>
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Nombre</th>
                  <th className="p-4">Slug</th>
                  <th className="p-4">Orden</th>
                  <th className="p-4">Productos</th>
                  <th className="p-4">Estado</th>
                  <th className="p-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {categories.map(c => (
                  <tr key={c.id} className="border-b border-line/40 last:border-0 hover:bg-surface-muted/50">
                    <td className="p-4 font-medium text-foreground">{c.name}</td>
                    <td className="p-4 text-foreground-faint">{c.slug}</td>
                    <td className="p-4 text-foreground-faint">{c.displayOrder ?? '-'}</td>
                    <td className="p-4 text-foreground-faint">{c.productCount ?? '-'}</td>
                    <td className="p-4">
                      <span className={`text-xs px-2.5 py-1 rounded-full ${c.active ? 'bg-accent-green/15 text-accent-dark' : 'bg-surface-muted text-foreground-faint'}`}>
                        {c.active ? 'Activa' : 'Inactiva'}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex gap-1 justify-end">
                        <button onClick={() => openEdit(c)} className="p-2 text-foreground-faint hover:text-[#C9A227]"><Pencil size={15} /></button>
                        <button onClick={() => remove(c.id)} className="p-2 text-foreground-faint hover:text-red-500"><Trash2 size={15} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal isOpen={modal} onClose={() => setModal(false)} title={editing ? 'Editar categoría' : 'Nueva categoría'}>
        <form onSubmit={save} className="space-y-4">
          <div>
            <label className={label}>Nombre *</label>
            <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required className={input} />
          </div>
          <div>
            <label className={label}>Descripción</label>
            <textarea value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} className={`${input} resize-none`} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={label}>Orden de visualización</label>
              <input type="number" value={form.displayOrder ?? 0} onChange={e => setForm({ ...form, displayOrder: Number(e.target.value) })} className={input} />
            </div>
            <div>
              <label className={label}>Imagen (URL)</label>
              <input value={form.image || ''} onChange={e => setForm({ ...form, image: e.target.value })} className={input} />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-foreground-muted">
            <input type="checkbox" checked={!!form.active} onChange={e => setForm({ ...form, active: e.target.checked })} className="accent-[#C9A227]" />
            Categoría activa
          </label>
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={() => setModal(false)}>Cancelar</Button>
            <Button type="submit" loading={saving} className="flex-1">{editing ? 'Guardar cambios' : 'Crear categoría'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}