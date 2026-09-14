import { UserManager, type User } from 'oidc-client-ts'

const clientId = import.meta.env.VITE_OAUTH_CLIENT_ID ?? 'llm-router-react-admin'
const authority = import.meta.env.VITE_OAUTH_AUTH_SERVER ?? 'https://auth.arorms.cn/realms/arorms'
const redirectUri = import.meta.env.VITE_OAUTH_REDIRECT_URI ?? `${window.location.origin}/callback`

const OIDC_CONFIG = {
  authority,
  client_id: clientId,
  redirect_uri: redirectUri,
  post_logout_redirect_uri: window.location.origin + '/login',
  response_type: 'code',
  scope: 'openid profile email',
  automaticSilentRenew: true,
}

let userManager: UserManager | null = null

export const getUserManager = (): UserManager => {
  if (!userManager) userManager = new UserManager(OIDC_CONFIG)
  return userManager
}

export const login = (): Promise<void> => getUserManager().signinRedirect()

export const logout = async (): Promise<void> => {
  try {
    await getUserManager().signoutRedirect()
  } finally {
    await getUserManager().removeUser()
  }
}

export const handleCallback = async (): Promise<User | null> => {
  try {
    return await getUserManager().signinRedirectCallback()
  } catch {
    return null
  }
}

export const getAccessToken = async (): Promise<string | null> => {
  try {
    const user = await getUserManager().getUser()
    return user?.expired === false ? user.access_token : null
  } catch {
    return null
  }
}

export const isAuthenticated = async (): Promise<boolean> => {
  try {
    const user = await getUserManager().getUser()
    return !!user && !user.expired
  } catch {
    return false
  }
}

export interface AuthUser {
  id: string
  username: string
  email: string
}

export const getUserInfo = async (): Promise<AuthUser | null> => {
  try {
    const user = await getUserManager().getUser()
    if (!user || user.expired) return null
    return {
      id: user.profile.sub,
      username: user.profile.preferred_username ?? user.profile.sub,
      email: user.profile.email ?? '',
    }
  } catch {
    return null
  }
}

export const fetchMe = async (): Promise<AuthUser | null> => {
  const accessToken = await getAccessToken()
  if (!accessToken) return null
  const response = await fetch('/api/v1/auth/me', {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!response.ok) return null
  return response.json()
}
