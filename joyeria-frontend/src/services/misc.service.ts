import api from './api'
import type { ApiResponse, SocialLink, CheckoutResponse, PaymentStatus } from '../types'

export const socialService = {
  async getAll(): Promise<SocialLink[]> {
    const { data } = await api.get<ApiResponse<SocialLink[]>>('/social-links')
    return data.data
  },
}

export const newsletterService = {
  async subscribe(email: string): Promise<string> {
    const { data } = await api.post<ApiResponse<string>>('/newsletter/subscribe', { email })
    return data.message || data.data
  },
}

export const paymentService = {
  async createCheckout(payload: { orderId: number; paymentMethod: string; guestToken?: string }): Promise<CheckoutResponse> {
    const { data } = await api.post<ApiResponse<CheckoutResponse>>('/payments/checkout', payload)
    return data.data
  },

  async getStatus(orderNumber: string, guestToken?: string): Promise<PaymentStatus> {
    const { data } = await api.get<ApiResponse<PaymentStatus>>(`/payments/status/${orderNumber}`, { params: guestToken ? { guestToken } : undefined })
    return data.data
  },

  async confirmSandbox(reference: string, guestToken?: string): Promise<PaymentStatus> {
    const { data } = await api.post<ApiResponse<PaymentStatus>>('/payments/sandbox/confirm', { reference, guestToken })
    return data.data
  },
}
