import { useEffect } from 'react'
import { isAuthenticated, login } from '../../api/auth'

export function LoginPage() {
  useEffect(() => {
    void isAuthenticated().then((authenticated) => {
      if (authenticated) window.location.replace('/')
      else void login()
    })
  }, [])

  return (
    <div className="grid min-h-screen place-items-center bg-background">
      <div className="text-center">
        <p className="mb-4 text-[11px] uppercase tracking-wider text-muted-foreground">Redirecting to Keycloak…</p>
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-border border-t-primary" />
      </div>
    </div>
  )
}
