import api from './api'
import type { ApiResponse, Category } from '../types'

export const categoryService = {
  async getAll(): Promise<Category[]> {
    const { data } = await api.get<ApiResponse<Category[]>>('/categories')
    return data.data
  },

  async getById(id: number): Promise<Category> {
    const { data } = await api.get<ApiResponse<Category>>(`/categories/${id}`)
    return data.data
  },
}