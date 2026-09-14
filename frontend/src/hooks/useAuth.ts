import { useCallback, useEffect, useState } from 'react'
import { isAuthenticated, login, logout, getUserInfo, type AuthUser } from '../api/auth'

export function useAuth() {
  const [state, setState] = useState<{ loading: boolean; user: AuthUser | null }>({
    loading: true,
    user: null,
  })

  useEffect(() => {
    let mounted = true
    void isAuthenticated().then(async (authenticated) => {
      const user = authenticated ? await getUserInfo() : null
      if (mounted) setState({ loading: false, user })
    })
    return () => { mounted = false }
  }, [])

  const handleLogin = useCallback(() => void login(), [])
  const handleLogout = useCallback(() => void logout(), [])

  return { ...state, login: handleLogin, logout: handleLogout }
}
