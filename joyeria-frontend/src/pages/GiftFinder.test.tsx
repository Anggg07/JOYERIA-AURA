import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import GiftFinder from './GiftFinder'

const { recommendMock, materialsMock, addItemMock, toastMock, authMock } = vi.hoisted(() => ({
  recommendMock: vi.fn(),
  materialsMock: vi.fn(),
  addItemMock: vi.fn(),
  toastMock: vi.fn(),
  authMock: vi.fn(),
}))

vi.mock('../services/gift.service', () => ({ giftService: { recommend: recommendMock } }))
vi.mock('../services/material.service', () => ({ materialService: { getAll: materialsMock } }))
vi.mock('../contexts/CartContext', () => ({ useCart: () => ({ addItem: addItemMock }) }))
vi.mock('../contexts/ToastContext', () => ({ useToast: () => ({ toast: toastMock }) }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => authMock() }))

const RECOMMENDATION = {
  id: 1,
  name: 'Anillo Aurora',
  slug: 'anillo-aurora',
  price: 1850000,
  image: null,
  material: 'Oro 18K',
  categoryName: 'Anillos',
  available: true,
  relevance: 90,
  explanation: 'Lo recomendamos porque se ajusta a tu presupuesto.',
}

function renderPage(path = '/gift-finder') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <GiftFinder />
    </MemoryRouter>,
  )
}

async function completeWizard() {
  fireEvent.click(screen.getByText('Mi pareja'))
  fireEvent.click(screen.getByText('Continuar'))
  fireEvent.click(screen.getByText('Cumpleaños'))
  fireEvent.click(screen.getByText('Continuar'))
  fireEvent.click(screen.getByText('Elegante'))
  fireEvent.click(screen.getByText('Continuar'))
  fireEvent.click(screen.getByText('Hasta $500.000'))
  fireEvent.click(screen.getByText('Continuar'))
  fireEvent.click(await screen.findByText('Oro 18K'))
  fireEvent.click(screen.getByText('Buscar regalos ideales'))
}

describe('GiftFinder', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authMock.mockReturnValue({ isAuthenticated: true })
    materialsMock.mockResolvedValue([{ id: 1, name: 'Oro 18K', slug: 'oro-18k', active: true }])
    recommendMock.mockResolvedValue({ recommendations: [RECOMMENDATION] })
  })

  it('renders the first question and progress', () => {
    renderPage()
    expect(screen.getByText('¿Para quién es el regalo?')).toBeInTheDocument()
    expect(screen.getByText('Paso 1 de 5')).toBeInTheDocument()
  })

  it('sends the selected criteria and renders real recommendations', async () => {
    renderPage()
    await completeWizard()

    expect(await screen.findByText('Anillo Aurora')).toBeInTheDocument()
    expect(recommendMock).toHaveBeenCalledWith({
      recipient: 'pareja',
      occasion: 'cumpleanos',
      style: 'elegante',
      budgetMax: 500000,
      material: 'Oro 18K',
    })
    expect(screen.getByText('90% coincidencia')).toBeInTheDocument()
  })

  it('shows an empty state when there are no matches', async () => {
    recommendMock.mockResolvedValue({ recommendations: [] })
    renderPage()
    await completeWizard()

    expect(await screen.findByText(/No encontramos coincidencias/)).toBeInTheDocument()
  })

  it('flags out-of-stock products as unavailable', async () => {
    recommendMock.mockResolvedValue({ recommendations: [{ ...RECOMMENDATION, available: false }] })
    renderPage()
    await completeWizard()

    expect(await screen.findByText('Agotado')).toBeInTheDocument()
    expect(screen.getByText('Sin stock')).toBeInTheDocument()
  })

  it('works with incomplete filters (no estoy seguro)', async () => {
    renderPage()

    fireEvent.click(screen.getByText('No estoy seguro'))
    fireEvent.click(screen.getByText('No estoy seguro'))
    fireEvent.click(screen.getByText('No estoy seguro'))
    fireEvent.click(screen.getByText('No estoy seguro'))
    fireEvent.click(screen.getByText('No estoy seguro'))

    await waitFor(() => expect(recommendMock).toHaveBeenCalledWith({}))
    expect(await screen.findByText('Anillo Aurora')).toBeInTheDocument()
  })

  it('loads shared criteria from the URL on mount', async () => {
    renderPage('/gift-finder?occasion=boda&budget=lt500k')

    await waitFor(() =>
      expect(recommendMock).toHaveBeenCalledWith({ occasion: 'boda', budgetMax: 500000 }),
    )
  })

  it('adds an available recommendation to the cart', async () => {
    renderPage()
    await completeWizard()

    fireEvent.click(await screen.findByLabelText('Agregar Anillo Aurora al carrito'))

    await waitFor(() => expect(addItemMock).toHaveBeenCalledWith(1, 1))
    expect(toastMock).toHaveBeenCalledWith(expect.stringContaining('Anillo Aurora'), 'success')
  })

  it('invites guests to sign in before adding to the cart', async () => {
    authMock.mockReturnValue({ isAuthenticated: false })
    renderPage()
    await completeWizard()

    fireEvent.click(await screen.findByLabelText('Agregar Anillo Aurora al carrito'))

    expect(addItemMock).not.toHaveBeenCalled()
    expect(toastMock).toHaveBeenCalledWith(expect.stringMatching(/Inicia sesión/), 'info')
  })
})
