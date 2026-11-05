import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Ticket } from 'lucide-react'
import { couponService } from '../../services/coupon.service'
import { useToast } from '../../contexts/ToastContext'
import type { Coupon } from '../../types'
import Button from '../../components/Button'
import Modal from '../../components/Modal'
import Skeleton from '../../components/Skeleton'
import { formatDate, formatPrice } from '../../utils/format'
import { getErrorMessage } from '../../utils/errors'

const empty: {
  code: string; description: string; discountType: 'PERCENTAGE' | 'FIXED'; discountValue: string
  minAmount: string; maxUses: string; validFrom: string; validUntil: string; active: boolean
} = {
  code: '', description: '', discountType: 'PERCENTAGE', discountValue: '',
  minAmount: '', maxUses: '', validFrom: '', validUntil: '', active: true,
}

export default function AdminCoupons() {
  const [coupons, setCoupons] = useState<Coupon[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState<Coupon | null>(null)
  const [form, setForm] = useState<typeof empty>(empty)
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  const load = async () => {
    setLoading(true)
    try { setCoupons(await couponService.getAll()) } catch { setCoupons([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openNew = () => { setEditing(null); setForm(empty); setModal(true) }

  const openEdit = (c: Coupon) => {
    setEditing(c)
    setForm({
      code: c.code, description: c.description || '', discountType: c.discountType as 'PERCENTAGE' | 'FIXED',
      discountValue: String(c.discountValue), minAmount: c.minAmount ? String(c.minAmount) : '',
      maxUses: c.maxUses ? String(c.maxUses) : '',
      validFrom: c.validFrom ? c.validFrom.slice(0, 16) : '',
      validUntil: c.validUntil ? c.validUntil.slice(0, 16) : '', active: c.active,
    })
    setModal(true)
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    const payload = {
      ...form,
      discountValue: Number(form.discountValue),
      minAmount: form.minAmount ? Number(form.minAmount) : null,
      maxUses: form.maxUses ? Number(form.maxUses) : null,
      validFrom: form.validFrom ? new Date(form.validFrom).toISOString() : null,
      validUntil: form.validUntil ? new Date(form.validUntil).toISOString() : null,
    }
    try {
      if (editing) await couponService.update(editing.id, payload)
      else await couponService.create(payload)
      toast(editing ? 'Cupón actualizado' : 'Cupón creado')
      setModal(false); load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al guardar')
      toast(message || 'Error al guardar', 'error')
    } finally { setSaving(false) }
  }

  const remove = async (id: number) => {
    if (!window.confirm('¿Eliminar este cupón?')) return
    try { await couponService.delete(id); toast('Cupón eliminado', 'info'); load() }
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
          <h1 className="font-serif text-2xl font-medium text-foreground">Cupones</h1>
          <p className="text-sm text-foreground-faint">{coupons.length} cupones</p>
        </div>
        <Button onClick={openNew}><Plus size={16} /> Nuevo cupón</Button>
      </div>

      {loading ? (
        <Skeleton className="h-72" />
      ) : coupons.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center text-foreground-faint">
          <Ticket size={40} className="mx-auto mb-3 text-foreground/20" />
          No hay cupones creados.
        </div>
      ) : (
        <div className="bg-surface rounded-lg border border-line/70 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-foreground-faint border-b border-line/70 bg-background-warm">
                  <th className="p-4">Código</th>
                  <th className="p-4">Descuento</th>
                  <th className="p-4">Mínimo</th>
                  <th className="p-4">Usos</th>
                  <th className="p-4">Válido hasta</th>
                  <th className="p-4">Estado</th>
                  <th className="p-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {coupons.map(c => (
                  <tr key={c.id} className="border-b border-line/40 last:border-0 hover:bg-surface-muted/50">
                    <td className="p-4">
                      <span className="font-mono font-semibold text-[#C9A227]">{c.code}</span>
                      {c.description && <p className="text-xs text-foreground-faint mt-0.5">{c.description}</p>}
                    </td>
                    <td className="p-4 font-medium">
                      {c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : formatPrice(c.discountValue)}
                    </td>
                    <td className="p-4 text-foreground-faint">{c.minAmount ? formatPrice(c.minAmount) : '-'}</td>
                    <td className="p-4 text-foreground-faint">{c.usesCount}/{c.maxUses ?? '∞'}</td>
                    <td className="p-4 text-foreground-faint text-xs">{c.validUntil ? formatDate(c.validUntil) : '-'}</td>
                    <td className="p-4">
                      <span className={`text-xs px-2.5 py-1 rounded-full ${c.active ? 'bg-accent-green/15 text-accent-dark' : 'bg-surface-muted text-foreground-faint'}`}>
                        {c.active ? 'Activo' : 'Inactivo'}
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

      <Modal isOpen={modal} onClose={() => setModal(false)} title={editing ? 'Editar cupón' : 'Nuevo cupón'}>
        <form onSubmit={save} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={label}>Código *</label>
              <input value={form.code} onChange={e => setForm({ ...form, code: e.target.value })}
                required className={`${input} font-mono uppercase`} placeholder="BIENVENIDA10" />
            </div>
            <div>
              <label className={label}>Tipo de descuento</label>
              <select value={form.discountType} onChange={e => setForm({ ...form, discountType: e.target.value as 'PERCENTAGE' | 'FIXED' })} className={input}>
                <option value="PERCENTAGE">Porcentaje (%)</option>
                <option value="FIXED">Monto fijo ($)</option>
              </select>
            </div>
            <div className="col-span-2">
              <label className={label}>Descripción</label>
              <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className={input} />
            </div>
            <div>
              <label className={label}>Valor del descuento *</label>
              <input type="number" value={form.discountValue} onChange={e => setForm({ ...form, discountValue: e.target.value })} required min={1} className={input} />
            </div>
            <div>
              <label className={label}>Monto mínimo de compra</label>
              <input type="number" value={form.minAmount} onChange={e => setForm({ ...form, minAmount: e.target.value })} min={0} className={input} />
            </div>
            <div>
              <label className={label}>Usos máximos</label>
              <input type="number" value={form.maxUses} onChange={e => setForm({ ...form, maxUses: e.target.value })} min={1} className={input} />
            </div>
            <div>
              <label className={label}>Válido desde</label>
              <input type="datetime-local" value={form.validFrom} onChange={e => setForm({ ...form, validFrom: e.target.value })} className={input} />
            </div>
            <div>
              <label className={label}>Válido hasta</label>
              <input type="datetime-local" value={form.validUntil} onChange={e => setForm({ ...form, validUntil: e.target.value })} className={input} />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-foreground-muted">
            <input type="checkbox" checked={!!form.active} onChange={e => setForm({ ...form, active: e.target.checked })} className="accent-[#C9A227]" />
            Cupón activo
          </label>
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={() => setModal(false)}>Cancelar</Button>
            <Button type="submit" loading={saving} className="flex-1">{editing ? 'Guardar cambios' : 'Crear cupón'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}