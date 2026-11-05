import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { Instagram, Facebook, Send, Phone, Mail, MapPin } from 'lucide-react'
import { socialService } from '../services/misc.service'
import { newsletterService } from '../services/misc.service'
import type { SocialLink } from '../types'
import Logo from './Logo'

const socialIcons: Record<string, React.ComponentType<{ size?: number | string }>> = { Instagram, Facebook, Send }

export default function Footer() {
  const [socials, setSocials] = useState<SocialLink[]>([])
  const [email, setEmail] = useState('')
  const [msg, setMsg] = useState('')

  useEffect(() => { socialService.getAll().then(setSocials).catch(() => {}) }, [])

  const subscribe = async (e: React.FormEvent) => {
    e.preventDefault()
    try { const m = await newsletterService.subscribe(email); setMsg(m); setEmail('') }
    catch { setMsg('Error al suscribirse') }
  }

  return (
    <footer className="bg-ink text-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10">
          <div>
            <Link to="/" aria-label="AURA - inicio"><Logo variant="dark" className="w-40 mb-5" /></Link>
            <p className="text-foreground-faint text-sm leading-relaxed mb-4">Fine Artisan Jewelry. Creando joyas excepcionales, hechas a mano para celebrar los momentos más importantes de tu vida.</p>
            <div className="flex gap-3">
              {socials.map(s => {
                const Icon = (s.icon && socialIcons[s.icon]) || Send
                return <a key={s.id} href={s.url} target="_blank" rel="noopener noreferrer"
                  className="w-9 h-9 rounded-full border border-white/15 flex items-center justify-center text-foreground-faint hover:text-[#C9A227] hover:border-[#C9A227] transition-colors">
                  <Icon size={16} /></a>
              })}
            </div>
          </div>

          <div>
            <h4 className="font-serif text-base font-medium mb-5 text-white">Tienda</h4>
            <ul className="space-y-2.5 text-sm text-foreground-faint">
              <li><Link to="/personaliza" className="hover:text-[#C9A227] transition-colors font-medium text-white/80">Personaliza tu joya</Link></li>
              <li><Link to="/productos?category=8" className="hover:text-[#C9A227] transition-colors">Regalos con historia</Link></li>
              {['Anillos', 'Collares', 'Pulseras', 'Aretes', 'Dijes', 'Sets'].map(c => (
                <li key={c}><Link to={`/productos?q=${c.toLowerCase()}`} className="hover:text-[#C9A227] transition-colors">{c}</Link></li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="font-serif text-base font-medium mb-5 text-white">Información</h4>
            <ul className="space-y-2.5 text-sm text-foreground-faint">
              <li><Link to="/nosotros" className="hover:text-[#C9A227] transition-colors">Nosotros</Link></li>
              <li><Link to="/faq" className="hover:text-[#C9A227] transition-colors">Preguntas frecuentes</Link></li>
              <li><Link to="/soporte" className="hover:text-[#C9A227] transition-colors">Soporte</Link></li>
              <li><Link to="/privacidad" className="hover:text-[#C9A227] transition-colors">Política de privacidad</Link></li>
              <li><Link to="/terminos" className="hover:text-[#C9A227] transition-colors">Términos y condiciones</Link></li>
              <li><Link to="/cookies" className="hover:text-[#C9A227] transition-colors">Política de cookies</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-serif text-base font-medium mb-5 text-white">Contacto</h4>
            <ul className="space-y-3 text-sm text-foreground-faint">
              <li className="flex items-center gap-2"><Phone size={15} className="text-[#C9A227]" /> +57 300 123 4567</li>
              <li className="flex items-center gap-2"><Mail size={15} className="text-[#C9A227]" /> info@aurajoyeria.com</li>
              <li className="flex items-start gap-2"><MapPin size={15} className="text-[#C9A227] mt-0.5" /> Bogotá, Colombia</li>
            </ul>
            <div className="mt-5">
              <h5 className="text-sm font-medium text-white mb-2">Suscríbete a novedades</h5>
              <form onSubmit={subscribe} className="flex">
                <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="Tu email" required
                  className="bg-ink-soft text-white text-sm rounded-l px-3 py-2 min-h-11 outline-none border border-white/15 focus:border-[#C9A227] flex-1" />
                <button type="submit" className="bg-[#C9A227] hover:bg-[#b8911f] text-white px-4 py-2 min-h-11 rounded-r text-sm transition-colors">
                  <Send size={15} /></button>
              </form>
              {msg && <p className="text-xs text-accent-green mt-1">{msg}</p>}
            </div>
          </div>
        </div>

        <div className="border-t border-white/10 mt-12 pt-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-sm text-foreground-faint">&copy; {new Date().getFullYear()} AURA Fine Artisan Jewelry. Todos los derechos reservados.</p>
          <div className="flex gap-4 text-xs text-foreground-faint">
            <Link to="/privacidad" className="hover:text-[#C9A227]">Privacidad</Link>
            <Link to="/terminos" className="hover:text-[#C9A227]">Términos</Link>
            <Link to="/cookies" className="hover:text-[#C9A227]">Cookies</Link>
          </div>
        </div>
      </div>
    </footer>
  )
}
