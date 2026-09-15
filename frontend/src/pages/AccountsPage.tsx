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
  modelsEndpoint: string
}

const emptyDraft: Draft = {
  name: '',
  protocolEndpoints: {
    OPENAI: '',
    OPENAI_RESPONSES: '',
    ANTHROPIC: '',
  },
  apiKey: '',
  enabled: true,
  priority: 100,
  weight: 100,
  balanceEndpoint: '',
  modelsEndpoint: '',
}

const placeholders: Record<Protocol, string> = {
  OPENAI: 'https://api.openai.com/v1/chat/completions',
  OPENAI_RESPONSES: 'https://api.openai.com/v1/responses',
  ANTHROPIC: 'https://api.anthropic.com/v1/messages',
}

export default function AccountsPage({ onChanged }: { onChanged?: () => void }) {
  const [accounts, setAccounts] = useState<ProviderAccount[]>([])
  const [draft, setDraft] = useState<Draft>(emptyDraft)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setAccounts(await api.get<ProviderAccount[]>('/api/v1/accounts'))
  }, [])

  useEffect(() => { void load() }, [load])

  const resetForm = () => {
    setDraft(emptyDraft)
    setEditingId(null)
  }

  const startEdit = (account: ProviderAccount) => {
    setError('')
    setEditingId(account.id)
    setDraft({
      name: account.name,
      protocolEndpoints: {
        OPENAI: account.protocolEndpoints.OPENAI ?? '',
        OPENAI_RESPONSES: account.protocolEndpoints.OPENAI_RESPONSES ?? '',
        ANTHROPIC: account.protocolEndpoints.ANTHROPIC ?? '',
      },
      apiKey: '',
      enabled: account.enabled,
      priority: account.priority,
      weight: account.weight,
      balanceEndpoint: account.balanceEndpoint ?? '',
      modelsEndpoint: account.configuration.modelsEndpoint ?? '',
    })
  }

  const submit = async () => {
    setBusy(true); setError('')
    try {
      const protocolEndpoints = Object.fromEntries(
        Object.entries(draft.protocolEndpoints)
          .map(([protocol, url]) => [protocol, url.trim()])
          .filter(([, url]) => url)
      )
      if (Object.keys(protocolEndpoints).length === 0) {
        throw new Error('Configure at least one protocol endpoint')
      }

      const existing = editingId == null ? null : accounts.find((account) => account.id === editingId)
      const configuration = { ...(existing?.configuration ?? {}) }
      if (draft.modelsEndpoint.trim()) configuration.modelsEndpoint = draft.modelsEndpoint.trim()
      else delete configuration.modelsEndpoint

      const payload: Record<string, unknown> = {
        name: draft.name.trim(),
        protocolEndpoints,
        enabled: draft.enabled,
        priority: draft.priority,
        weight: draft.weight,
        balanceEndpoint: draft.balanceEndpoint.trim(),
        configuration,
      }

      if (editingId == null) {
        await api.post<ProviderAccount>('/api/v1/accounts', { ...payload, apiKey: draft.apiKey })
      } else {
        if (draft.apiKey.trim()) payload.apiKey = draft.apiKey.trim()
        await api.patch<ProviderAccount>(`/api/v1/accounts/${editingId}`, payload)
      }

      resetForm()
      await load(); onChanged?.()
    } catch (cause) {
      setError(String(cause instanceof Error ? cause.message : cause))
    } finally {
      setBusy(false)
    }
  }

  const mutate = async (action: () => Promise<unknown>) => {
    setError(''); setBusy(true)
    try { await action(); await load(); onChanged?.() }
    catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  const remove = async (account: ProviderAccount) => {
    await mutate(() => api.delete(`/api/v1/accounts/${account.id}`))
    if (editingId === account.id) resetForm()
  }

  const syncModels = (account: ProviderAccount) => mutate(() => api.post<ModelListResponse>(`/api/v1/models/accounts/${account.id}/sync`))

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
      <Card>
        <CardHeader title="Provider accounts" description="Each protocol stores one complete POST endpoint." action={<Button variant="secondary" onClick={() => void load()}>Refresh</Button>} />
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
                  <Button onClick={() => startEdit(account)}>Edit</Button>
                  <Button onClick={() => void syncModels(account)}>Sync models</Button>
                  <Button onClick={() => void mutate(() => api.post(`/api/v1/accounts/${account.id}/balance/refresh`))}>Balance</Button>
                  <Button onClick={() => void mutate(() => api.patch(`/api/v1/accounts/${account.id}`, { enabled: !account.enabled }))}>{account.enabled ? 'Disable' : 'Enable'}</Button>
                  <Button variant="danger" onClick={() => void remove(account)}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <CardHeader
          title={editingId == null ? 'Add provider' : 'Edit provider'}
          description="Enter complete POST endpoint URLs. The gateway does not append paths."
        />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void submit() }}>
          <Field label="Name"><input className={inputClass} required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} /></Field>
          {protocols.map((protocol) => (
            <Field key={protocol} label={`${protocol} endpoint URL`}>
              <input
                className={inputClass}
                required={!!draft.protocolEndpoints[protocol]}
                value={draft.protocolEndpoints[protocol] ?? ''}
                onChange={(e) => setDraft({ ...draft, protocolEndpoints: { ...draft.protocolEndpoints, [protocol]: e.target.value } })}
                placeholder={placeholders[protocol]}
              />
            </Field>
          ))}
          <Field label={editingId == null ? 'API key' : 'API key (leave blank to keep)'}>
            <input
              className={inputClass}
              required={editingId == null}
              type="password"
              value={draft.apiKey}
              onChange={(e) => setDraft({ ...draft, apiKey: e.target.value })}
            />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Priority"><input className={inputClass} type="number" value={draft.priority} onChange={(e) => setDraft({ ...draft, priority: Number(e.target.value) })} /></Field>
            <Field label="Weight"><input className={inputClass} type="number" value={draft.weight} onChange={(e) => setDraft({ ...draft, weight: Number(e.target.value) })} /></Field>
          </div>
          <Field label="Balance endpoint (optional)"><input className={inputClass} value={draft.balanceEndpoint} onChange={(e) => setDraft({ ...draft, balanceEndpoint: e.target.value })} /></Field>
          <Field label="Model list endpoint (optional)"><input className={inputClass} value={draft.modelsEndpoint} onChange={(e) => setDraft({ ...draft, modelsEndpoint: e.target.value })} /></Field>
          <label className="flex items-center gap-2 text-xs"><input type="checkbox" checked={draft.enabled} onChange={(e) => setDraft({ ...draft, enabled: e.target.checked })} /> Enabled</label>
          <div className="flex gap-2">
            <Button type="submit" variant="primary" disabled={busy}>{editingId == null ? 'Create provider' : 'Save changes'}</Button>
            {editingId != null && <Button onClick={resetForm}>Cancel</Button>}
          </div>
        </form>
      </Card>
    </div>
  )
}
