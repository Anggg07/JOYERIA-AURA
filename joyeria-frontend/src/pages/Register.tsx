import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Mail, Lock, User, Phone } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../contexts/ToastContext'
import { getErrorMessage } from '../utils/errors'
import Button from '../components/Button'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function Register() {
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '', password: '', confirmPassword: '' })
  const [showPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const { register } = useAuth()
  const { toast } = useToast()
  const navigate = useNavigate()

  useDocumentTitle('Crear cuenta')

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (form.password !== form.confirmPassword) { toast('Las contraseñas no coinciden', 'error'); return }
    setLoading(true)
    try {
      await register(form)
      toast('¡Cuenta creada exitosamente!')
      navigate('/')
    } catch (err: unknown) {
      const emailError = err instanceof Error && 'response' in err
        ? (err as { response?: { data?: { data?: { email?: string } } } }).response?.data?.data?.email
        : undefined
      toast(getErrorMessage(err, emailError || 'Error al registrarse'), 'error')
    } finally { setLoading(false) }
  }

  const input = 'w-full pl-9 pr-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition'

  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4 py-16 bg-gradient-to-b from-background to-background-warm">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-lg bg-surface rounded-xl border border-line/70 shadow-sm p-8">
        <div className="text-center mb-8">
          <div className="w-12 h-12 mx-auto rounded-full border-2 border-[#C9A227] flex items-center justify-center mb-3">
            <span className="font-serif text-xl text-[#C9A227] font-bold">J</span>
          </div>
          <h1 className="font-serif text-2xl font-medium text-foreground">Crear cuenta</h1>
          <p className="text-sm text-foreground-faint mt-1">Únete a la experiencia AURA</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm text-foreground-muted mb-1.5">Nombre</label>
              <div className="relative">
                <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
                <input name="firstName" value={form.firstName} onChange={handleChange} required className={input} placeholder="María" />
              </div>
            </div>
            <div>
              <label className="block text-sm text-foreground-muted mb-1.5">Apellido</label>
              <div className="relative">
                <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
                <input name="lastName" value={form.lastName} onChange={handleChange} required className={input} placeholder="García" />
              </div>
            </div>
          </div>
          <div>
            <label className="block text-sm text-foreground-muted mb-1.5">Email</label>
            <div className="relative">
              <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
              <input type="email" name="email" value={form.email} onChange={handleChange} required className={input} placeholder="tu@email.com" />
            </div>
          </div>
          <div>
            <label className="block text-sm text-foreground-muted mb-1.5">Teléfono</label>
            <div className="relative">
              <Phone size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
              <input type="tel" name="phone" value={form.phone} onChange={handleChange} required className={input} placeholder="+57 300 000 0000" />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm text-foreground-muted mb-1.5">Contraseña</label>
              <div className="relative">
                <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
                <input type={showPassword ? 'text' : 'password'} name="password" value={form.password} onChange={handleChange} required minLength={8} pattern="(?=.*[A-Za-z])(?=.*\d).+" title="Debe contener al menos una letra y un número" className={input} placeholder="Mín. 8 caracteres, letra y número" />
              </div>
            </div>
            <div>
              <label className="block text-sm text-foreground-muted mb-1.5">Confirmar</label>
              <div className="relative">
                <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
                <input type={showPassword ? 'text' : 'password'} name="confirmPassword" value={form.confirmPassword} onChange={handleChange} required minLength={8} className={input} placeholder="Repite" />
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2 text-xs text-foreground-faint">
            <input type="checkbox" required className="accent-[#C9A227]" />
            Acepto los <Link to="/terminos" className="text-[#C9A227]">términos</Link> y la <Link to="/privacidad" className="text-[#C9A227]">política de privacidad</Link>
          </div>
          <Button type="submit" loading={loading} className="w-full" size="lg">Crear cuenta</Button>
        </form>
        <div className="mt-6 pt-6 border-t border-line/70 text-center">
          <p className="text-sm text-foreground-faint">¿Ya tienes cuenta? <Link to="/login" className="text-[#C9A227] font-medium hover:underline">Inicia sesión</Link></p>
        </div>
      </motion.div>
    </div>
  )
}
