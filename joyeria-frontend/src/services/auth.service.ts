import axios from 'axios'
import api from './api'
import type { ApiResponse, AuthResponse, User } from '../types'

const baseURL = import.meta.env.VITE_API_URL || '/api'

export const authService = {
  async login(email: string, password: string): Promise<AuthResponse> {
    const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password })
    return data.data
  },

  async register(userData: {
    firstName: string; lastName: string; email: string;
    phone: string; password: string; confirmPassword: string
  }): Promise<AuthResponse> {
    const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/register', userData)
    return data.data
  },

  async getMe(): Promise<User> {
    const { data } = await api.get<ApiResponse<User>>('/auth/me')
    return data.data
  },

  async forgotPassword(email: string): Promise<string> {
    const { data } = await api.post<ApiResponse<null>>('/auth/forgot-password', { email })
    return data.message || 'Solicitud enviada'
  },

  async resetPassword(token: string, newPassword: string): Promise<string> {
    const { data } = await api.post<ApiResponse<null>>('/auth/reset-password', { token, newPassword })
    return data.message || 'Contraseña actualizada'
  },

  logout(refreshToken: string): Promise<void> {
    return axios.post(`${baseURL}/auth/logout`, { refreshToken }, {
      headers: { 'Content-Type': 'application/json' },
    }).then(() => undefined)
  },
}