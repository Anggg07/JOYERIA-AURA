import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import Button from '../components/Button'
import useDocumentTitle from '../hooks/useDocumentTitle'

export default function NotFound() {
  useDocumentTitle('Página no encontrada')

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4">
      <motion.h1
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="font-serif text-8xl font-bold text-primary mb-4">
        404
      </motion.h1>
      <p className="font-serif text-2xl font-medium text-foreground mb-2">Página no encontrada</p>
      <p className="text-foreground-faint mb-8 max-w-md">
        La página que buscas no existe, fue movida o el enlace es incorrecto.
      </p>
      <Link to="/"><Button size="lg">Volver al inicio</Button></Link>
    </div>
  )
}