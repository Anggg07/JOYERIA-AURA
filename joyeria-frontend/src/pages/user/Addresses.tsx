import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, MapPin, Home } from 'lucide-react'
import { userService } from '../../services/user.service'
import { useToast } from '../../contexts/ToastContext'
import type { Address } from '../../types'
import Button from '../../components/Button'
import Modal from '../../components/Modal'
import UserNav from '../../components/UserNav'
import Skeleton from '../../components/Skeleton'
import { getErrorMessage } from '../../utils/errors'
import useDocumentTitle from '../../hooks/useDocumentTitle'

interface AddressForm {
  firstName: string
  lastName: string
  phone: string
  addressLine1: string
  addressLine2: string
  city: string
  department: string
  postalCode: string
  addressType: 'SHIPPING' | 'BILLING'
  isDefault: boolean
}

const empty: AddressForm = {
  firstName: '', lastName: '', phone: '', addressLine1: '', addressLine2: '',
  city: '', department: '', postalCode: '', addressType: 'SHIPPING', isDefault: false,
}

export default function Addresses() {
  const [addresses, setAddresses] = useState<Address[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState<Address | null>(null)
  const [form, setForm] = useState<typeof empty>(empty)
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  useDocumentTitle('Mis direcciones')

  const load = async () => {
    setLoading(true)
    try { setAddresses(await userService.getAddresses()) } catch { setAddresses([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const openNew = () => { setEditing(null); setForm(empty); setModal(true) }

  const openEdit = (addr: Address) => {
    setEditing(addr)
    setForm({
      firstName: addr.firstName || '',
      lastName: addr.lastName || '',
      phone: addr.phone || '',
      addressLine1: addr.addressLine1,
      addressLine2: addr.addressLine2 || '',
      city: addr.city,
      department: addr.department || '',
      postalCode: addr.postalCode || '',
      addressType: addr.addressType,
      isDefault: addr.isDefault,
    })
    setModal(true)
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editing) await userService.updateAddress(editing.id, form)
      else await userService.createAddress(form)
      toast(editing ? 'Dirección actualizada' : 'Dirección guardada')
      setModal(false)
      load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al guardar la dirección')
      toast(message || 'Error al guardar la dirección', 'error')
    } finally { setSaving(false) }
  }

  const remove = async (id: number) => {
    try {
      await userService.deleteAddress(id)
      toast('Dirección eliminada', 'info')
      load()
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'No se pudo eliminar')
      toast(message || 'No se pudo eliminar', 'error')
    }
  }

  if (loading) return <div className="max-w-4xl mx-auto px-4 py-10"><Skeleton className="h-64" /></div>

  const input = 'w-full px-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition'

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <h1 className="font-serif text-2xl font-medium text-foreground">Mis direcciones</h1>
        <Button onClick={openNew}><Plus size={16} /> Nueva dirección</Button>
      </div>
      <UserNav />

      {addresses.length === 0 ? (
        <div className="bg-surface rounded-lg border border-line/70 p-12 text-center">
          <MapPin size={40} className="mx-auto mb-3 text-foreground/20" />
          <p className="text-foreground-faint mb-4">No tienes direcciones guardadas</p>
          <Button onClick={openNew}>Agregar dirección</Button>
        </div>
      ) : (
        <div className="grid md:grid-cols-2 gap-4">
          {addresses.map(addr => (
            <div key={addr.id} className="bg-surface rounded-lg border border-line/70 p-5 relative">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <Home size={16} className="text-[#C9A227]" />
                  <span className="text-sm font-medium text-foreground capitalize">{addr.addressType === 'SHIPPING' ? 'Envío' : 'Facturación'}</span>
                  {addr.isDefault && <span className="text-[10px] bg-[#C9A227]/10 text-[#C9A227] px-2 py-0.5 rounded-full">Predeterminada</span>}
                </div>
                <div className="flex gap-1">
                  <button onClick={() => openEdit(addr)} className="p-2 text-foreground-faint hover:text-[#C9A227]"><Pencil size={15} /></button>
                  <button onClick={() => remove(addr.id)} className="p-2 text-foreground-faint hover:text-red-500"><Trash2 size={15} /></button>
                </div>
              </div>
              <p className="text-sm text-foreground-muted">{addr.firstName} {addr.lastName}</p>
              <p className="text-sm text-foreground-faint">{addr.addressLine1}</p>
              {addr.addressLine2 && <p className="text-sm text-foreground-faint">{addr.addressLine2}</p>}
              <p className="text-sm text-foreground-faint">{addr.city}{addr.department ? `, ${addr.department}` : ''}{addr.postalCode ? ` - ${addr.postalCode}` : ''}</p>
              {addr.phone && <p className="text-sm text-foreground-faint">{addr.phone}</p>}
            </div>
          ))}
        </div>
      )}

      <Modal isOpen={modal} onClose={() => setModal(false)} title={editing ? 'Editar dirección' : 'Nueva dirección'}>
        <form onSubmit={save} className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div><label className="block text-xs font-medium text-foreground-faint mb-1">Nombre</label>
              <input value={form.firstName || ''} onChange={e => setForm({ ...form, firstName: e.target.value })} className={input} /></div>
            <div><label className="block text-xs font-medium text-foreground-faint mb-1">Apellido</label>
              <input value={form.lastName || ''} onChange={e => setForm({ ...form, lastName: e.target.value })} className={input} /></div>
          </div>
          <div><label className="block text-xs font-medium text-foreground-faint mb-1">Teléfono</label>
            <input value={form.phone || ''} onChange={e => setForm({ ...form, phone: e.target.value })} className={input} placeholder="+57 300 000 0000" /></div>
          <div><label className="block text-xs font-medium text-foreground-faint mb-1">Dirección *</label>
            <input value={form.addressLine1} onChange={e => setForm({ ...form, addressLine1: e.target.value })} required className={input} placeholder="Calle 123 # 45-67" /></div>
          <div><label className="block text-xs font-medium text-foreground-faint mb-1">Complemento</label>
            <input value={form.addressLine2 || ''} onChange={e => setForm({ ...form, addressLine2: e.target.value })} className={input} placeholder="Apto 301, Torre B" /></div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div><label className="block text-xs font-medium text-foreground-faint mb-1">Ciudad *</label>
              <input value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} required className={input} /></div>
            <div><label className="block text-xs font-medium text-foreground-faint mb-1">Departamento</label>
              <input value={form.department || ''} onChange={e => setForm({ ...form, department: e.target.value })} className={input} /></div>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div><label className="block text-xs font-medium text-foreground-faint mb-1">Código postal</label>
              <input value={form.postalCode || ''} onChange={e => setForm({ ...form, postalCode: e.target.value })} className={input} /></div>
            <div>
              <label className="block text-xs font-medium text-foreground-faint mb-1">Tipo</label>
              <select value={form.addressType} onChange={e => setForm({ ...form, addressType: e.target.value as 'SHIPPING' | 'BILLING' })} className={input}>
                <option value="SHIPPING">Envío</option>
                <option value="BILLING">Facturación</option>
              </select>
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-foreground-muted">
            <input type="checkbox" checked={!!form.isDefault} onChange={e => setForm({ ...form, isDefault: e.target.checked })} className="accent-[#C9A227]" />
            Usar como dirección predeterminada
          </label>
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={() => setModal(false)}>Cancelar</Button>
            <Button type="submit" loading={saving} className="flex-1">Guardar dirección</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}