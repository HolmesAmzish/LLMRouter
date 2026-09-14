import { useEffect, useRef, useState } from 'react'
import { handleCallback, isAuthenticated } from '../../api/auth'

export function CallbackPage() {
  const exchanged = useRef(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (exchanged.current) return
    exchanged.current = true
    void handleCallback().then(async () => {
      const authenticated = await isAuthenticated()
      if (authenticated) window.location.replace('/')
      else setError('Login could not be completed')
    })
  }, [])

  return (
    <div className="grid min-h-screen place-items-center bg-background">
      <div className="text-center">
        <p className="mb-4 text-[11px] uppercase tracking-wider text-muted-foreground">
          {error ? error : 'Completing login…'}
        </p>
        {!error && <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-border border-t-primary" />}
      </div>
    </div>
  )
}
