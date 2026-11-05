import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import ImageLightbox from './ImageLightbox'

describe('ImageLightbox', () => {
  it('does not render when closed', () => {
    render(<ImageLightbox src="/img/a.png" alt="Anillo" isOpen={false} onClose={vi.fn()} />)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('renders an accessible dialog with the image and caption', () => {
    render(<ImageLightbox src="/img/a.png" alt="Anillo de oro" isOpen onClose={vi.fn()} caption="Anillos" />)

    expect(screen.getByRole('dialog', { name: /Vista ampliada: Anillo de oro/ })).toBeInTheDocument()
    expect(screen.getByAltText('Anillo de oro')).toBeInTheDocument()
    expect(screen.getByText('Anillos')).toBeInTheDocument()
  })

  it('closes with the close button and the Escape key', () => {
    const onClose = vi.fn()
    render(<ImageLightbox src="/img/a.png" alt="Anillo de oro" isOpen onClose={onClose} />)

    fireEvent.click(screen.getByLabelText('Cerrar vista ampliada'))
    expect(onClose).toHaveBeenCalledTimes(1)

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(2)
  })

  it('zooms in and out with the on-screen controls', () => {
    render(<ImageLightbox src="/img/a.png" alt="Anillo de oro" isOpen onClose={vi.fn()} />)

    expect(screen.getByText('100%')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Acercar'))
    expect(screen.getByText('160%')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Restablecer zoom'))
    expect(screen.getByText('100%')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Alejar'))
    expect(screen.getByText('100%')).toBeInTheDocument()
  })

  it('locks body scroll while open', () => {
    const { unmount } = render(<ImageLightbox src="/img/a.png" alt="Anillo de oro" isOpen onClose={vi.fn()} />)

    expect(document.body.style.overflow).toBe('hidden')
    unmount()
    expect(document.body.style.overflow).not.toBe('hidden')
  })
})
