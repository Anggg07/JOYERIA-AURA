import api from './api'
import type { ApiResponse, Cart, CheckoutSummary } from '../types'

export const cartService = {
  async getCart(): Promise<Cart> {
    const { data } = await api.get<ApiResponse<Cart>>('/cart')
    return data.data
  },

  async getSummary(coupon?: string): Promise<CheckoutSummary> {
    const { data } = await api.get<ApiResponse<CheckoutSummary>>('/cart/summary', {
      params: coupon ? { coupon } : {},
    })
    return data.data
  },

  async addItem(productId: number, quantity: number = 1): Promise<Cart> {
    const { data } = await api.post<ApiResponse<Cart>>('/cart/items', { productId, quantity })
    return data.data
  },

  async updateQuantity(itemId: number, quantity: number): Promise<Cart> {
    const { data } = await api.put<ApiResponse<Cart>>(`/cart/items/${itemId}`, null, { params: { quantity } })
    return data.data
  },

  async removeItem(itemId: number): Promise<Cart> {
    const { data } = await api.delete<ApiResponse<Cart>>(`/cart/items/${itemId}`)
    return data.data
  },

  async clearCart(): Promise<void> {
    await api.delete('/cart')
  },
}