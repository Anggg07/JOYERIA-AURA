import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ProductCard from './ProductCard'
import type { Product } from '../types'

const product: Product = {
  id: 7,
  name: 'Anillo de oro',
  slug: 'anillo-de-oro',
  price: 150000,
  comparePrice: 200000,
  sku: 'AN-1',
  stock: 3,
  featured: false,
  isNew: true,
  bestSeller: false,
  active: true,
  soldCount: 2,
  categoryName: 'Anillos',
  discountPercent: 25,
  images: [{ id: 1, url: '/img/a.png', isPrimary: true, sortOrder: 0 }],
  averageRating: 4.5,
  reviewCount: 10,
}

function renderCard(props: Partial<React.ComponentProps<typeof ProductCard>> = {}) {
  return render(
    <MemoryRouter>
      <ProductCard product={product} {...props} />
    </MemoryRouter>,
  )
}

describe('ProductCard', () => {
  it('renders the product name, category and badges', () => {
    renderCard({ onQuickView: vi.fn() })

    expect(screen.getByText('Anillo de oro')).toBeInTheDocument()
    expect(screen.getByText('Anillos')).toBeInTheDocument()
    expect(screen.getByText('-25%')).toBeInTheDocument()
    expect(screen.getByText('Nuevo')).toBeInTheDocument()
  })

  it('invokes onQuickView with the product', () => {
    const onQuickView = vi.fn()
    renderCard({ onQuickView })

    fireEvent.click(screen.getByLabelText(`Vista rápida de ${product.name}`))

    expect(onQuickView).toHaveBeenCalledWith(product)
  })

  it('invokes onAddToCart with the product id', () => {
    const onAddToCart = vi.fn()
    renderCard({ onAddToCart })

    fireEvent.click(screen.getByText('Agregar al carrito'))

    expect(onAddToCart).toHaveBeenCalledWith(7)
  })

  it('shows a "Ver producto" link when there is no add-to-cart handler', () => {
    renderCard()

    expect(screen.getByText('Ver producto')).toBeInTheDocument()
  })

  it('shows "Agotado" and hides add-to-cart when out of stock', () => {
    render(
      <MemoryRouter>
        <ProductCard product={{ ...product, stock: 0 }} onAddToCart={vi.fn()} />
      </MemoryRouter>,
    )

    expect(screen.getByText('Agotado')).toBeInTheDocument()
    expect(screen.queryByText('Agregar al carrito')).not.toBeInTheDocument()
  })
})