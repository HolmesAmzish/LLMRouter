import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { ModelListResponse, ModelPrice, Protocol, ProviderAccount } from '../types'
import { Alert, Badge, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'OPENAI_RESPONSES', 'ANTHROPIC']

type ModelDraft = {
  modelId: string
  modelName: string
  modelPriceId: number | null
  enabled: boolean
}

type Draft = {
  name: string
  protocolEndpoints: Partial<Record<Protocol, string>>
  apiKey: string
  enabled: boolean
  priority: number
  weight: number
  balanceEndpoint: string
  modelsEndpoint: string
  models: ModelDraft[]
}

const emptyDraft: Draft = {
  name: '',
  protocolEndpoints: { OPENAI: '', OPENAI_RESPONSES: '', ANTHROPIC: '' },
  apiKey: '',
  enabled: true,
  priority: 100,
  weight: 100,
  balanceEndpoint: '',
  modelsEndpoint: '',
  models: [{ modelId: '', modelName: '', modelPriceId: null, enabled: true }],
}

const placeholders: Record<Protocol, string> = {
  OPENAI: 'https://api.openai.com/v1/chat/completions',
  OPENAI_RESPONSES: 'https://api.openai.com/v1/responses',
  ANTHROPIC: 'https://api.anthropic.com/v1/messages',
}

export default function AccountsPage({ onChanged }: { onChanged?: () => void }) {
  const [accounts, setAccounts] = useState<ProviderAccount[]>([])
  const [modelPrices, setModelPrices] = useState<ModelPrice[]>([])
  const [draft, setDraft] = useState<Draft>(emptyDraft)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    const [accountResult, modelResult] = await Promise.all([
      api.get<ProviderAccount[]>('/api/v1/accounts'),
      api.get<ModelPrice[]>('/api/v1/models/prices'),
    ])
    setAccounts(accountResult)
    setModelPrices(modelResult)
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
      models: account.models.length > 0 ? account.models.map((model) => ({
        modelId: model.modelId,
        modelName: model.modelName,
        modelPriceId: model.modelPriceId,
        enabled: model.enabled,
      })) : [{ modelId: '', modelName: '', modelPriceId: null, enabled: true }],
    })
  }

  const updateModel = (index: number, value: string) => {
    const matched = modelPrices.find((model) => model.modelId === value)
    setDraft({
      ...draft,
      models: draft.models.map((model, position) => position === index ? {
        modelId: value,
        modelName: matched?.modelName ?? model.modelName,
        modelPriceId: matched?.id ?? null,
        enabled: model.enabled,
      } : model),
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
      if (Object.keys(protocolEndpoints).length === 0) throw new Error('Configure at least one protocol endpoint')

      const models = draft.models
        .map((model) => ({ ...model, modelId: model.modelId.trim() }))
        .filter((model) => model.modelId)
      if (models.some((model, index, all) => all.findIndex((item) => item.modelId === model.modelId) !== index)) {
        throw new Error('Model ids must be unique within a provider')
      }

      const configuration = { ...(editingId == null ? {} : accounts.find((account) => account.id === editingId)?.configuration ?? {}) }
      if (draft.modelsEndpoint.trim()) configuration.modelsEndpoint = draft.modelsEndpoint.trim()
      else delete configuration.modelsEndpoint

      const basePayload = {
        name: draft.name.trim(),
        protocolEndpoints,
        enabled: draft.enabled,
        priority: draft.priority,
        weight: draft.weight,
        balanceEndpoint: draft.balanceEndpoint.trim(),
        configuration,
        models: models.map((model) => ({
          modelId: model.modelId,
          modelName: model.modelName.trim() || model.modelId,
          modelPriceId: model.modelPriceId,
          enabled: model.enabled,
        })),
      }

      if (editingId == null) await api.post<ProviderAccount>('/api/v1/accounts', { ...basePayload, apiKey: draft.apiKey })
      else {
        const payload: Record<string, unknown> = { ...basePayload }
        if (draft.apiKey.trim()) payload.apiKey = draft.apiKey.trim()
        await api.patch<ProviderAccount>(`/api/v1/accounts/${editingId}`, payload)
      }

      resetForm()
      await load(); onChanged?.()
    } catch (cause) {
      setError(String(cause instanceof Error ? cause.message : cause))
    } finally { setBusy(false) }
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
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
      <Card>
        <CardHeader title="Provider accounts" description="Each protocol stores one complete POST endpoint." action={<Button variant="secondary" onClick={() => void load()}>Refresh</Button>} />
        {error && <div className="p-4"><Alert>{error}</Alert></div>}
        {accounts.length === 0 ? <EmptyState title="No accounts" description="Add an upstream provider to start routing requests." /> : (
          <div className="divide-y divide-border">
            {accounts.map((account) => (
              <article key={account.id} className="px-4 py-3">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h3 className="truncate text-sm font-medium">{account.name}</h3>
                      {account.enabled ? <Badge tone="success">enabled</Badge> : <Badge tone="warning">disabled</Badge>}
                    </div>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {Object.keys(account.protocolEndpoints).map((protocol) => <Badge key={protocol} tone="primary">{protocol}</Badge>)}
                    </div>
                    <div className="mt-2 space-y-1">
                      {Object.entries(account.protocolEndpoints).map(([protocol, url]) => (
                        <p key={protocol} className="truncate text-xs text-muted-foreground">{protocol}: {url}</p>
                      ))}
                    </div>
                    {account.models.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-1.5">
                        {account.models.map((model) => (
                          <Badge key={model.id} tone={model.priced ? 'success' : 'warning'}>
                            {model.modelId}{model.priced ? '' : ' · unpriced'}
                          </Badge>
                        ))}
                      </div>
                    )}
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
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <CardHeader
          title={editingId == null ? 'Add provider' : 'Edit provider'}
          description="Enter complete POST endpoint URLs. Model ids can be typed freely; matches to pricing profiles are suggestions only."
        />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void submit() }}>
          <datalist id="provider-model-options">
            {modelPrices.map((model) => (
              <option key={model.id} value={model.modelId}>{model.modelName}</option>
            ))}
          </datalist>
          <Field label="Name"><input className={inputClass} required value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} /></Field>
          {protocols.map((protocol) => (
            <Field key={protocol} label={`${protocol} endpoint URL`}>
              <input
                className={inputClass}
                required={!!draft.protocolEndpoints[protocol]}
                value={draft.protocolEndpoints[protocol] ?? ''}
                onChange={(event) => setDraft({ ...draft, protocolEndpoints: { ...draft.protocolEndpoints, [protocol]: event.target.value } })}
                placeholder={placeholders[protocol]}
              />
            </Field>
          ))}
          <Field label={editingId == null ? 'API key' : 'API key (leave blank to keep)'}>
            <input className={inputClass} required={editingId == null} type="password" value={draft.apiKey} onChange={(event) => setDraft({ ...draft, apiKey: event.target.value })} />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Priority"><input className={inputClass} type="number" value={draft.priority} onChange={(event) => setDraft({ ...draft, priority: Number(event.target.value) })} /></Field>
            <Field label="Weight"><input className={inputClass} type="number" value={draft.weight} onChange={(event) => setDraft({ ...draft, weight: Number(event.target.value) })} /></Field>
          </div>
          <Field label="Balance endpoint (optional)"><input className={inputClass} value={draft.balanceEndpoint} onChange={(event) => setDraft({ ...draft, balanceEndpoint: event.target.value })} /></Field>
          <Field label="Model list endpoint (optional)"><input className={inputClass} value={draft.modelsEndpoint} onChange={(event) => setDraft({ ...draft, modelsEndpoint: event.target.value })} /></Field>

          <div className="rounded-md border border-border p-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-medium">Provider models</p>
              <Button onClick={() => setDraft({ ...draft, models: [...draft.models, { modelId: '', modelName: '', modelPriceId: null, enabled: true }] })}>Add model</Button>
            </div>
            <p className="mt-1 text-xs text-muted-foreground">Type a model id. Existing pricing profiles appear as suggestions, but unmatched ids are allowed.</p>
            <div className="mt-3 space-y-3">
              {draft.models.map((model, index) => {
                const matched = modelPrices.find((price) => price.modelId === model.modelId)
                return (
                  <div key={index} className="grid gap-2 rounded-md border border-border p-2">
                    <Field label="Model id">
                      <input
                        className={inputClass}
                        list="provider-model-options"
                        required
                        value={model.modelId}
                        onChange={(event) => updateModel(index, event.target.value)}
                      />
                    </Field>
                    <Field label="Model name">
                      <input
                        className={inputClass}
                        value={model.modelName}
                        onChange={(event) => setDraft({
                          ...draft,
                          models: draft.models.map((item, position) => position === index ? { ...item, modelName: event.target.value } : item),
                        })}
                      />
                    </Field>
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs text-muted-foreground">{matched ? `pricing: ${matched.modelName}` : 'no pricing profile'}</span>
                      <div className="flex items-center gap-2">
                        <label className="flex items-center gap-1 text-xs"><input type="checkbox" checked={model.enabled} onChange={(event) => setDraft({ ...draft, models: draft.models.map((item, position) => position === index ? { ...item, enabled: event.target.checked } : item) })} /> Enabled</label>
                        <Button onClick={() => setDraft({ ...draft, models: draft.models.filter((_, position) => position !== index) })}>Remove</Button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          <label className="flex items-center gap-2 text-xs"><input type="checkbox" checked={draft.enabled} onChange={(event) => setDraft({ ...draft, enabled: event.target.checked })} /> Enabled</label>
          <div className="flex gap-2">
            <Button type="submit" variant="primary" disabled={busy}>{editingId == null ? 'Create provider' : 'Save changes'}</Button>
            {editingId != null && <Button onClick={resetForm}>Cancel</Button>}
          </div>
        </form>
      </Card>
    </div>
  )
}
