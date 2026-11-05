import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import EditorialCategories from './EditorialCategories'
import type { Category } from '../types'

const categories: Category[] = [
  { id: 1, name: 'Anillos', slug: 'anillos', active: true },
  { id: 2, name: 'Collares', slug: 'collares', active: true },
  { id: 3, name: 'Aretes', slug: 'aretes', active: true },
]

function renderSection(props: Partial<React.ComponentProps<typeof EditorialCategories>> = {}) {
  return render(
    <MemoryRouter>
      <EditorialCategories categories={categories} images={{ 1: '/img/anillos.png' }} {...props} />
    </MemoryRouter>,
  )
}

describe('EditorialCategories', () => {
  it('renders the heading, every category and its numbering', () => {
    renderSection()

    expect(screen.getByRole('heading', { name: 'Encuentra tu pieza perfecta' })).toBeInTheDocument()
    expect(screen.getByText('Colección 01')).toBeInTheDocument()
    expect(screen.getByText('Colección 02')).toBeInTheDocument()
    expect(screen.getByText('Colección 03')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Anillos' })).toHaveAttribute('href', '/productos?category=1')
    expect(screen.getAllByRole('link', { name: /Ver piezas/ })).toHaveLength(3)
  })

  it('opens the zoom viewer when a category photo is clicked', () => {
    renderSection()

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Ampliar imagen de Anillos'))
    expect(screen.getByRole('dialog', { name: /Vista ampliada: Anillos/ })).toBeInTheDocument()
    expect(screen.getByLabelText('Cerrar vista ampliada')).toBeInTheDocument()
  })
})
