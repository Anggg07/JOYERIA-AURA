import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { ChevronDown, HelpCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import useDocumentTitle from '../hooks/useDocumentTitle'

const faqs = [
  {
    q: '¿Qué medios de pago aceptan?',
    a: 'Aceptamos tarjetas de crédito y débito (Visa, Mastercard, American Express) y pagos a través de PSE con cuentas bancarias colombianas. Todos los pagos están protegidos y no almacenamos datos de tarjetas.',
  },
  {
    q: '¿Realizan envíos a todo el país?',
    a: 'Sí. Realizamos envíos a todo el territorio colombiano a través de mensajería certificada. Los envíos a Bogotá demoran entre 1 y 3 días hábiles, y al resto del país entre 3 y 7 días hábiles.',
  },
  {
    q: '¿El envío es gratis?',
    a: 'Sí, el envío es gratis para compras superiores a $500.000 COP. Para compras menores se aplica una tarifa de $15.000 COP.',
  },
  {
    q: '¿Cómo sé que las joyas son originales?',
    a: 'Cada joya incluye un certificado de autenticidad y materiales. Trabajamos solo con oro 18 kilates (750), plata 925 y piedras preciosas certificadas.',
  },
  {
    q: '¿Puedo devolver o cambiar un producto?',
    a: 'Tienes 15 días calendario desde la entrega para solicitar devolución o cambio, siempre que la joya esté en su estado original con el certificado y empaque.',
  },
  {
    q: '¿Cómo cuido mis joyas?',
    a: 'Evita el contacto con perfumes, cremas y productos químicos. Guarda tus joyas en su estuche para evitar rayones y límpialas con un paño suave después de usarlas.',
  },
]

export default function FAQ() {
  const [open, setOpen] = useState<number | null>(0)

  useDocumentTitle('Preguntas frecuentes')

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="text-center mb-10">
        <div className="w-16 h-16 rounded-full bg-[#C9A227]/10 flex items-center justify-center mx-auto mb-4">
          <HelpCircle size={32} className="text-[#C9A227]" />
        </div>
        <h1 className="font-serif text-3xl font-medium text-foreground mb-2">Preguntas frecuentes</h1>
        <p className="text-foreground-faint">Encuentra respuestas a las dudas más comunes</p>
      </div>

      <div className="space-y-3">
        {faqs.map((f, i) => (
          <div key={i} className="bg-surface rounded-lg border border-line/70 overflow-hidden">
            <button onClick={() => setOpen(open === i ? null : i)} className="w-full flex items-center justify-between gap-4 p-4 text-left">
              <span className="font-medium text-foreground text-sm">{f.q}</span>
              <ChevronDown size={18} className={`shrink-0 text-[#C9A227] transition-transform ${open === i ? 'rotate-180' : ''}`} />
            </button>
            <AnimatePresence initial={false}>
              {open === i && (
                <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.25 }}>
                  <p className="px-4 pb-4 text-sm text-foreground-muted leading-relaxed">{f.a}</p>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        ))}
      </div>

      <div className="text-center mt-10">
        <p className="text-foreground-faint mb-4">¿No encontraste tu respuesta?</p>
        <Link to="/soporte" className="inline-flex items-center gap-2 px-6 py-3 bg-[#C9A227] hover:bg-[#b8911f] text-white rounded-lg font-medium transition"
          style={{ textDecoration: 'none' }}>Contactar soporte</Link>
      </div>
    </div>
  )
}