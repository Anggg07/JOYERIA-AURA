import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { CartProvider, useCart } from './CartContext'
import type { Cart } from '../types'

const { authMock, getCartMock, addItemMock, updateQuantityMock, removeItemMock, clearCartMock } = vi.hoisted(() => ({
  authMock: vi.fn(),
  getCartMock: vi.fn(),
  addItemMock: vi.fn(),
  updateQuantityMock: vi.fn(),
  removeItemMock: vi.fn(),
  clearCartMock: vi.fn(),
}))

vi.mock('./AuthContext', () => ({ useAuth: () => authMock() }))

vi.mock('../services/cart.service', () => ({
  cartService: {
    getCart: getCartMock,
    addItem: addItemMock,
    updateQuantity: updateQuantityMock,
    removeItem: removeItemMock,
    clearCart: clearCartMock,
    getSummary: vi.fn(),
  },
}))

const emptyCart: Cart = { id: 1, items: [], itemCount: 0, subtotal: 0 }
const cartWithItem: Cart = {
  id: 1,
  itemCount: 1,
  subtotal: 100,
  items: [{
    id: 10, productId: 5, productName: 'Anillo', productSlug: 'anillo',
    productPrice: 100, productStock: 3, quantity: 1, subtotal: 100,
  }],
}

const wrapper = ({ children }: { children: ReactNode }) => <CartProvider>{children}</CartProvider>

describe('CartContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authMock.mockReturnValue({ isAuthenticated: true })
    getCartMock.mockResolvedValue(emptyCart)
  })

  it('loads the cart on mount when authenticated', async () => {
    const { result } = renderHook(() => useCart(), { wrapper })

    await waitFor(() => expect(result.current.cart).toEqual(emptyCart))
    expect(getCartMock).toHaveBeenCalledTimes(1)
  })

  it('does not fetch when the user is unauthenticated', async () => {
    authMock.mockReturnValue({ isAuthenticated: false })

    const { result } = renderHook(() => useCart(), { wrapper })

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(getCartMock).not.toHaveBeenCalled()
    expect(result.current.cart).toBeNull()
  })

  it('addItem updates the cart and reports itemCount', async () => {
    addItemMock.mockResolvedValue(cartWithItem)
    const { result } = renderHook(() => useCart(), { wrapper })
    await waitFor(() => expect(result.current.cart).toEqual(emptyCart))

    await act(async () => { await result.current.addItem(5, 2) })

    expect(addItemMock).toHaveBeenCalledWith(5, 2)
    expect(result.current.cart).toEqual(cartWithItem)
    expect(result.current.itemCount).toBe(1)
  })

  it('surfaces a friendly error when a mutation fails', async () => {
    addItemMock.mockRejectedValue({ response: { data: { message: 'Sin stock' } } })
    const { result } = renderHook(() => useCart(), { wrapper })
    await waitFor(() => expect(result.current.cart).toEqual(emptyCart))

    await act(async () => {
      await result.current.addItem(5).catch(() => undefined)
    })

    expect(result.current.error).toBe('Sin stock')
    expect(result.current.pendingItemId).toBeNull()
  })

  it('clearCart resets the cart to null', async () => {
    const { result } = renderHook(() => useCart(), { wrapper })
    await waitFor(() => expect(result.current.cart).toEqual(emptyCart))

    await act(async () => { await result.current.clearCart() })

    expect(clearCartMock).toHaveBeenCalled()
    expect(result.current.cart).toBeNull()
  })
})