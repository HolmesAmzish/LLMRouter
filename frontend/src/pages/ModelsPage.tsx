import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { ModelPrice, ModelPriceRequest } from '../types'
import { Alert, Badge, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

type Draft = {
  modelId: string
  modelName: string
  ownedBy: string
  enabled: boolean
  inputCostPerMillion: string
  outputCostPerMillion: string
  cacheReadCostPerMillion: string
  cacheCreationCostPerMillion: string
  currency: string
}

const emptyDraft: Draft = {
  modelId: '',
  modelName: '',
  ownedBy: '',
  enabled: true,
  inputCostPerMillion: '',
  outputCostPerMillion: '',
  cacheReadCostPerMillion: '',
  cacheCreationCostPerMillion: '',
  currency: 'USD',
}

function toNumber(value: string): number | undefined {
  if (!value.trim()) return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function fromNumber(value: number | null | undefined): string {
  return value == null ? '' : String(value)
}

export default function ModelsPage({ onChanged }: { onChanged?: () => void }) {
  const [models, setModels] = useState<ModelPrice[]>([])
  const [draft, setDraft] = useState<Draft>(emptyDraft)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setModels(await api.get<ModelPrice[]>('/api/v1/models/prices'))
  }, [])

  useEffect(() => { void load() }, [load])

  const resetForm = () => {
    setDraft(emptyDraft)
    setEditingId(null)
  }

  const startEdit = (model: ModelPrice) => {
    setEditingId(model.id)
    setDraft({
      modelId: model.modelId,
      modelName: model.modelName,
      ownedBy: model.ownedBy ?? '',
      enabled: model.enabled,
      inputCostPerMillion: fromNumber(model.inputCostPerMillion),
      outputCostPerMillion: fromNumber(model.outputCostPerMillion),
      cacheReadCostPerMillion: fromNumber(model.cacheReadCostPerMillion),
      cacheCreationCostPerMillion: fromNumber(model.cacheCreationCostPerMillion),
      currency: model.currency,
    })
  }

  const submit = async () => {
    setBusy(true); setError('')
    try {
      if (!draft.modelId.trim()) throw new Error('Model id is required')
      const payload: ModelPriceRequest = {
        modelId: draft.modelId.trim(),
        modelName: draft.modelName.trim() || draft.modelId.trim(),
        ownedBy: draft.ownedBy.trim() || undefined,
        enabled: draft.enabled,
        inputCostPerMillion: toNumber(draft.inputCostPerMillion) ?? null,
        outputCostPerMillion: toNumber(draft.outputCostPerMillion) ?? null,
        cacheReadCostPerMillion: toNumber(draft.cacheReadCostPerMillion) ?? null,
        cacheCreationCostPerMillion: toNumber(draft.cacheCreationCostPerMillion) ?? null,
        currency: draft.currency.trim().toUpperCase() || 'USD',
      }
      if (editingId == null) await api.post<ModelPrice>('/api/v1/models/prices', payload)
      else await api.patch<ModelPrice>(`/api/v1/models/prices/${editingId}`, payload)
      resetForm()
      await load(); onChanged?.()
    } catch (cause) {
      setError(String(cause instanceof Error ? cause.message : cause))
    } finally { setBusy(false) }
  }

  const mutate = async (action: () => Promise<unknown>) => {
    setBusy(true); setError('')
    try { await action(); await load(); onChanged?.() }
    catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  const visible = models.filter((model) =>
    `${model.modelId} ${model.modelName} ${model.ownedBy ?? ''}`.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_400px]">
      <Card>
        <CardHeader
          title="Models"
          description="Global model profiles used for pricing. Provider bindings are managed separately."
          action={<Button variant="secondary" onClick={() => void load()}>Refresh</Button>}
        />
        <div className="px-4 pb-4">
          <input className={inputClass} placeholder="Search model id or name" value={search} onChange={(event) => setSearch(event.target.value)} />
        </div>
        {error && <div className="px-4 pb-4"><Alert>{error}</Alert></div>}
        {visible.length === 0 ? (
          <EmptyState title="No model profiles" description="Create a model profile so usage can be priced later." />
        ) : (
          <div className="divide-y divide-border">
            {visible.map((model) => (
              <article key={model.id} className="px-4 py-3">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h3 className="truncate font-mono text-sm font-medium">{model.modelId}</h3>
                      {model.enabled ? <Badge tone="success">enabled</Badge> : <Badge tone="warning">disabled</Badge>}
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">{model.modelName} · {model.ownedBy ?? '—'} · {model.currency}</p>
                    <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-muted-foreground sm:grid-cols-4">
                      <span>In: {model.inputCostPerMillion ?? '—'}</span>
                      <span>Out: {model.outputCostPerMillion ?? '—'}</span>
                      <span>Cache read: {model.cacheReadCostPerMillion ?? '—'}</span>
                      <span>Cache write: {model.cacheCreationCostPerMillion ?? '—'}</span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button onClick={() => startEdit(model)}>Edit</Button>
                    <Button onClick={() => void mutate(() => api.patch(`/api/v1/models/prices/${model.id}`, { enabled: !model.enabled }))}>
                      {model.enabled ? 'Disable' : 'Enable'}
                    </Button>
                    <Button variant="danger" onClick={() => void mutate(() => api.delete(`/api/v1/models/prices/${model.id}`))}>Delete</Button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <CardHeader
          title={editingId == null ? 'Add model profile' : 'Edit model pricing'}
          description="Prices are per million tokens. Cache write is charged when a provider creates prompt cache."
        />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void submit() }}>
          <Field label="Model id">
            <input
              className={inputClass}
              required
              value={draft.modelId}
              disabled={editingId != null}
              onChange={(event) => setDraft({ ...draft, modelId: event.target.value })}
              placeholder="deepseek-v4-flash"
            />
          </Field>
          <Field label="Model name"><input className={inputClass} required value={draft.modelName} onChange={(event) => setDraft({ ...draft, modelName: event.target.value })} /></Field>
          <Field label="Owned by"><input className={inputClass} value={draft.ownedBy} onChange={(event) => setDraft({ ...draft, ownedBy: event.target.value })} /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Input / M"><input className={inputClass} type="number" step="any" min="0" value={draft.inputCostPerMillion} onChange={(event) => setDraft({ ...draft, inputCostPerMillion: event.target.value })} /></Field>
            <Field label="Output / M"><input className={inputClass} type="number" step="any" min="0" value={draft.outputCostPerMillion} onChange={(event) => setDraft({ ...draft, outputCostPerMillion: event.target.value })} /></Field>
            <Field label="Cache read / M"><input className={inputClass} type="number" step="any" min="0" value={draft.cacheReadCostPerMillion} onChange={(event) => setDraft({ ...draft, cacheReadCostPerMillion: event.target.value })} /></Field>
            <Field label="Cache write / M"><input className={inputClass} type="number" step="any" min="0" value={draft.cacheCreationCostPerMillion} onChange={(event) => setDraft({ ...draft, cacheCreationCostPerMillion: event.target.value })} /></Field>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Currency"><input className={inputClass} value={draft.currency} onChange={(event) => setDraft({ ...draft, currency: event.target.value })} /></Field>
            <label className="flex items-end gap-2 pb-2 text-xs"><input type="checkbox" checked={draft.enabled} onChange={(event) => setDraft({ ...draft, enabled: event.target.checked })} /> Enabled</label>
          </div>
          <div className="flex gap-2">
            <Button type="submit" variant="primary" disabled={busy}>{editingId == null ? 'Create model' : 'Save pricing'}</Button>
            {editingId != null && <Button onClick={resetForm}>Cancel</Button>}
          </div>
        </form>
      </Card>
    </div>
  )
}
