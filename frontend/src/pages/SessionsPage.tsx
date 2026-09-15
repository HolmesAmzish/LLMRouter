import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { Protocol, SessionResponse } from '../types'
import { Alert, Badge, Button, Card, CardHeader, EmptyState, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'OPENAI_RESPONSES', 'ANTHROPIC']

export default function SessionsPage() {
  const [sessions, setSessions] = useState<SessionResponse[]>([])
  const [draft, setDraft] = useState({ title: '', model: '', protocol: 'OPENAI' as Protocol })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => { setSessions(await api.get<SessionResponse[]>('/api/v1/sessions')) }, [])
  useEffect(() => { void load() }, [load])

  const create = async () => {
    setBusy(true); setError('')
    try { await api.post<SessionResponse>('/api/v1/sessions', draft); await load() }
    catch (cause) { setError(String(cause instanceof Error ? cause.message : cause)) }
    finally { setBusy(false) }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_340px]">
      <Card>
        <CardHeader title="Sessions" description="Conversation identity and token totals." action={<Button onClick={() => void load()}>Refresh</Button>} />
        {sessions.length === 0 ? <EmptyState title="No sessions" description="Create a session before sending requests to associate usage." /> : (
          <div className="divide-y divide-border">
            {sessions.map((session) => (
              <article key={session.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <div>
                  <h3 className="text-sm font-medium">{session.title}</h3>
                  <p className="mt-1 text-xs text-muted-foreground">{session.model} · {session.protocol} · {session.id}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge>{session.messageCount} messages</Badge>
                  <Badge tone="primary">{session.inputTokens + session.outputTokens} tokens</Badge>
                  <Button variant="danger" onClick={() => void api.delete(`/api/v1/sessions/${session.id}`).then(load)}>Delete</Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </Card>
      <Card>
        <CardHeader title="New session" />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void create() }}>
          <Field label="Title"><input className={inputClass} required value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} /></Field>
          <Field label="Model"><input className={inputClass} required value={draft.model} onChange={(event) => setDraft({ ...draft, model: event.target.value })} /></Field>
          <Field label="Protocol">
            <select className={inputClass} value={draft.protocol} onChange={(event) => setDraft({ ...draft, protocol: event.target.value as Protocol })}>
              {protocols.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          {error && <Alert>{error}</Alert>}
          <Button type="submit" variant="primary" disabled={busy}>Create</Button>
        </form>
      </Card>
    </div>
  )
}
