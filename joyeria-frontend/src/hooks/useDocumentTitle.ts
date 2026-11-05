import { useEffect } from 'react'

const BASE_TITLE = 'AURA | Joyería Fina Artesanal'

/**
 * Define `document.title` por ruta (F-M10), con el sufijo de marca.
 * Pasar `undefined` restaura el título base.
 */
export default function useDocumentTitle(title?: string) {
  useEffect(() => {
    document.title = title ? `${title} · AURA` : BASE_TITLE
  }, [title])
}
