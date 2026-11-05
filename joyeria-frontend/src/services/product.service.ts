import api from './api'
import type { ApiResponse, Product, PagedResponse } from '../types'

export interface ProductSearchParams {
  page?: number
  size?: number
  category?: string
  material?: string
  minPrice?: number
  maxPrice?: number
  search?: string
  sort?: string
}

export const productService = {
  async getAll(params: ProductSearchParams = {}, signal?: AbortSignal): Promise<PagedResponse<Product>> {
    const { data } = await api.get<ApiResponse<PagedResponse<Product>>>('/products', { params, signal })
    return data.data
  },

  async getById(id: number): Promise<Product> {
    const { data } = await api.get<ApiResponse<Product>>(`/products/${id}`)
    return data.data
  },

  async getBySlug(slug: string, signal?: AbortSignal): Promise<Product> {
    const { data } = await api.get<ApiResponse<Product>>(`/products/slug/${slug}`, { signal })
    return data.data
  },

  async getFeatured(): Promise<Product[]> {
    const { data } = await api.get<ApiResponse<Product[]>>('/products/featured')
    return data.data
  },

  async getNew(): Promise<Product[]> {
    const { data } = await api.get<ApiResponse<Product[]>>('/products/new')
    return data.data
  },

  async getBestSellers(): Promise<Product[]> {
    const { data } = await api.get<ApiResponse<Product[]>>('/products/best-sellers')
    return data.data
  },

  async getRelated(id: number, signal?: AbortSignal): Promise<Product[]> {
    const { data } = await api.get<ApiResponse<Product[]>>(`/products/${id}/related`, { signal })
    return data.data
  },

  async search(query: string, params: ProductSearchParams = {}, signal?: AbortSignal): Promise<PagedResponse<Product>> {
    const { data } = await api.get<ApiResponse<PagedResponse<Product>>>('/search', { params: { q: query, ...params }, signal })
    return data.data
  },
}
