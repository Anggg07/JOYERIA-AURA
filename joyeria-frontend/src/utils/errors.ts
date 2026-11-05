interface ApiErrorPayload {
  message?: string
  data?: unknown
}

export function getErrorMessage(error: unknown, fallback = 'Ocurrió un error'): string {
  if (error && typeof error === 'object' && 'response' in error) {
    const payload = (error as { response?: { data?: ApiErrorPayload } }).response?.data
    if (payload?.message) return payload.message
    if (typeof payload?.data === 'string' && payload.data) return payload.data
  }
  return fallback
}
