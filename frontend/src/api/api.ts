import { getAccessToken, getUserManager, isAuthenticated } from './auth'

const jsonHeaders = { 'Content-Type': 'application/json' }

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  Object.entries(jsonHeaders).forEach(([name, value]) => headers.set(name, value))

  const accessToken = await getAccessToken()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)

  const response = await fetch(url, { ...init, headers })
  if (response.status === 401) {
    const authenticated = await isAuthenticated()
    // A valid token that is rejected by the backend is an authorization/token
    // configuration problem. Redirecting again would create a login loop.
    if (!authenticated) void getUserManager().signinRedirect()
    throw new Error(authenticated ? 'Backend rejected the access token' : 'Authentication required')
  }

  const text = await response.text()
  const body = text ? JSON.parse(text) : null
  if (!response.ok) {
    throw new Error(body?.message ?? body?.error?.message ?? text ?? `Request failed: ${response.status}`)
  }
  return body as T
}

export const api = {
  get: <T>(url: string) => request<T>(url),
  post: <T>(url: string, data?: unknown) => request<T>(url, { method: 'POST', headers: jsonHeaders, body: data ? JSON.stringify(data) : undefined }),
  patch: <T>(url: string, data: unknown) => request<T>(url, { method: 'PATCH', headers: jsonHeaders, body: JSON.stringify(data) }),
  delete: <T>(url: string) => request<T>(url, { method: 'DELETE' }),
}
