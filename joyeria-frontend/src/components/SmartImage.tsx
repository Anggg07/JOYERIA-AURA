import { useState, type ImgHTMLAttributes } from 'react'

/**
 * Placeholder SVG (diamante AURA) para imágenes de producto ausentes o rotas.
 * Reutilizado por ProductCard, QuickViewModal, ProductDetail, carrito, etc. (F-M8).
 */
export const IMAGE_PLACEHOLDER =
  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iNTAwIiB2aWV3Qm94PSIwIDAgNDAwIDUwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI0Y3RjVGMCIvPjxwYXRoIGQ9Ik0yMDAgMjAwIGw1MiAzMCAtNTIgMzAgLTUyLTMwWiIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjQzlBMjI3IiBzdHJva2Utd2lkdGg9IjEuNiIvPjxwYXRoIGQ9Ik0yMDAgMjIyIGwyMCAxMS41IC0yMCAxMS41IC0yMC0xMS41WiIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjQzlBMjI3IiBzdHJva2Utd2lkdGg9IjEiIG9wYWNpdHk9IjAuNSIvPjx0ZXh0IHg9IjIwMCIgeT0iMzAwIiBmb250LWZhbWlseT0iR2VvcmdpYSwgc2VyaWYiIGZvbnQtc2l6ZT0iMjQiIGxldHRlci1zcGFjaW5nPSI5IiBmaWxsPSIjQjlBOTZCIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj5BVVJBPC90ZXh0Pjwvc3ZnPg=='

interface Props extends Omit<ImgHTMLAttributes<HTMLImageElement>, 'src'> {
  src?: string | null
  fallback?: string
  /** Carga inmediata (imagen above-the-fold). Por defecto las imágenes son lazy (F-M14). */
  eager?: boolean
}

/**
 * <img> con fallback robusto: si `src` falta o falla al cargar muestra un
 * placeholder, y aplica `loading="lazy"`/`decoding="async"` por defecto (F-M8, F-M14).
 */
export default function SmartImage({ src, fallback = IMAGE_PLACEHOLDER, eager = false, loading, decoding, onError, ...rest }: Props) {
  const [erroredSrc, setErroredSrc] = useState<string | null>(null)
  const resolved = src && src !== erroredSrc ? src : fallback

  return (
    <img
      src={resolved}
      loading={loading ?? (eager ? 'eager' : 'lazy')}
      decoding={decoding ?? 'async'}
      onError={(e) => {
        if (src && src !== erroredSrc) setErroredSrc(src)
        onError?.(e)
      }}
      {...rest}
    />
  )
}
