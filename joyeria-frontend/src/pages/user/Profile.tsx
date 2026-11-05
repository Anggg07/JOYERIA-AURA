import { useEffect, useState } from 'react'
import { User, Mail, Phone, Lock, Bell } from 'lucide-react'
import { subscribeToPush, unsubscribeFromPush } from '../../utils/push'
import { userService } from '../../services/user.service'
import { useToast } from '../../contexts/ToastContext'
import Button from '../../components/Button'
import UserNav from '../../components/UserNav'
import Skeleton from '../../components/Skeleton'
import type { User as UserType } from '../../types'
import { getErrorMessage } from '../../utils/errors'
import useDocumentTitle from '../../hooks/useDocumentTitle'

export default function Profile() {
  const [profile, setProfile] = useState<UserType | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [changingPassword, setChangingPassword] = useState(false)
  const [pushEnabled, setPushEnabled] = useState(false)
  const [pushLoading, setPushLoading] = useState(false)
  const { toast } = useToast()

  useDocumentTitle('Mi cuenta')

  useEffect(() => {
    userService.getProfile().then(u => { setProfile(u); setLoading(false) }).catch(() => setLoading(false))
  }, [])

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!profile) return
    setSaving(true)
    try {
      await userService.updateProfile({ firstName: profile.firstName, lastName: profile.lastName, phone: profile.phone })
      toast('Perfil actualizado')
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al actualizar el perfil')
      toast(message || 'Error al actualizar el perfil', 'error')
    } finally { setSaving(false) }
  }

  const handlePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (passwords.newPassword !== passwords.confirmPassword) { toast('Las contraseñas no coinciden', 'error'); return }
    setChangingPassword(true)
    try {
      await userService.changePassword(passwords)
      toast('Contraseña cambiada exitosamente')
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al cambiar la contraseña')
      toast(message || 'Error al cambiar la contraseña', 'error')
    } finally { setChangingPassword(false) }
  }

  const handleTogglePush = async () => {
    setPushLoading(true)
    try {
      if (pushEnabled) {
        await unsubscribeFromPush()
        setPushEnabled(false)
        toast('Notificaciones desactivadas')
      } else {
        const ok = await subscribeToPush()
        if (ok) { setPushEnabled(true); toast('Notificaciones activadas') }
        else toast('No se pudieron activar las notificaciones', 'error')
      }
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error con las notificaciones')
      toast(message || 'Error con las notificaciones', 'error')
    } finally { setPushLoading(false) }
  }

  if (loading) return <div className="max-w-4xl mx-auto px-4 py-10"><Skeleton className="h-64" /></div>
  if (!profile) return <div className="max-w-4xl mx-auto px-4 py-10"><Skeleton className="h-64" /></div>

  const input = 'w-full px-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition'

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="font-serif text-2xl font-medium text-foreground mb-6">Mi cuenta</h1>
      <UserNav />
      <div className="grid md:grid-cols-2 gap-6">
        <form onSubmit={handleSave} className="bg-surface rounded-lg border border-line/70 p-6">
          <h2 className="font-medium text-foreground mb-5 flex items-center gap-2"><User size={18} className="text-[#C9A227]" /> Información personal</h2>
          <div className="grid grid-cols-1 gap-4 mb-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium text-foreground-faint mb-1">Nombre</label>
              <input value={profile.firstName} onChange={e => setProfile({ ...profile, firstName: e.target.value })} required className={input} />
            </div>
            <div>
              <label className="block text-xs font-medium text-foreground-faint mb-1">Apellido</label>
              <input value={profile.lastName} onChange={e => setProfile({ ...profile, lastName: e.target.value })} required className={input} />
            </div>
          </div>
          <div className="mb-4">
            <label className="block text-xs font-medium text-foreground-faint mb-1">Email</label>
            <div className="relative">
              <Mail size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
              <input value={profile.email} disabled className={`${input} pl-9 bg-surface-muted text-foreground-faint`} />
            </div>
          </div>
          <div className="mb-5">
            <label className="block text-xs font-medium text-foreground-faint mb-1">Teléfono</label>
            <div className="relative">
              <Phone size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
              <input value={profile.phone || ''} onChange={e => setProfile({ ...profile, phone: e.target.value })} className={`${input} pl-9`} />
            </div>
          </div>
          <Button type="submit" loading={saving}>Guardar cambios</Button>
        </form>

        <form onSubmit={handlePassword} className="bg-surface rounded-lg border border-line/70 p-6">
          <h2 className="font-medium text-foreground mb-5 flex items-center gap-2"><Lock size={18} className="text-[#C9A227]" /> Cambiar contraseña</h2>
          <div className="space-y-4 mb-5">
            <div>
              <label className="block text-xs font-medium text-foreground-faint mb-1">Contraseña actual</label>
              <input type="password" value={passwords.currentPassword} onChange={e => setPasswords({ ...passwords, currentPassword: e.target.value })} required className={input} />
            </div>
            <div>
              <label className="block text-xs font-medium text-foreground-faint mb-1">Nueva contraseña</label>
              <input type="password" value={passwords.newPassword} onChange={e => setPasswords({ ...passwords, newPassword: e.target.value })} required minLength={8} pattern="(?=.*[A-Za-z])(?=.*\d).+" title="Debe contener al menos una letra y un número" className={input} />
            </div>
            <div>
              <label className="block text-xs font-medium text-foreground-faint mb-1">Confirmar nueva contraseña</label>
              <input type="password" value={passwords.confirmPassword} onChange={e => setPasswords({ ...passwords, confirmPassword: e.target.value })} required minLength={8} className={input} />
            </div>
          </div>
          <Button type="submit" variant="secondary" loading={changingPassword}>Cambiar contraseña</Button>
        </form>

        <div className="bg-surface rounded-lg border border-line/70 p-6 md:col-span-2">
          <h2 className="font-medium text-foreground mb-3 flex items-center gap-2"><Bell size={18} className="text-[#C9A227]" /> Notificaciones push</h2>
          <p className="text-sm text-foreground-muted mb-4">Recibe avisos cuando tu pedido cambie de estado o tu pago sea aprobado.</p>
          <Button variant={pushEnabled ? 'secondary' : 'primary'} loading={pushLoading} onClick={handleTogglePush}>
            {pushEnabled ? 'Desactivar notificaciones' : 'Activar notificaciones'}
          </Button>
        </div>
      </div>
    </div>
  )
}