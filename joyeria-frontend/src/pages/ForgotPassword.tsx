import { useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Mail, ArrowLeft } from 'lucide-react'
import { useToast } from '../contexts/ToastContext'
import { authService } from '../services/auth.service'
import Button from '../components/Button'
import { getErrorMessage } from '../utils/errors'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)
  const { toast } = useToast()

  useDocumentTitle('Recuperar contraseña')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const message = await authService.forgotPassword(email)
      setSent(true)
      toast(message)
    } catch (err: unknown) {
      const message = getErrorMessage(err, 'Error al enviar la solicitud')
      toast(message || 'Error al enviar la solicitud', 'error')
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
          <h1 className="font-serif text-2xl font-medium text-foreground">Recuperar contraseña</h1>
          <p className="text-sm text-foreground-faint mt-2">
            {sent
              ? 'Si tu email está registrado, recibirás un enlace para restablecer tu contraseña. Revisa también la carpeta de spam.'
              : 'Ingresa tu email y te enviaremos un enlace para restablecer tu contraseña.'}
          </p>
        </div>

        {!sent && (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm text-foreground-muted mb-1.5">Email</label>
              <div className="relative">
                <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-foreground-faint" />
                <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
                  className="w-full pl-9 pr-3 py-2.5 border border-line rounded-lg text-sm outline-none focus:border-[#C9A227] transition" placeholder="tu@email.com" />
              </div>
            </div>
            <Button type="submit" loading={loading} className="w-full" size="lg">Enviar enlace</Button>
          </form>
        )}

        <div className="mt-6 pt-6 border-t border-line/70 text-center">
          <Link to="/login" className="inline-flex items-center gap-1.5 text-sm text-[#C9A227] font-medium hover:underline">
            <ArrowLeft size={14} /> Volver a iniciar sesión
          </Link>
        </div>
      </motion.div>
    </div>
  )
}