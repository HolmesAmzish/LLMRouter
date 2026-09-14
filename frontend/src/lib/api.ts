const jsonHeaders = { 'Content-Type': 'application/json' }

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
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
