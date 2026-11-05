import { createContext, useContext, useState, useEffect, useCallback, useMemo, type ReactNode } from 'react'
import type { User } from '../types'
import { authService } from '../services/auth.service'

interface RegisterData {
  firstName: string
  lastName: string
  email: string
  phone: string
  password: string
  confirmPassword: string
}

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isAdmin: boolean
  loading: boolean
  authLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (userData: RegisterData) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextType>({} as AuthContextType)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [authLoading, setAuthLoading] = useState(false)

  const loadUser = useCallback(async () => {
    const token = localStorage.getItem('token')
    if (!token) {
      setLoading(false)
      return
    }
    try {
      const userData = await authService.getMe()
      setUser(userData)
    } catch {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadUser()
  }, [loadUser])

  const login = useCallback(async (email: string, password: string) => {
    setAuthLoading(true)
    try {
      const res = await authService.login(email, password)
      localStorage.setItem('token', res.token)
      if (res.refreshToken) localStorage.setItem('refreshToken', res.refreshToken)
      setUser({ id: res.id, email: res.email, firstName: res.fullName.split(' ')[0],
        lastName: res.fullName.split(' ').slice(1).join(' '),
        role: res.role as 'USER' | 'ADMIN', active: true } as User)
    } finally {
      setAuthLoading(false)
    }
  }, [])

  const register = useCallback(async (userData: RegisterData) => {
    setAuthLoading(true)
    try {
      const res = await authService.register(userData)
      localStorage.setItem('token', res.token)
      if (res.refreshToken) localStorage.setItem('refreshToken', res.refreshToken)
      setUser({ id: res.id, email: res.email, firstName: res.fullName.split(' ')[0],
        lastName: res.fullName.split(' ').slice(1).join(' '),
        role: res.role as 'USER' | 'ADMIN', active: true } as User)
    } finally {
      setAuthLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      authService.logout(refreshToken).catch(() => undefined)
    }
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, isAuthenticated: !!user, isAdmin: user?.role === 'ADMIN', loading, authLoading, login, register, logout, refreshUser: loadUser }),
    [user, loading, authLoading, login, register, logout, loadUser]
  )

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)