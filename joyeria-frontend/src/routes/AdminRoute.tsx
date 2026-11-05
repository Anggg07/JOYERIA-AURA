import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import Loader from '../components/Loader'

export default function AdminRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isAdmin, loading } = useAuth()
  if (loading) return <Loader fullScreen />
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!isAdmin) return <Navigate to="/" replace />
  return <>{children}</>
}
