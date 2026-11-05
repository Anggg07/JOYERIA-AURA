import api from './api'
import type { ApiResponse, Material } from '../types'

export const materialService = {
  async getAll(): Promise<Material[]> {
    const { data } = await api.get<ApiResponse<Material[]>>('/materials')
    return data.data
  },
}