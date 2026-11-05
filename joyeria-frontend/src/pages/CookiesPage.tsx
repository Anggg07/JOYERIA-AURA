import useDocumentTitle from '../hooks/useDocumentTitle'

const cookieTypes = [
  {
    title: 'Cookies esenciales',
    body: 'Necesarias para el funcionamiento básico del sitio: inicio de sesión, carrito de compras y preferencias de seguridad. No pueden desactivarse.',
    required: true,
  },
  {
    title: 'Cookies de preferencias',
    body: 'Recuerdan información como tu idioma, moneda y preferencias de navegación para ofrecerte una experiencia personalizada.',
    required: false,
  },
  {
    title: 'Cookies analíticas',
    body: 'Nos permiten entender cómo usas el sitio (páginas visitadas, tiempo de permanencia) para mejorar nuestros servicios. La información es anónima y agregada.',
    required: false,
  },
  {
    title: 'Cookies de marketing',
    body: 'Se utilizan para mostrarte anuncios relevantes sobre nuestras colecciones en otros sitios web y plataformas.',
    required: false,
  },
]

export default function CookiesPage() {
  useDocumentTitle('Política de cookies')

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <h1 className="font-serif text-3xl font-medium text-foreground mb-2">Política de cookies</h1>
      <p className="text-sm text-foreground-faint mb-10">Última actualización: {new Date().toLocaleDateString('es-CO', { year: 'numeric', month: 'long', day: 'numeric' })}</p>

      <p className="text-foreground-muted leading-relaxed mb-8">
        Una cookie es un pequeño archivo que se almacena en tu dispositivo cuando visitas nuestro sitio web. Su objetivo principal es
        recordar información sobre tu visita y mejorar tu experiencia de navegación. A continuación explicamos qué cookies utilizamos
        y para qué.
      </p>

      <div className="space-y-6">
        {cookieTypes.map((c, i) => (
          <div key={i} className="bg-surface rounded-lg border border-line/70 p-5">
            <div className="flex items-center justify-between mb-2">
              <h2 className="font-serif text-lg font-medium text-foreground">{c.title}</h2>
              {c.required
                ? <span className="text-[10px] bg-accent-green/10 text-accent-green px-2 py-1 rounded-full uppercase">Siempre activas</span>
                : <label className="flex items-center gap-2 text-xs text-foreground-faint">
                    Activar <input type="checkbox" defaultChecked className="accent-[#C9A227]" />
                  </label>}
            </div>
            <p className="text-sm text-foreground-muted leading-relaxed">{c.body}</p>
          </div>
        ))}
      </div>

      <div className="mt-10 p-5 bg-background-warm rounded-lg">
        <h2 className="font-serif text-lg font-medium text-foreground mb-2">¿Cómo gestionar las cookies?</h2>
        <p className="text-sm text-foreground-muted leading-relaxed">
          Puedes configurar tu navegador para bloquear o eliminar cookies desde los ajustes de privacidad de tu navegador
          (Chrome, Safari, Firefox, Edge). Ten en cuenta que al desactivar las cookies esenciales, algunas funciones del sitio
          podrían dejar de funcionar correctamente.
        </p>
      </div>

      <p className="text-sm text-foreground-faint mt-8">
        Para más información, consulta nuestra <span style={{ color: '#C9A227' }}>Política de privacidad</span>.
      </p>
    </div>
  )
}