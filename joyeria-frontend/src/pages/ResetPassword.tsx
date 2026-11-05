import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Lock, Eye, EyeOff, CheckCircle2 } from 'lucide-react'
import { useToast } from '../contexts/ToastContext'
import { authService } from '../services/auth.service'
import Button from '../components/Button'
import { getErrorMessage } from '../utils/errors'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)
  const { toast } = useToast()
  const navigate = useNavigate()

  useDocumentTitle('Restablecer contraseña')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!token) { toast('El enlace es inválido o está incompleto', 'error'); return }
    if (newPassword !== confirmPassword) { toast('Las contraseñas no coinciden', 'error'); return }
    setLoading(true)
    try {
      const message = await authService.resetPassword(token, newPassword)
      setDone(true)
      toast(message)
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'El enlace es inválido o expiró')
      toast(message || 'El enlace es inválido o expiró', 'error')
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4 py-16 bg-gradient-to-b from-background to-background-warm">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md bg-surface rounded-xl border border-line/70 shadow-sm p-8">
        <div className="text-center mb-8">
          <div className="w-12 h-12 mx-auto rounded-full border-2 border-[#C9A227] flex items-center justify-center mb-3">
            <span className="font-serif text-xl text-[#C9A227] font-bold">J</span>
          </div>
          <h1 className="font-serif text-2xl font-medium text-foreground">Nueva contraseña</h1>
          <p className="text-sm text-foreground-faint mt-2">
            {done ? 'Tu contraseña ha sido actualizada.' : 'Define una nueva contraseña para tu cuenta.'}
          </p>
        </div>

        {done ? (
          <div className="text-center">
            <CheckCircle2 size={40} className="mx-auto text-accent-green mb-3" />
            <Button onClick={() => navigate('/login')} className="w-full" size="lg">Iniciar sesión</Button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm text-foreground-muted mb-1.5">Nueva contraseña</label>
              <div className="relative">
                <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
                <input type={showPassword ? 'text' : 'password'} value={newPassword} onChange={e => setNewPassword(e.target.value)} required minLength={8} pattern="(?=.*[A-Za-z])(?=.*\d).+" title="Debe contener al menos una letra y un número"
                  className="w-full pl-9 pr-10 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition" placeholder="Mín. 8 caracteres, letra y número" />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-foreground-faint hover:text-foreground-muted">
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>
            <div>
              <label className="block text-sm text-foreground-muted mb-1.5">Confirmar contraseña</label>
              <div className="relative">
                <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
                <input type={showPassword ? 'text' : 'password'} value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} required minLength={8}
                  className="w-full pl-9 pr-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition" placeholder="Repite la contraseña" />
              </div>
            </div>
            <Button type="submit" loading={loading} className="w-full" size="lg">Actualizar contraseña</Button>
          </form>
        )}

        <div className="mt-6 pt-6 border-t border-line/70 text-center">
          <Link to="/login" className="inline-flex items-center gap-1.5 text-sm text-[#C9A227] font-medium hover:underline">
            Volver a iniciar sesión
          </Link>
        </div>
      </motion.div>
    </div>
  )
}