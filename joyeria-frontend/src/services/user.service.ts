import api from './api'
import type { ApiResponse, User, Address, Review, SupportTicket, PagedResponse, Product } from '../types'

export interface UserUpdateRequest {
  firstName?: string
  lastName?: string
  email?: string
  phone?: string
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface AddressCreateRequest {
  firstName?: string
  lastName?: string
  phone?: string
  addressLine1: string
  addressLine2?: string
  city: string
  department?: string
  postalCode?: string
  addressType: 'SHIPPING' | 'BILLING'
  isDefault: boolean
}

export interface ReviewCreateRequest {
  productId: number
  rating: number
  title?: string
  comment?: string
}

export interface SupportTicketCreateRequest {
  subject: string
  message: string
  category?: string
}

export const userService = {
  async getProfile(): Promise<User> {
    const { data } = await api.get<ApiResponse<User>>('/users/me')
    return data.data
  },

  async updateProfile(userData: UserUpdateRequest): Promise<User> {
    const { data } = await api.put<ApiResponse<User>>('/users/me', userData)
    return data.data
  },

  async changePassword(passwords: PasswordChangeRequest): Promise<void> {
    await api.put('/users/me/password', passwords)
  },

  async getAddresses(): Promise<Address[]> {
    const { data } = await api.get<ApiResponse<Address[]>>('/addresses')
    return data.data
  },

  async createAddress(address: AddressCreateRequest): Promise<Address> {
    const { data } = await api.post<ApiResponse<Address>>('/addresses', address)
    return data.data
  },

  async updateAddress(id: number, address: AddressCreateRequest): Promise<Address> {
    const { data } = await api.put<ApiResponse<Address>>(`/addresses/${id}`, address)
    return data.data
  },

  async deleteAddress(id: number): Promise<void> {
    await api.delete(`/addresses/${id}`)
  },

  async toggleFavorite(productId: number): Promise<boolean> {
    const { data } = await api.post<ApiResponse<boolean>>(`/favorites/toggle/${productId}`)
    return data.data
  },

  async isFavorite(productId: number, signal?: AbortSignal): Promise<boolean> {
    const { data } = await api.get<ApiResponse<boolean>>(`/favorites/check/${productId}`, { signal })
    return data.data
  },

  async getFavorites(page = 0, size = 20): Promise<PagedResponse<Product>> {
    const { data } = await api.get<ApiResponse<PagedResponse<Product>>>('/favorites', { params: { page, size } })
    return data.data
  },

  async submitReview(review: ReviewCreateRequest): Promise<Review> {
    const { data } = await api.post<ApiResponse<Review>>('/reviews', review)
    return data.data
  },

  async getProductReviews(productId: number, page = 0, size = 10, signal?: AbortSignal): Promise<PagedResponse<Review>> {
    const { data } = await api.get<ApiResponse<PagedResponse<Review>>>(`/reviews/product/${productId}`, { params: { page, size }, signal })
    return data.data
  },

  async getRecentReviews(): Promise<Review[]> {
    const { data } = await api.get<ApiResponse<Review[]>>('/reviews/recent')
    return data.data
  },

  async createSupportTicket(ticket: SupportTicketCreateRequest): Promise<SupportTicket> {
    const { data } = await api.post<ApiResponse<SupportTicket>>('/support/tickets', ticket)
    return data.data
  },

  async getSupportTickets(page = 0, size = 10): Promise<PagedResponse<SupportTicket>> {
    const { data } = await api.get<ApiResponse<PagedResponse<SupportTicket>>>('/support/tickets', { params: { page, size } })
    return data.data
  },

  async getSupportTicket(ticketNumber: string): Promise<SupportTicket> {
    const { data } = await api.get<ApiResponse<SupportTicket>>(`/support/tickets/${ticketNumber}`)
    return data.data
  },

  async replyToSupportTicket(ticketNumber: string, message: string): Promise<SupportTicket> {
    const { data } = await api.post<ApiResponse<SupportTicket>>(`/support/tickets/${ticketNumber}/responses`, { message })
    return data.data
  },
}
