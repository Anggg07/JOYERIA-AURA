import { describe, it, expect } from 'vitest'
import { getErrorMessage } from './errors'

describe('getErrorMessage', () => {
  it('returns fallback for non-api errors', () => {
    expect(getErrorMessage(undefined, 'fallback')).toBe('fallback')
    expect(getErrorMessage(null, 'fallback')).toBe('fallback')
    expect(getErrorMessage('texto', 'fallback')).toBe('fallback')
    expect(getErrorMessage(new Error('boom'), 'fallback')).toBe('fallback')
    expect(getErrorMessage({ code: 1 }, 'fallback')).toBe('fallback')
  })

  it('extracts server message from response.data', () => {
    const error = { response: { data: { message: 'Credenciales inválidas' } } }
    expect(getErrorMessage(error)).toBe('Credenciales inválidas')
  })

  it('extracts a string data payload as message', () => {
    const error = { response: { data: { data: 'Fuera de stock' } } }
    expect(getErrorMessage(error)).toBe('Fuera de stock')
  })

  it('uses message over data when both present', () => {
    const error = { response: { data: { message: 'Con mensaje', data: 'Con data' } } }
    expect(getErrorMessage(error)).toBe('Con mensaje')
  })

  it('falls back when response has no message', () => {
    expect(getErrorMessage({ response: {} }, 'fallback')).toBe('fallback')
    expect(getErrorMessage({}, 'fallback')).toBe('fallback')
  })
})