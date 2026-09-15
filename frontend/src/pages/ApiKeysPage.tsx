import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { ApiKey } from '../types'
import { Alert, Badge, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

export default function ApiKeysPage() {
  const [keys, setKeys] = useState<ApiKey[]>([])
  const [draft, setDraft] = useState({ name: '', expiresAt: '' })
  const [createdKey, setCreatedKey] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setKeys(await api.get<ApiKey[]>('/api/v1/api-keys'))
  }, [])

  useEffect(() => { void load() }, [load])

  const create = async () => {
    setBusy(true); setError('')
    try {
      const result = await api.post<ApiKey>('/api/v1/api-keys', {
        name: draft.name,
        expiresAt: draft.expiresAt ? new Date(draft.expiresAt).toISOString() : undefined,
      })
      setCreatedKey(result.apiKey ?? '')
      setDraft({ name: '', expiresAt: '' })
      await load()
    } catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  const mutate = async (action: () => Promise<unknown>) => {
    setError(''); setBusy(true)
    try { await action(); await load() }
    catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
      <Card>
        <CardHeader title="Gateway API keys" description="External clients authenticate with X-Api-Key. Keys are stored hashed." action={<Button onClick={() => void load()}>Refresh</Button>} />
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        {keys.length === 0 ? <EmptyState title="No API keys" description="Issue a key before calling the gateway." /> : (
          <div className="divide-y divide-border">
            {keys.map((key) => (
              <article key={key.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-medium">{key.name}</h3>
                    {key.enabled ? <Badge tone="success">enabled</Badge> : <Badge tone="danger">revoked</Badge>}
                    {key.expiresAt && <Badge tone="warning">expires</Badge>}
                  </div>
                  <p className="mt-1 font-mono text-xs text-muted-foreground">{key.prefix}…</p>
                  <p className="text-xs text-muted-foreground">Last used: {key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleString() : 'never'}</p>
                </div>
                <div className="flex gap-2">
                  {key.enabled && <Button onClick={() => void mutate(() => api.post(`/api/v1/api-keys/${key.id}/revoke`))}>Revoke</Button>}
                  <Button variant="danger" onClick={() => void mutate(() => api.delete(`/api/v1/api-keys/${key.id}`))}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>

      <div className="grid gap-4">
        <Card>
          <CardHeader title="Issue key" />
          <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void create() }}>
            <Field label="Name"><input className={inputClass} required value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} /></Field>
            <Field label="Expires at"><input className={inputClass} type="datetime-local" value={draft.expiresAt} onChange={(event) => setDraft({ ...draft, expiresAt: event.target.value })} /></Field>
            <Button type="submit" variant="primary" disabled={busy}>Create key</Button>
          </form>
        </Card>
        {createdKey && (
          <Card>
            <CardHeader title="Copy your key" description="This value will never be shown again." />
            <div className="p-4">
              <textarea className={`${inputClass} h-28 resize-none font-mono`} readOnly value={createdKey} />
              <Button className="mt-2" onClick={() => void navigator.clipboard.writeText(createdKey)}>Copy</Button>
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}
