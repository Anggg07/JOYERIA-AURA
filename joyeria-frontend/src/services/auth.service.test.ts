import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { authService } from './auth.service'
import api from './api'

vi.mock('./api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const mockedApi = vi.mocked(api, { deep: true })

describe('authService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('login posts credentials and returns the auth payload', async () => {
    mockedApi.post.mockResolvedValue({
      data: { success: true, data: { token: 't', id: 1, email: 'a@b.co', fullName: 'Ana Diaz', role: 'USER' } },
    })

    const res = await authService.login('a@b.co', 'secret123')

    expect(mockedApi.post).toHaveBeenCalledWith('/auth/login', { email: 'a@b.co', password: 'secret123' })
    expect(res.token).toBe('t')
    expect(res.role).toBe('USER')
  })

  it('register sends all user fields', async () => {
    mockedApi.post.mockResolvedValue({
      data: { success: true, data: { token: 't', id: 2, email: 'n@b.co', fullName: 'Nora Lee', role: 'USER' } },
    })

    await authService.register({
      firstName: 'Nora', lastName: 'Lee', email: 'n@b.co', phone: '3000000000', password: 'abc12345', confirmPassword: 'abc12345',
    })

    expect(mockedApi.post).toHaveBeenCalledWith('/auth/register', expect.objectContaining({ email: 'n@b.co' }))
  })

  it('getMe returns the user', async () => {
    mockedApi.get.mockResolvedValue({ data: { success: true, data: { id: 3, email: 'm@b.co' } } })

    const me = await authService.getMe()

    expect(mockedApi.get).toHaveBeenCalledWith('/auth/me')
    expect(me.id).toBe(3)
  })

  it('forgotPassword returns the server message', async () => {
    mockedApi.post.mockResolvedValue({ data: { success: true, message: 'Si el email existe…' } })

    const msg = await authService.forgotPassword('a@b.co')

    expect(msg).toBe('Si el email existe…')
  })

  it('logout posts the refresh token through axios', async () => {
    const postSpy = vi.spyOn(axios, 'post').mockResolvedValue({ data: {} })

    await authService.logout('refresh-token')

    expect(postSpy).toHaveBeenCalledWith(
      '/api/auth/logout',
      { refreshToken: 'refresh-token' },
      expect.objectContaining({ headers: { 'Content-Type': 'application/json' } }),
    )
    postSpy.mockRestore()
  })
})