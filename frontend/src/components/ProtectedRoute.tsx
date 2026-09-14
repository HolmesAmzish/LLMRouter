import { useEffect, useState, type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { isAuthenticated } from '../api/auth'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const [authenticated, setAuthenticated] = useState<boolean | null>(null)

  useEffect(() => {
    let mounted = true
    isAuthenticated().then((value) => mounted && setAuthenticated(value))
    return () => { mounted = false }
  }, [])

  if (authenticated === null) {
    return (
      <div className="grid min-h-screen place-items-center bg-background">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-border border-t-primary" />
      </div>
    )
  }

  if (!authenticated) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}
