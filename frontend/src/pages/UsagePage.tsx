import { useCallback, useEffect, useState } from 'react'
import { api } from '../lib/api'
import type { UsagePageResponse } from '../types'
import { Alert, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

const emptyFilter = { from: '', to: '', provider: '', model: '', sessionId: '' }

export default function UsagePage() {
  const [result, setResult] = useState<UsagePageResponse | null>(null)
  const [filter, setFilter] = useState(emptyFilter)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setBusy(true); setError('')
    try {
      const params = new URLSearchParams()
      Object.entries(filter).forEach(([key, value]) => { if (value) params.set(key, value) })
      setResult(await api.get<UsagePageResponse>(`/api/v1/usage?${params.toString()}`))
    } catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }, [filter])

  useEffect(() => { void load() }, [load])

  return (
    <Card>
      <CardHeader title="Token usage" description={result ? `${result.total} records · ${result.totalTokens} tokens` : 'Query aggregated and per-request usage.'} action={<Button disabled={busy} onClick={() => void load()}>Refresh</Button>} />
      <div className="grid gap-3 p-4 sm:grid-cols-2 xl:grid-cols-5">
        <Field label="From"><input className={inputClass} value={filter.from} onChange={(event) => setFilter({ ...filter, from: event.target.value })} placeholder="2026-09-01T00:00:00" /></Field>
        <Field label="To"><input className={inputClass} value={filter.to} onChange={(event) => setFilter({ ...filter, to: event.target.value })} placeholder="2026-09-14T23:59:59" /></Field>
        <Field label="Provider"><input className={inputClass} value={filter.provider} onChange={(event) => setFilter({ ...filter, provider: event.target.value })} /></Field>
        <Field label="Model"><input className={inputClass} value={filter.model} onChange={(event) => setFilter({ ...filter, model: event.target.value })} /></Field>
        <Field label="Session"><input className={inputClass} value={filter.sessionId} onChange={(event) => setFilter({ ...filter, sessionId: event.target.value })} /></Field>
      </div>
      {error && <div className="px-4 pb-4"><Alert>{error}</Alert></div>}
      {!result || result.content.length === 0 ? <EmptyState title="No usage records" description="Usage is recorded after successful gateway calls." /> : (
        <div className="overflow-x-auto border-t border-border">
          <table className="w-full min-w-[900px] text-left">
            <thead className="border-b border-border bg-muted/50 text-[11px] uppercase tracking-wide text-muted-foreground">
              <tr><th className="px-4 py-2">Time</th><th className="px-4 py-2">Model</th><th className="px-4 py-2">Provider</th><th className="px-4 py-2">Account</th><th className="px-4 py-2">In</th><th className="px-4 py-2">Out</th><th className="px-4 py-2">Total</th><th className="px-4 py-2">Latency</th></tr>
            </thead>
            <tbody className="divide-y divide-border">
              {result.content.map((record) => (
                <tr key={record.id} className="text-xs">
                  <td className="whitespace-nowrap px-4 py-2">{record.createdAt?.slice(0, 19).replace('T', ' ')}</td>
                  <td className="px-4 py-2">{record.model}</td>
                  <td className="px-4 py-2">{record.provider}</td>
                  <td className="px-4 py-2">{record.accountName ?? '—'}</td>
                  <td className="px-4 py-2">{record.inputTokens}</td>
                  <td className="px-4 py-2">{record.outputTokens}</td>
                  <td className="px-4 py-2 font-medium">{record.totalTokens}</td>
                  <td className="px-4 py-2">{record.latencyMs} ms</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}
