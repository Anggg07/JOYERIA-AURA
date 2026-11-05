import api from './api'
import type { ApiResponse, GiftFinderRequest, GiftFinderResponse } from '../types'

/**
 * Smart Gift Finder. Envía los criterios del usuario (todos opcionales) y
 * devuelve productos reales del catálogo ordenados por relevancia. El backend
 * nunca inventa productos ni precios.
 */
export const giftService = {
  async recommend(criteria: GiftFinderRequest): Promise<GiftFinderResponse> {
    const { data } = await api.post<ApiResponse<GiftFinderResponse>>('/gift-finder/recommend', criteria)
    return data.data
  },
}
