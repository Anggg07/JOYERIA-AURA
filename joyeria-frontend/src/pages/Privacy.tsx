import { Link } from 'react-router-dom'
import useDocumentTitle from '../hooks/useDocumentTitle'

const sections = [
  {
    title: '1. Información que recopilamos',
    body: 'Recopilamos información que nos proporcionas al registrarte (nombre, apellido, correo electrónico, teléfono), información de envío y facturación, e información sobre tus compras. No almacenamos datos de tarjetas de crédito: el procesamiento de pagos se realiza a través de pasarelas seguras.',
  },
  {
    title: '2. Uso de la información',
    body: 'Utilizamos tus datos para procesar pedidos y pagos, gestionar tu cuenta, enviar notificaciones sobre el estado de tus compras y, con tu consentimiento, enviarte comunicaciones comerciales sobre nuestras colecciones y promociones.',
  },
  {
    title: '3. Protección de datos',
    body: 'Implementamos medidas técnicas y organizativas para proteger tu información personal contra accesos no autorizados, alteración, divulgación o destrucción. El acceso a tus datos solo está disponible para personal autorizado.',
  },
  {
    title: '4. Cookies',
    body: 'Utilizamos cookies para mejorar tu experiencia de navegación, recordar tus preferencias y analizar el tráfico del sitio. Puedes gestionar tus preferencias de cookies en cualquier momento desde nuestra página de cookies.',
  },
  {
    title: '5. Compartir información con terceros',
    body: 'Solo compartimos información con terceros cuando es necesario para el funcionamiento del servicio (mensajería, pasarelas de pago, proveedores de analítica) y nunca vendemos tus datos personales.',
  },
  {
    title: '6. Tus derechos',
    body: 'De acuerdo con la Ley 1581 de 2012, tienes derecho a conocer, actualizar, rectificar y solicitar la supresión de tus datos personales. Puedes ejercer estos derechos escribiéndonos a privacidad@aurajoyeria.com.',
  },
]

export default function Privacy() {
  useDocumentTitle('Política de privacidad')
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <h1 className="font-serif text-3xl font-medium text-foreground mb-2">Política de privacidad</h1>
      <p className="text-sm text-foreground-faint mb-10">Última actualización: {new Date().toLocaleDateString('es-CO', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
      <div className="prose space-y-8">
        {sections.map((s, i) => (
          <div key={i}>
            <h2 className="font-serif text-xl font-medium text-foreground mb-3">{s.title}</h2>
            <p className="text-foreground-muted leading-relaxed">{s.body}</p>
          </div>
        ))}
      </div>
      <p className="text-sm text-foreground-faint mt-10">
        Para más información, revisa nuestras <Link to="/terminos" style={{ color: '#C9A227' }}>Condiciones de uso</Link> o <Link to="/cookies" style={{ color: '#C9A227' }}>Política de cookies</Link>.
      </p>
    </div>
  )
}