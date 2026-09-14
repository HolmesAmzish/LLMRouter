import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { ModelListResponse, ProviderAccount, Protocol } from '../types'
import { Alert, Badge, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'ANTHROPIC', 'GEMINI']

type Draft = {
  name: string
  protocol: Protocol
  baseUrl: string
  apiKey: string
  enabled: boolean
  priority: number
  weight: number
  balanceEndpoint: string
}

const emptyDraft: Draft = {
  name: '', protocol: 'OPENAI', baseUrl: 'https://api.openai.com', apiKey: '', enabled: true, priority: 100, weight: 100, balanceEndpoint: '',
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
      await api.post<ProviderAccount>('/api/v1/accounts', draft)
      setDraft({ ...emptyDraft, protocol: draft.protocol, baseUrl: draft.protocol === 'OPENAI' ? 'https://api.openai.com' : draft.protocol === 'ANTHROPIC' ? 'https://api.anthropic.com' : 'https://generativelanguage.googleapis.com' })
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
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
      <Card>
        <CardHeader title="Provider accounts" description="Central pool for upstream providers and keys." action={<Button variant="secondary" onClick={() => void load()}>Refresh</Button>} />
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        {accounts.length === 0 ? <EmptyState title="No accounts" description="Add an upstream provider to start routing requests." /> : (
          <div className="divide-y divide-border">
            {accounts.map((account) => (
              <article key={account.id} className="flex flex-wrap items-center justify-between gap-4 px-4 py-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="truncate text-sm font-medium">{account.name}</h3>
                    <Badge tone="primary">{account.protocol}</Badge>
                    {account.enabled ? <Badge tone="success">enabled</Badge> : <Badge tone="warning">disabled</Badge>}
                  </div>
                  <p className="mt-1 truncate text-xs text-muted-foreground">{account.baseUrl}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
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
        <CardHeader title="Add account" description="Credentials are stored only on the server." />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void submit() }}>
          <Field label="Name"><input className={inputClass} required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} /></Field>
          <Field label="Protocol">
            <select className={inputClass} value={draft.protocol} onChange={(e) => setDraft({ ...draft, protocol: e.target.value as Protocol })}>
              {protocols.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          <Field label="Base URL"><input className={inputClass} required value={draft.baseUrl} onChange={(e) => setDraft({ ...draft, baseUrl: e.target.value })} /></Field>
          <Field label="API key"><input className={inputClass} required type="password" value={draft.apiKey} onChange={(e) => setDraft({ ...draft, apiKey: e.target.value })} /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Priority"><input className={inputClass} type="number" value={draft.priority} onChange={(e) => setDraft({ ...draft, priority: Number(e.target.value) })} /></Field>
            <Field label="Weight"><input className={inputClass} type="number" value={draft.weight} onChange={(e) => setDraft({ ...draft, weight: Number(e.target.value) })} /></Field>
          </div>
          <Field label="Balance endpoint (optional)"><input className={inputClass} value={draft.balanceEndpoint} onChange={(e) => setDraft({ ...draft, balanceEndpoint: e.target.value })} /></Field>
          <label className="flex items-center gap-2 text-xs"><input type="checkbox" checked={draft.enabled} onChange={(e) => setDraft({ ...draft, enabled: e.target.checked })} /> Enabled</label>
          <Button type="submit" variant="primary" disabled={busy}>Create account</Button>
        </form>
      </Card>
    </div>
  )
}
