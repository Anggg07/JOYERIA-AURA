import api from './api'
import type { ApiResponse, Order, PagedResponse } from '../types'

export interface OrderCreateRequest {
  shippingAddress: string
  shippingCity: string
  shippingDepartment?: string
  recipientName?: string
  phone?: string
  paymentMethod: string
  couponCode?: string
  notes?: string
}

export interface GuestOrderItemRequest {
  productId: number
  quantity: number
}

export interface GuestOrderRequest {
  email: string
  shippingAddress: string
  shippingCity: string
  shippingDepartment?: string
  recipientName?: string
  phone?: string
  paymentMethod: string
  couponCode?: string
  notes?: string
  items: GuestOrderItemRequest[]
}

export const orderService = {
  async createOrder(orderData: OrderCreateRequest): Promise<Order> {
    const { data } = await api.post<ApiResponse<Order>>('/orders', orderData)
    return data.data
  },

  async createGuestOrder(orderData: GuestOrderRequest): Promise<Order> {
    const { data } = await api.post<ApiResponse<Order>>('/orders/guest', orderData)
    return data.data
  },

  async getGuestOrder(orderNumber: string, token: string): Promise<Order> {
    const { data } = await api.get<ApiResponse<Order>>(`/orders/guest/${orderNumber}`, { params: { token } })
    return data.data
  },

  async getUserOrders(page = 0, size = 10): Promise<PagedResponse<Order>> {
    const { data } = await api.get<ApiResponse<PagedResponse<Order>>>('/orders', { params: { page, size } })
    return data.data
  },

  async getOrderById(id: number): Promise<Order> {
    const { data } = await api.get<ApiResponse<Order>>(`/orders/${id}`)
    return data.data
  },

  async getByOrderNumber(orderNumber: string): Promise<Order> {
    const { data } = await api.get<ApiResponse<Order>>(`/orders/number/${orderNumber}`)
    return data.data
  },

  async getAllOrders(page = 0, size = 20, status?: string): Promise<PagedResponse<Order>> {
    const url = status ? `/admin/orders/status/${status}` : '/admin/orders'
    const { data } = await api.get<ApiResponse<PagedResponse<Order>>>(url, { params: { page, size } })
    return data.data
  },

  async getAdminOrder(id: number): Promise<Order> {
    const { data } = await api.get<ApiResponse<Order>>(`/admin/orders/${id}`)
    return data.data
  },

  async updateStatus(id: number, status: string): Promise<Order> {
    const { data } = await api.put<ApiResponse<Order>>(`/admin/orders/${id}/status`, null, { params: { status } })
    return data.data
  },
}
