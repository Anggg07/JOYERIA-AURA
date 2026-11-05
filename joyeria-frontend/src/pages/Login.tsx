import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Mail, Lock, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../contexts/ToastContext'
import Button from '../components/Button'
import { getErrorMessage } from '../utils/errors'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const { toast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const expiredMessage = (location.state as { message?: string } | null)?.message

  useDocumentTitle('Iniciar sesión')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await login(email, password)
      toast('¡Bienvenido de nuevo!')
      navigate('/')
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al iniciar sesión')
      toast(message || 'Error al iniciar sesión', 'error')
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
          <h1 className="font-serif text-2xl font-medium text-foreground">Iniciar sesión</h1>
          <p className="text-sm text-foreground-faint mt-1">Bienvenido de vuelta a AURA</p>
        </div>
        {expiredMessage && (
          <div className="mb-5 px-4 py-3 rounded-lg bg-amber-500/10 border border-amber-500/30 text-sm text-amber-700 dark:text-amber-400" role="status">
            {expiredMessage}
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-foreground-muted mb-1.5">Email</label>
            <div className="relative">
              <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
                className="w-full pl-9 pr-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition" placeholder="tu@email.com" />
            </div>
          </div>
          <div>
            <label className="block text-sm text-foreground-muted mb-1.5">Contraseña</label>
            <div className="relative">
              <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
              <input type={showPassword ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)} required
                className="w-full pl-9 pr-10 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition" placeholder="Tu contraseña" />
              <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-foreground-faint hover:text-foreground-muted">
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>
          <div className="flex justify-between items-center text-sm">
            <label className="flex items-center gap-2 text-foreground-faint"><input type="checkbox" className="accent-[#C9A227]" /> Recordarme</label>
            <Link to="/recuperar-contrasena" className="text-[#C9A227] hover:underline">¿Olvidaste tu contraseña?</Link>
          </div>
          <Button type="submit" loading={loading} className="w-full" size="lg">Iniciar sesión</Button>
        </form>
        <div className="mt-6 pt-6 border-t border-line/70 text-center">
          <p className="text-sm text-foreground-faint">¿No tienes cuenta? <Link to="/registro" className="text-[#C9A227] font-medium hover:underline">Regístrate</Link></p>
        </div>
      </motion.div>
    </div>
  )
}
