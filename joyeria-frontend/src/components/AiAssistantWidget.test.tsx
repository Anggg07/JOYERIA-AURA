import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import AiAssistantWidget from './AiAssistantWidget'

const { askMock, addItemMock, toastMock, authMock } = vi.hoisted(() => ({
  askMock: vi.fn(),
  addItemMock: vi.fn(),
  toastMock: vi.fn(),
  authMock: vi.fn(),
}))

vi.mock('../services/ai.service', () => ({
  aiService: { ask: askMock },
  aiAssistantEnabled: true,
}))

vi.mock('../contexts/CartContext', () => ({ useCart: () => ({ addItem: addItemMock }) }))
vi.mock('../contexts/ToastContext', () => ({ useToast: () => ({ toast: toastMock }) }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => authMock() }))

function renderWidget() {
  return render(
    <MemoryRouter>
      <AiAssistantWidget />
    </MemoryRouter>,
  )
}

describe('AiAssistantWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authMock.mockReturnValue({ isAuthenticated: true })
  })

  it('opens the chat and shows the greeting', () => {
    renderWidget()

    fireEvent.click(screen.getByLabelText('Abrir asistente de compras'))

    expect(screen.getByText(/soy AURA/i)).toBeInTheDocument()
  })

  it('sends the query and renders suggested products', async () => {
    askMock.mockResolvedValue({
      message: 'Estas opciones te pueden gustar.',
      products: [
        { id: 5, name: 'Aretes Luna', slug: 'aretes-luna', price: 120000, image: null, available: true, reason: 'Ideales para regalar' },
      ],
    })
    renderWidget()

    fireEvent.click(screen.getByLabelText('Abrir asistente de compras'))
    fireEvent.change(screen.getByLabelText('Escribe tu consulta'), { target: { value: 'busco un regalo' } })
    fireEvent.click(screen.getByLabelText('Enviar consulta'))

    expect(await screen.findByText('Estas opciones te pueden gustar.')).toBeInTheDocument()
    expect(screen.getByText('Aretes Luna')).toBeInTheDocument()
    expect(askMock).toHaveBeenCalledWith('busco un regalo', [])
  })

  it('renders follow-up suggestions and sends one when clicked', async () => {
    askMock
      .mockResolvedValueOnce({
        message: 'Estas opciones te pueden gustar.',
        products: [],
        followUpQuestion: '¿Tienes un presupuesto en mente?',
        suggestions: ['Más barato', 'Solo disponibles'],
      })
      .mockResolvedValue({ message: 'Listo.', products: [] })
    renderWidget()

    fireEvent.click(screen.getByLabelText('Abrir asistente de compras'))
    fireEvent.change(screen.getByLabelText('Escribe tu consulta'), { target: { value: 'busco un regalo' } })
    fireEvent.click(screen.getByLabelText('Enviar consulta'))

    expect(await screen.findByText('¿Tienes un presupuesto en mente?')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Más barato' }))

    await waitFor(() =>
      expect(askMock).toHaveBeenLastCalledWith('Más barato', expect.any(Array)),
    )
  })

  it('adds a suggested product to the cart', async () => {
    askMock.mockResolvedValue({
      message: 'Mira esto.',
      products: [{ id: 5, name: 'Aretes Luna', slug: 'aretes-luna', price: 120000, available: true }],
    })
    renderWidget()

    fireEvent.click(screen.getByLabelText('Abrir asistente de compras'))
    fireEvent.change(screen.getByLabelText('Escribe tu consulta'), { target: { value: 'aretes' } })
    fireEvent.click(screen.getByLabelText('Enviar consulta'))
    fireEvent.click(await screen.findByLabelText('Agregar Aretes Luna al carrito'))

    expect(addItemMock).toHaveBeenCalledWith(5)
    await waitFor(() =>
      expect(toastMock).toHaveBeenCalledWith(expect.stringContaining('Aretes Luna'), 'success'),
    )
  })

  it('invites guests to sign in before adding to the cart', async () => {
    authMock.mockReturnValue({ isAuthenticated: false })
    askMock.mockResolvedValue({
      message: 'Mira esto.',
      products: [{ id: 5, name: 'Aretes Luna', slug: 'aretes-luna', price: 120000, available: true }],
    })
    renderWidget()

    fireEvent.click(screen.getByLabelText('Abrir asistente de compras'))
    fireEvent.change(screen.getByLabelText('Escribe tu consulta'), { target: { value: 'aretes' } })
    fireEvent.click(screen.getByLabelText('Enviar consulta'))
    fireEvent.click(await screen.findByLabelText('Agregar Aretes Luna al carrito'))

    expect(addItemMock).not.toHaveBeenCalled()
    expect(toastMock).toHaveBeenCalledWith(expect.stringMatching(/inicia sesión/i), 'info')
  })

  it('shows a fallback message when the assistant fails', async () => {
    askMock.mockRejectedValue(new Error('boom'))
    renderWidget()

    fireEvent.click(screen.getByLabelText('Abrir asistente de compras'))
    fireEvent.change(screen.getByLabelText('Escribe tu consulta'), { target: { value: 'hola' } })
    fireEvent.click(screen.getByLabelText('Enviar consulta'))

    expect(await screen.findByText(/no pudimos procesar/i)).toBeInTheDocument()
  })
})
