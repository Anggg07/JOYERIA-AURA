import { beforeEach, describe, expect, it, vi } from 'vitest'
import { giftService } from './gift.service'
import api from './api'

vi.mock('./api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const mockedApi = vi.mocked(api, { deep: true })

describe('giftService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('posts the criteria and returns the recommendations payload', async () => {
    const payload = {
      recommendations: [
        { id: 1, name: 'Anillo Aurora', slug: 'anillo-aurora', price: 185000, available: true, relevance: 90 },
      ],
    }
    mockedApi.post.mockResolvedValue({ data: { success: true, data: payload } })

    const res = await giftService.recommend({ recipient: 'pareja', budgetMax: 200000 })

    expect(mockedApi.post).toHaveBeenCalledWith('/gift-finder/recommend', {
      recipient: 'pareja',
      budgetMax: 200000,
    })
    expect(res.recommendations).toHaveLength(1)
    expect(res.recommendations[0].name).toBe('Anillo Aurora')
  })
})
