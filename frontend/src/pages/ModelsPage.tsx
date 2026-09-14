import { useCallback, useEffect, useState } from 'react'
import { api } from '../lib/api'
import type { ModelListResponse, Protocol } from '../types'
import { Alert, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'ANTHROPIC', 'GEMINI']

export default function ModelsPage() {
  const [models, setModels] = useState<string[]>([])
  const [provider, setProvider] = useState('')
  const [protocol, setProtocol] = useState<Protocol>('OPENAI')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async (nextProtocol: Protocol) => {
    setBusy(true); setError('')
    try {
      const result = await api.get<ModelListResponse>(`/api/v1/models?protocol=${nextProtocol}`)
      setModels(result.data.map((model) => model.id)); setProvider(result.provider)
    } catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }, [])

  useEffect(() => { void load('OPENAI') }, [load])

  return (
    <div className="grid gap-4">
      <Card>
        <CardHeader title="Upstream models" description="Refresh model catalogs directly from configured providers." action={<Button variant="primary" disabled={busy} onClick={() => void load(protocol)}>Load</Button>} />
        <div className="grid gap-3 p-4 md:grid-cols-3">
          <Field label="Protocol">
            <select className={inputClass} value={protocol} onChange={(event) => setProtocol(event.target.value as Protocol)}>
              {protocols.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
        </div>
        {error && <div className="px-4 pb-4"><Alert>{error}</Alert></div>}
        {models.length === 0 ? <EmptyState title="No models loaded" description="Configure an enabled provider and refresh the model catalog." /> : (
          <div className="grid gap-2 p-4 pt-0 sm:grid-cols-2 lg:grid-cols-3">
            {models.map((model) => (
              <div key={model} className="rounded-md border border-border bg-muted/40 px-3 py-2">
                <p className="truncate text-xs font-medium">{model}</p>
                <p className="mt-0.5 text-[11px] text-muted-foreground">{provider}</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
