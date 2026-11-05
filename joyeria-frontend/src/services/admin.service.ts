import api from './api'
import type {
  ApiResponse, User, Product, Category, Material, Review,
  PagedResponse, DashboardStats, SocialLink, SupportTicket,
} from '../types'

export interface ProductCreateRequest {
  name: string
  slug?: string
  description?: string
  price: number
  comparePrice?: number | null
  sku: string
  stock: number
  weight?: string
  dimensions?: string
  size?: string
  color?: string
  careInstructions?: string
  features?: string
  deliveryTime?: string
  featured?: boolean
  isNew?: boolean
  bestSeller?: boolean
  active?: boolean
  categoryId?: number | null
  materialIds?: number[]
  imageUrls?: string[]
}

export interface CategoryCreateRequest {
  name: string
  slug?: string
  description?: string
  displayOrder?: number
  image?: string
}

export interface MaterialCreateRequest {
  name: string
  slug?: string
  description?: string
  image?: string
}

export interface SocialLinkCreateRequest {
  name: string
  url: string
  icon?: string
}

export const adminService = {
  // Dashboard
  async getStats(): Promise<DashboardStats> {
    const { data } = await api.get<ApiResponse<DashboardStats>>('/admin/orders/stats')
    return data.data
  },

  // Products
  async createProduct(product: ProductCreateRequest): Promise<Product> {
    const { data } = await api.post<ApiResponse<Product>>('/admin/products', product)
    return data.data
  },
  async updateProduct(id: number, product: ProductCreateRequest): Promise<Product> {
    const { data } = await api.put<ApiResponse<Product>>(`/admin/products/${id}`, product)
    return data.data
  },
  async deleteProduct(id: number): Promise<void> {
    await api.delete(`/admin/products/${id}`)
  },
  async uploadImage(file: File): Promise<string> {
    const form = new FormData()
    form.append('file', file)
    const { data } = await api.post<ApiResponse<{ url: string }>>('/admin/uploads', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data.data.url
  },

  // Categories
  async createCategory(category: CategoryCreateRequest): Promise<Category> {
    const { data } = await api.post<ApiResponse<Category>>('/categories', category)
    return data.data
  },
  async updateCategory(id: number, category: CategoryCreateRequest): Promise<Category> {
    const { data } = await api.put<ApiResponse<Category>>(`/categories/${id}`, category)
    return data.data
  },
  async deleteCategory(id: number): Promise<void> {
    await api.delete(`/categories/${id}`)
  },

  // Materials
  async createMaterial(material: MaterialCreateRequest): Promise<Material> {
    const { data } = await api.post<ApiResponse<Material>>('/materials', material)
    return data.data
  },
  async updateMaterial(id: number, material: MaterialCreateRequest): Promise<Material> {
    const { data } = await api.put<ApiResponse<Material>>(`/materials/${id}`, material)
    return data.data
  },
  async deleteMaterial(id: number): Promise<void> {
    await api.delete(`/materials/${id}`)
  },

  // Users
  async getUsers(page = 0, size = 20): Promise<PagedResponse<User>> {
    const { data } = await api.get<ApiResponse<PagedResponse<User>>>('/admin/users', { params: { page, size } })
    return data.data
  },
  async updateUserRole(id: number, role: string): Promise<User> {
    const { data } = await api.put<ApiResponse<User>>(`/admin/users/${id}/role`, null, { params: { role } })
    return data.data
  },
  async toggleUserActive(id: number): Promise<User> {
    const { data } = await api.put<ApiResponse<User>>(`/admin/users/${id}/toggle`)
    return data.data
  },

  // Reviews
  async getReviews(page = 0, size = 20, status?: string): Promise<PagedResponse<Review>> {
    const { data } = await api.get<ApiResponse<PagedResponse<Review>>>('/admin/reviews', { params: { page, size, status } })
    return data.data
  },
  async moderateReview(id: number, status: string): Promise<Review> {
    const { data } = await api.put<ApiResponse<Review>>(`/admin/reviews/${id}/status`, null, { params: { status } })
    return data.data
  },

  // Support tickets
  async getTickets(page = 0, size = 20): Promise<PagedResponse<SupportTicket>> {
    const { data } = await api.get<ApiResponse<PagedResponse<SupportTicket>>>('/admin/support/tickets', { params: { page, size } })
    return data.data
  },
  async getTicket(id: number): Promise<SupportTicket> {
    const { data } = await api.get<ApiResponse<SupportTicket>>(`/admin/support/tickets/${id}`)
    return data.data
  },
  async replyToTicket(id: number, message: string): Promise<SupportTicket> {
    const { data } = await api.post<ApiResponse<SupportTicket>>(`/admin/support/tickets/${id}/responses`, { message })
    return data.data
  },
  async updateTicketStatus(id: number, status: string): Promise<SupportTicket> {
    const { data } = await api.put<ApiResponse<SupportTicket>>(`/admin/support/tickets/${id}/status`, null, { params: { status } })
    return data.data
  },

  // Social links
  async getSocialLinks(): Promise<SocialLink[]> {
    const { data } = await api.get<ApiResponse<SocialLink[]>>('/social-links/admin')
    return data.data
  },
  async createSocialLink(link: SocialLinkCreateRequest): Promise<SocialLink> {
    const { data } = await api.post<ApiResponse<SocialLink>>('/social-links', link)
    return data.data
  },
  async updateSocialLink(id: number, link: SocialLinkCreateRequest): Promise<SocialLink> {
    const { data } = await api.put<ApiResponse<SocialLink>>(`/social-links/${id}`, link)
    return data.data
  },
  async deleteSocialLink(id: number): Promise<void> {
    await api.delete(`/social-links/${id}`)
  },
}
