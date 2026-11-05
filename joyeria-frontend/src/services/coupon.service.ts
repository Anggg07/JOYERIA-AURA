import api from './api'
import type { ApiResponse, Coupon } from '../types'

export interface CouponCreateRequest {
  code: string
  description?: string
  discountType: 'PERCENTAGE' | 'FIXED'
  discountValue: number
  minAmount?: number | null
  maxUses?: number | null
  validFrom?: string | null
  validUntil?: string | null
}

export const couponService = {
  async validate(code: string): Promise<Coupon> {
    const { data } = await api.post<ApiResponse<Coupon>>(`/coupons/validate`, null, { params: { code } })
    return data.data
  },

  async getAll(): Promise<Coupon[]> {
    const { data } = await api.get<ApiResponse<Coupon[]>>('/coupons')
    return data.data
  },

  async create(coupon: CouponCreateRequest): Promise<Coupon> {
    const { data } = await api.post<ApiResponse<Coupon>>('/coupons', coupon)
    return data.data
  },

  async update(id: number, coupon: CouponCreateRequest): Promise<Coupon> {
    const { data } = await api.put<ApiResponse<Coupon>>(`/coupons/${id}`, coupon)
    return data.data
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/coupons/${id}`)
  },
}
