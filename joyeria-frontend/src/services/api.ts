import axios, { type InternalAxiosRequestConfig } from 'axios'
import { SESSION_EXPIRED_EVENT } from '../utils/sessionEvents'

const baseURL = import.meta.env.VITE_API_URL || '/api'

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

let isRefreshing = false
let pendingQueue: Array<{
  config: RetryableConfig
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
}> = []

function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
}

function broadcastSessionExpired() {
  if (window.location.pathname !== '/login') {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
  }
}

const REFRESH_EXCLUDED = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout']

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as RetryableConfig | undefined
    const url: string = original?.url || ''
    const skipRefresh = REFRESH_EXCLUDED.some((path) => url.includes(path))
    const refreshToken = localStorage.getItem('refreshToken')

    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !skipRefresh &&
      refreshToken
    ) {
      original._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push({ config: original, resolve, reject })
        })
      }

      isRefreshing = true
      try {
        const { data } = await axios.post<{ data?: { token?: string; refreshToken?: string } }>(
          `${baseURL}/auth/refresh`,
          { refreshToken }
        )
        const next = data.data
        if (!next?.token) {
          throw new Error('Refresh de sesión fallido')
        }
        localStorage.setItem('token', next.token)
        if (next.refreshToken) {
          localStorage.setItem('refreshToken', next.refreshToken)
        }
        const queue = pendingQueue
        pendingQueue = []
        queue.forEach((entry) => {
          entry.config.headers.Authorization = `Bearer ${next.token}`
          entry.resolve(api(entry.config))
        })
        return api(original)
      } catch {
        const queue = pendingQueue
        pendingQueue = []
        queue.forEach((entry) => entry.reject(new Error('Sesión expirada')))
        clearSession()
        broadcastSessionExpired()
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response?.status === 401) {
      clearSession()
      broadcastSessionExpired()
    }
    return Promise.reject(error)
  }
)

export default api
