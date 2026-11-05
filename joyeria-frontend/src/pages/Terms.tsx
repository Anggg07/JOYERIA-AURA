import { Link } from 'react-router-dom'
import useDocumentTitle from '../hooks/useDocumentTitle'

const sections = [
  {
    title: '1. Aceptación de los términos',
    body: 'Al utilizar el sitio web de AURA (aurajoyeria.com), aceptas estos términos y condiciones. Si no estás de acuerdo con ellos, te solicitamos no utilizar nuestros servicios.',
  },
  {
    title: '2. Uso del sitio',
    body: 'Podrás utilizar el sitio para explorar y adquirir productos, gestionar tu cuenta y acceder a información sobre nuestras joyas. Queda prohibido cualquier uso fraudulento, la reproducción indebida de contenido y el acceso no autorizado a información de otros usuarios.',
  },
  {
    title: '3. Precios y pagos',
    body: 'Los precios se expresan en pesos colombianos (COP) y pueden cambiar sin previo aviso. Los precios aplicados serán los vigentes al momento de confirmar la compra. El pago se realiza a través de los métodos habilitados (tarjeta o PSE).',
  },
  {
    title: '4. Pedidos y envíos',
    body: 'Una vez confirmado el pago, procesaremos tu pedido en un plazo máximo de 2 días hábiles. Recibirás un número de seguimiento cuando tu pedido sea despachado. Los tiempos de entrega estimados son de 1 a 7 días hábiles según tu ubicación.',
  },
  {
    title: '5. Devoluciones y cambios',
    body: 'Dispones de 15 días calendario desde la fecha de entrega para solicitar la devolución o cambio de un producto, siempre que esté en perfecto estado y con su certificado y empaque originales.',
  },
  {
    title: '6. Garantía',
    body: 'Nuestras joyas cuentan con garantía de calidad sobre los materiales y la manufactura. La garantía no cubre el desgaste normal ni daños causados por uso indebido. Para hacer válida tu garantía, contáctanos a través de nuestro centro de soporte.',
  },
  {
    title: '7. Propiedad intelectual',
    body: 'Todo el contenido del sitio (diseños, textos, imágenes, logotipos, fotografías de productos) es propiedad de AURA Fine Artisan Jewelry y está protegido por las normas de propiedad intelectual vigentes.',
  },
]

export default function Terms() {
  useDocumentTitle('Términos y condiciones')
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <h1 className="font-serif text-3xl font-medium text-foreground mb-2">Términos y condiciones</h1>
      <p className="text-sm text-foreground-faint mb-10">Última actualización: {new Date().toLocaleDateString('es-CO', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
      <div className="space-y-8">
        {sections.map((s, i) => (
          <div key={i}>
            <h2 className="font-serif text-xl font-medium text-foreground mb-3">{s.title}</h2>
            <p className="text-foreground-muted leading-relaxed">{s.body}</p>
          </div>
        ))}
      </div>
      <p className="text-sm text-foreground-faint mt-10">
        Consulta también nuestra <Link to="/privacidad" style={{ color: '#C9A227' }}>Política de privacidad</Link> y <Link to="/cookies" style={{ color: '#C9A227' }}>Política de cookies</Link>.
      </p>
    </div>
  )
}