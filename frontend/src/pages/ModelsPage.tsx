import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { ManualModelRequest, ModelListResponse, ModelResponse, Protocol, ProviderAccount } from '../types'
import { Alert, Badge, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'OPENAI_RESPONSES', 'ANTHROPIC']

export default function ModelsPage({ onChanged }: { onChanged?: () => void }) {
  const [models, setModels] = useState<ModelResponse[]>([])
  const [accounts, setAccounts] = useState<ProviderAccount[]>([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [draft, setDraft] = useState<ManualModelRequest>({
    accountId: 0, model: '', protocol: 'OPENAI', displayName: '', ownedBy: '', enabled: true,
  })

  const load = useCallback(async () => {
    const [modelResult, accountResult] = await Promise.all([
      api.get<ModelListResponse>('/api/v1/models'),
      api.get<ProviderAccount[]>('/api/v1/accounts'),
    ])
    setModels(modelResult.data)
    setAccounts(accountResult)
    setDraft((current) => ({ ...current, accountId: current.accountId || accountResult[0]?.id || 0 }))
  }, [])

  useEffect(() => { void load() }, [load])

  const mutate = async (action: () => Promise<unknown>) => {
    setError(''); setBusy(true)
    try { await action(); await load(); onChanged?.() }
    catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  const create = async () => {
    setBusy(true); setError('')
    try {
      await api.post<ModelResponse>('/api/v1/models', { ...draft, displayName: draft.displayName || undefined })
      setDraft({ ...draft, model: '', displayName: '', ownedBy: '' })
      await load(); onChanged?.()
    } catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
      <Card>
        <CardHeader title="Models" description="Public names use <provider>/<model> and map to an explicit upstream account." action={<Button onClick={() => void load()}>Refresh</Button>} />
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        {models.length === 0 ? <EmptyState title="No models" description="Manually add a model or sync one from a provider endpoint." /> : (
          <div className="divide-y divide-border">
            {models.map((model) => (
              <article key={model.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="truncate text-sm font-medium">{model.id}</h3>
                    {model.enabled ? <Badge tone="success">enabled</Badge> : <Badge tone="warning">disabled</Badge>}
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">provider: {model.provider} · model: {model.model} · {model.ownedBy}</p>
                </div>
                <div className="flex gap-2">
                  <Button onClick={() => void mutate(() => api.patch(`/api/v1/models/${model.id}?enabled=${!model.enabled}`, {}))}>{model.enabled ? 'Disable' : 'Enable'}</Button>
                  <Button variant="danger" onClick={() => void mutate(() => api.delete(`/api/v1/models/${model.id}`))}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>

      <div className="grid gap-4">
        <Card>
          <CardHeader title="Add model" description="Manual entries are preserved during provider sync." />
          <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void create() }}>
            <Field label="Provider account">
              <select className={inputClass} required value={draft.accountId} onChange={(event) => setDraft({ ...draft, accountId: Number(event.target.value) })}>
                <option value={0} disabled>Select account</option>
                {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
              </select>
            </Field>
            <Field label="Model name"><input className={inputClass} required value={draft.model} onChange={(event) => setDraft({ ...draft, model: event.target.value })} /></Field>
            <Field label="Protocol">
              <select className={inputClass} value={draft.protocol ?? 'OPENAI'} onChange={(event) => setDraft({ ...draft, protocol: event.target.value as Protocol })}>
                {protocols.map((protocol) => <option key={protocol} value={protocol}>{protocol}</option>)}
              </select>
            </Field>
            <Field label="Display name"><input className={inputClass} value={draft.displayName ?? ''} onChange={(event) => setDraft({ ...draft, displayName: event.target.value })} /></Field>
            <Field label="Owned by"><input className={inputClass} value={draft.ownedBy ?? ''} onChange={(event) => setDraft({ ...draft, ownedBy: event.target.value })} /></Field>
            <label className="flex items-center gap-2 text-xs"><input type="checkbox" checked={draft.enabled} onChange={(event) => setDraft({ ...draft, enabled: event.target.checked })} /> Enabled</label>
            <Button type="submit" variant="primary" disabled={busy}>Add model</Button>
          </form>
        </Card>

        <Card>
          <CardHeader title="Sync upstream" description="Sync each protocol endpoint separately." />
          <div className="grid gap-3 p-4">
            {accounts.length === 0 && <p className="text-xs text-muted-foreground">No accounts configured.</p>}
            {accounts.map((account) => (
              <div key={account.id} className="rounded-md border border-border p-3">
                <p className="text-xs font-medium">{account.name}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {protocols.map((protocol) => (
                    <Button key={protocol} disabled={busy || !account.protocolEndpoints[protocol]}
                      onClick={() => void mutate(() => api.post<ModelListResponse>(`/api/v1/models/accounts/${account.id}/${protocol}/sync`))}>
                      {protocol}
                    </Button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}
