import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { ModelListResponse, Protocol, ProviderAccount } from '../types'
import { Alert, Badge, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'OPENAI_RESPONSES', 'ANTHROPIC']

type Draft = {
  name: string
  protocolEndpoints: Partial<Record<Protocol, string>>
  apiKey: string
  enabled: boolean
  priority: number
  weight: number
  balanceEndpoint: string
}

const emptyDraft: Draft = {
  name: '',
  protocolEndpoints: {
    OPENAI: 'https://api.openai.com',
    OPENAI_RESPONSES: 'https://api.openai.com',
    ANTHROPIC: '',
  },
  apiKey: '',
  enabled: true,
  priority: 100,
  weight: 100,
  balanceEndpoint: '',
}

export default function AccountsPage({ onChanged }: { onChanged?: () => void }) {
  const [accounts, setAccounts] = useState<ProviderAccount[]>([])
  const [draft, setDraft] = useState<Draft>(emptyDraft)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setAccounts(await api.get<ProviderAccount[]>('/api/v1/accounts'))
  }, [])

  useEffect(() => { void load() }, [load])

  const submit = async () => {
    setBusy(true); setError('')
    try {
      const protocolEndpoints = Object.fromEntries(
        Object.entries(draft.protocolEndpoints).filter(([, url]) => url.trim())
      )
      if (Object.keys(protocolEndpoints).length === 0) throw new Error('Configure at least one protocol endpoint')
      await api.post<ProviderAccount>('/api/v1/accounts', { ...draft, protocolEndpoints })
      setDraft({ ...emptyDraft, name: '', apiKey: '' })
      await load(); onChanged?.()
    } catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  const mutate = async (action: () => Promise<unknown>) => {
    setError(''); setBusy(true)
    try { await action(); await load(); onChanged?.() }
    catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  const syncModels = (account: ProviderAccount) => mutate(() => api.post<ModelListResponse>(`/api/v1/models/accounts/${account.id}/sync`))

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
      <Card>
        <CardHeader title="Provider accounts" description="One account can expose multiple protocol endpoints." action={<Button variant="secondary" onClick={() => void load()}>Refresh</Button>} />
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        {accounts.length === 0 ? <EmptyState title="No accounts" description="Add an upstream provider to start routing requests." /> : (
          <div className="divide-y divide-border">
            {accounts.map((account) => (
              <article key={account.id} className="flex flex-wrap items-start justify-between gap-4 px-4 py-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="truncate text-sm font-medium">{account.name}</h3>
                    {account.enabled ? <Badge tone="success">enabled</Badge> : <Badge tone="warning">disabled</Badge>}
                  </div>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {Object.keys(account.protocolEndpoints).map((protocol) => (
                      <Badge key={protocol} tone="primary">{protocol}</Badge>
                    ))}
                  </div>
                  <div className="mt-2 space-y-1">
                    {Object.entries(account.protocolEndpoints).map(([protocol, url]) => (
                      <p key={protocol} className="truncate text-xs text-muted-foreground">{protocol}: {url}</p>
                    ))}
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">
                    P{account.priority} · W{account.weight} · {account.hasApiKey ? 'key configured' : 'missing key'}
                    {account.balance != null && ` · ${account.balance} ${account.currency ?? ''}`}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button onClick={() => void syncModels(account)}>Sync models</Button>
                  <Button onClick={() => void mutate(() => api.post(`/api/v1/accounts/${account.id}/balance/refresh`))}>Balance</Button>
                  <Button onClick={() => void mutate(() => api.patch(`/api/v1/accounts/${account.id}`, { enabled: !account.enabled }))}>{account.enabled ? 'Disable' : 'Enable'}</Button>
                  <Button variant="danger" onClick={() => void mutate(() => api.delete(`/api/v1/accounts/${account.id}`))}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <CardHeader title="Add provider" description="Use one key with multiple protocol-specific URLs." />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void submit() }}>
          <Field label="Name"><input className={inputClass} required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} /></Field>
          {protocols.map((protocol) => (
            <Field key={protocol} label={`${protocol} base URL`}>
              <input
                className={inputClass}
                value={draft.protocolEndpoints[protocol] ?? ''}
                onChange={(e) => setDraft({ ...draft, protocolEndpoints: { ...draft.protocolEndpoints, [protocol]: e.target.value } })}
                placeholder={protocol === 'ANTHROPIC' ? 'https://api.anthropic.com' : 'https://api.openai.com'}
              />
            </Field>
          ))}
          <Field label="API key"><input className={inputClass} required type="password" value={draft.apiKey} onChange={(e) => setDraft({ ...draft, apiKey: e.target.value })} /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Priority"><input className={inputClass} type="number" value={draft.priority} onChange={(e) => setDraft({ ...draft, priority: Number(e.target.value) })} /></Field>
            <Field label="Weight"><input className={inputClass} type="number" value={draft.weight} onChange={(e) => setDraft({ ...draft, weight: Number(e.target.value) })} /></Field>
          </div>
          <Field label="Balance endpoint (optional)"><input className={inputClass} value={draft.balanceEndpoint} onChange={(e) => setDraft({ ...draft, balanceEndpoint: e.target.value })} /></Field>
          <label className="flex items-center gap-2 text-xs"><input type="checkbox" checked={draft.enabled} onChange={(e) => setDraft({ ...draft, enabled: e.target.checked })} /> Enabled</label>
          <Button type="submit" variant="primary" disabled={busy}>Create provider</Button>
        </form>
      </Card>
    </div>
  )
}
