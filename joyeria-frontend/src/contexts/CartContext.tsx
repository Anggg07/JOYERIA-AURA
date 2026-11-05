import { createContext, useContext, useState, useEffect, useCallback, useMemo, type ReactNode } from 'react'
import type { Cart, CartItem } from '../types'
import { cartService } from '../services/cart.service'
import { productService } from '../services/product.service'
import { getErrorMessage } from '../utils/errors'
import { useAuth } from './AuthContext'

interface CartContextType {
  cart: Cart | null
  itemCount: number
  loading: boolean
  error: string | null
  pendingItemId: number | null
  clearError: () => void
  addItem: (productId: number, quantity?: number) => Promise<void>
  updateQuantity: (itemId: number, quantity: number) => Promise<void>
  removeItem: (itemId: number) => Promise<void>
  clearCart: () => Promise<void>
  refreshCart: () => Promise<void>
}

const CartContext = createContext<CartContextType>({} as CartContextType)

const GUEST_KEY = 'aura_guest_cart'

interface GuestItem { productId: number; quantity: number }

function readGuestItems(): GuestItem[] {
  try {
    const raw = localStorage.getItem(GUEST_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch { return [] }
}

function writeGuestItems(items: GuestItem[]) {
  if (items.length === 0) localStorage.removeItem(GUEST_KEY)
  else localStorage.setItem(GUEST_KEY, JSON.stringify(items))
}

export function CartProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const [cart, setCart] = useState<Cart | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pendingItemId, setPendingItemId] = useState<number | null>(null)

  const clearError = useCallback(() => setError(null), [])

  const buildGuestCart = useCallback(async (items: GuestItem[]): Promise<Cart | null> => {
    if (items.length === 0) return null
    const products = await Promise.all(items.map(i => productService.getById(i.productId).catch(() => null)))
    const cartItems: CartItem[] = items.map((i, idx) => {
      const p = products[idx]
      if (!p) return null as any
      return {
        id: i.productId,
        productId: i.productId,
        productName: p.name,
        productSlug: p.slug,
        productImage: p.images?.[0]?.url,
        productPrice: p.price,
        productStock: p.stock,
        quantity: i.quantity,
        subtotal: p.price * i.quantity,
      }
    }).filter(Boolean)
    const subtotal = cartItems.reduce((s, ci) => s + ci.subtotal, 0)
    return { id: 0, items: cartItems, itemCount: cartItems.length, subtotal }
  }, [])

  const refreshGuestCart = useCallback(async () => {
    const items = readGuestItems()
    if (items.length === 0) { setCart(null); setLoading(false); return }
    setLoading(true)
    try {
      const c = await buildGuestCart(items)
      setCart(c)
    } catch { setCart(null) }
    finally { setLoading(false) }
  }, [buildGuestCart])

  const refreshCart = useCallback(async () => {
    if (!isAuthenticated) { await refreshGuestCart(); return }
    try {
      setLoading(true)
      const data = await cartService.getCart()
      setCart(data)
    } catch { setCart(null) }
    finally { setLoading(false) }
  }, [isAuthenticated, refreshGuestCart])

  useEffect(() => { refreshCart() }, [refreshCart])

  const runMutation = useCallback(async (
    key: number,
    action: () => Promise<Cart | void>,
    fallbackMessage: string,
  ) => {
    setError(null)
    setPendingItemId(key)
    try {
      const data = await action()
      if (data) setCart(data)
    } catch (err) {
      setError(getErrorMessage(err, fallbackMessage))
      throw err
    } finally {
      setPendingItemId(null)
    }
  }, [])

  const addItem = useCallback((productId: number, quantity = 1) => {
    if (!isAuthenticated) {
      return runMutation(productId, async () => {
        const items = readGuestItems()
        const existing = items.find(i => i.productId === productId)
        if (existing) existing.quantity += quantity
        else items.push({ productId, quantity })
        writeGuestItems(items)
        const c = await buildGuestCart(items)
        setCart(c)
        return undefined
      }, 'No se pudo agregar el producto al carrito')
    }
    return runMutation(productId, () => cartService.addItem(productId, quantity), 'No se pudo agregar el producto al carrito')
  }, [isAuthenticated, runMutation, buildGuestCart])

  const updateQuantity = useCallback((itemId: number, quantity: number) => {
    if (!isAuthenticated) {
      return runMutation(itemId, async () => {
        const items = readGuestItems().map(i => i.productId === itemId ? { ...i, quantity } : i)
        writeGuestItems(items)
        const c = await buildGuestCart(items)
        setCart(c)
        return undefined
      }, 'No se pudo actualizar la cantidad')
    }
    return runMutation(itemId, () => cartService.updateQuantity(itemId, quantity), 'No se pudo actualizar la cantidad')
  }, [isAuthenticated, runMutation, buildGuestCart])

  const removeItem = useCallback((itemId: number) => {
    if (!isAuthenticated) {
      return runMutation(itemId, async () => {
        const items = readGuestItems().filter(i => i.productId !== itemId)
        writeGuestItems(items)
        const c = await buildGuestCart(items)
        setCart(c)
        return undefined
      }, 'No se pudo eliminar el producto')
    }
    return runMutation(itemId, () => cartService.removeItem(itemId), 'No se pudo eliminar el producto')
  }, [isAuthenticated, runMutation, buildGuestCart])

  const clearCart = useCallback(async () => {
    setError(null)
    setPendingItemId(-1)
    try {
      if (!isAuthenticated) {
        writeGuestItems([])
        setCart(null)
      } else {
        await cartService.clearCart()
        setCart(null)
      }
    } catch (err) {
      setError(getErrorMessage(err, 'No se pudo vaciar el carrito'))
      throw err
    } finally {
      setPendingItemId(null)
    }
  }, [isAuthenticated])

  const value = useMemo(
    () => ({ cart, itemCount: cart?.itemCount || 0, loading, error, pendingItemId, clearError, addItem, updateQuantity, removeItem, clearCart, refreshCart }),
    [cart, loading, error, pendingItemId, clearError, addItem, updateQuantity, removeItem, clearCart, refreshCart]
  )

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  )
}

export const useCart = () => useContext(CartContext)
