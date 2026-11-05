import { useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight } from 'lucide-react'
import type { Category } from '../types'
import SmartImage from './SmartImage'
import ImageLightbox from './ImageLightbox'

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: 'easeOut' as const } },
}

interface CategoryTileProps {
  category: Category
  image: string | null
  index: number
  /** Desfase vertical para la retícula asimétrica (columnas alternas). */
  offset: boolean
  onZoom: (src: string | null, alt: string) => void
}

function CategoryTile({ category, image, index, offset, onZoom }: CategoryTileProps) {
  const href = `/productos?category=${category.id}`

  return (
    <motion.figure
      variants={fadeUp}
      initial="hidden"
      whileInView="visible"
      viewport={{ once: true, margin: '-60px' }}
      transition={{ delay: (index % 4) * 0.06 }}
      className={`group/cat flex flex-col ${offset ? 'md:mt-14 lg:mt-16' : ''}`}
    >
      {/* Foto protagonista: sin marco, sin esquinas pesadas */}
      <button
        type="button"
        onClick={() => onZoom(image, category.name)}
        aria-label={`Ampliar imagen de ${category.name}`}
        className="group/photo relative block w-full overflow-hidden bg-surface-muted focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-gold focus-visible:ring-offset-4 focus-visible:ring-offset-background-warm"
      >
        <div className="aspect-[4/5] w-full overflow-hidden">
          <SmartImage
            src={image}
            alt={category.name}
            draggable={false}
            className="h-full w-full object-cover transition-transform duration-[400ms] ease-out will-change-transform group-hover/photo:scale-[1.04]"
          />
        </div>
        <span
          aria-hidden
          className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/30 via-transparent to-transparent opacity-0 transition-opacity duration-500 group-hover/photo:opacity-100"
        />
      </button>

      <figcaption className="mt-5 flex flex-col">
        <span className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">
          Colección {String(index + 1).padStart(2, '0')}
        </span>
        <Link
          to={href}
          className="mt-2 font-serif text-lg font-medium leading-snug text-foreground transition-colors hover:text-gold md:text-xl"
        >
          {category.name}
        </Link>
        <Link
          to={href}
          className="group/link mt-2 inline-flex items-center gap-1.5 text-[12px] font-medium uppercase tracking-[0.18em] text-foreground-muted transition-colors hover:text-gold"
        >
          Ver piezas
          <ArrowRight size={13} className="transition-transform duration-300 ease-out group-hover/link:translate-x-1" />
        </Link>
      </figcaption>
    </motion.figure>
  )
}

interface EditorialCategoriesProps {
  categories: Category[]
  images?: Record<string, string>
  loading?: boolean
}

/**
 * Sección editorial de colecciones: retícula asimétrica y sin marcos donde la
 * fotografía es la protagonista sobre el fondo marfil. Cada imagen abre un
 * visor ampliado y enlaza al catálogo filtrado por categoría.
 */
export default function EditorialCategories({ categories, images = {}, loading = false }: EditorialCategoriesProps) {
  const [zoom, setZoom] = useState<{ src: string | null; alt: string } | null>(null)
  const imageFor = (category: Category) => images[category.id] ?? category.image ?? null

  return (
    <section className="relative overflow-hidden bg-background-warm py-24 sm:py-28 lg:py-32">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Encabezado con aire: mucho espacio negativo */}
        <div className="grid items-end gap-x-10 gap-y-8 lg:grid-cols-12">
          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            variants={fadeUp}
            className="lg:col-span-7"
          >
            <p className="text-[11px] font-medium uppercase tracking-[0.3em] text-gold">Categorías</p>
            <h2 className="mt-4 font-serif text-4xl font-medium leading-[1.1] text-foreground sm:text-5xl">
              Encuentra tu pieza perfecta
            </h2>
          </motion.div>

          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            variants={fadeUp}
            className="lg:col-span-5 lg:pb-2"
          >
            <p className="max-w-md text-sm leading-relaxed text-foreground-muted">
              Formas puras y materiales nobles para los momentos que se recuerdan. Descubre cada
              colección en su propia sala.
            </p>
            <Link
              to="/productos"
              className="group mt-5 inline-flex items-center gap-1.5 text-[12px] font-medium uppercase tracking-[0.2em] text-foreground transition-colors hover:text-gold"
            >
              Ver todo el catálogo
              <ArrowRight size={14} className="transition-transform duration-300 ease-out group-hover:translate-x-1" />
            </Link>
          </motion.div>
        </div>

        <div className="mt-16 grid grid-cols-2 gap-x-4 gap-y-12 sm:gap-x-6 md:grid-cols-3 lg:grid-cols-4 lg:gap-x-8 lg:gap-y-16">
          {loading
            ? Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className={i % 2 === 1 ? 'md:mt-14 lg:mt-16' : ''}>
                  <div className="aspect-[4/5] w-full animate-pulse bg-surface-elevated" />
                  <div className="mt-5 h-3 w-24 animate-pulse rounded bg-surface-elevated" />
                  <div className="mt-3 h-4 w-20 animate-pulse rounded bg-surface-elevated" />
                </div>
              ))
            : categories.map((category, i) => (
                <CategoryTile
                  key={category.id}
                  category={category}
                  image={imageFor(category)}
                  index={i}
                  offset={i % 2 === 1}
                  onZoom={(src, alt) => setZoom({ src, alt })}
                />
              ))}
        </div>
      </div>

      <ImageLightbox
        src={zoom?.src ?? null}
        alt={zoom?.alt ?? ''}
        isOpen={zoom !== null}
        onClose={() => setZoom(null)}
        caption={zoom?.alt}
      />
    </section>
  )
}
