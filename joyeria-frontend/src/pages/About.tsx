import useDocumentTitle from '../hooks/useDocumentTitle'

export default function About() {
  useDocumentTitle('Nosotros — AURA')
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <h1 className="font-serif text-3xl font-medium text-foreground mb-6">Nosotros</h1>
      <p className="text-foreground-muted leading-relaxed mb-4">
        AURA es una joyería boutique dedicada a crear piezas que acompañan tus historias.
        Cada joya se elabora con materiales nobles y un cuidado artesanal, combinando
        tradición y diseño contemporáneo.
      </p>
      <p className="text-foreground-muted leading-relaxed mb-4">
        Creemos en la belleza que perdura: oro 18 kilates, plata 925 y piedras preciosas
        certificadas, seleccionadas una a una. Nuestro compromiso es ofrecerte una
        experiencia cercana, transparente y memorable, desde el primer clic hasta que
        la joya llega a tus manos.
      </p>
      <p className="text-foreground-muted leading-relaxed">
        Gracias por elegir AURA. Estamos aquí para ayudarte a encontrar la pieza perfecta
        para cada momento.
      </p>
    </div>
  )
}