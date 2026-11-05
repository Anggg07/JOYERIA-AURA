import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import Loader from '../components/Loader'

export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, loading } = useAuth()
  if (loading) return <Loader fullScreen />
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>
}
