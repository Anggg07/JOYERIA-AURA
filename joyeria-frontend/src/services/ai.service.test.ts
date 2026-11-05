import { beforeEach, describe, expect, it, vi } from 'vitest'
import { aiService } from './ai.service'
import api from './api'

vi.mock('./api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const mockedApi = vi.mocked(api, { deep: true })

describe('aiService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('posts the message and returns the assistant payload', async () => {
    const payload = {
      message: 'Te muestro algunas opciones.',
      products: [{ id: 1, name: 'Anillo Aurora', price: 185000, available: true }],
    }
    mockedApi.post.mockResolvedValue({ data: { success: true, data: payload } })

    const res = await aiService.ask('busco un regalo')

    expect(mockedApi.post).toHaveBeenCalledWith('/ai/assistant', { message: 'busco un regalo', history: [] })
    expect(res.message).toBe('Te muestro algunas opciones.')
    expect(res.products).toHaveLength(1)
  })
})
