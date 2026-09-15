import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/api'
import type { ModelListResponse, ModelResponse, Protocol, ThinkingEffort } from '../types'
import { Alert, Badge, Button, Card, CardHeader, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'OPENAI_RESPONSES', 'ANTHROPIC']
const efforts: ThinkingEffort[] = ['NONE', 'MINIMAL', 'LOW', 'MEDIUM', 'HIGH']

type ChatMessageView = { role: string; content: string }

export default function PlaygroundPage() {
  const [protocol, setProtocol] = useState<Protocol>('OPENAI')
  const [models, setModels] = useState<ModelResponse[]>([])
  const [model, setModel] = useState('')
  const [apiKey, setApiKey] = useState(() => localStorage.getItem('router.playground.apiKey') ?? '')
  const [temperature, setTemperature] = useState('0.7')
  const [sessionId, setSessionId] = useState('')
  const [thinkingEffort, setThinkingEffort] = useState<ThinkingEffort>('NONE')
  const [prompt, setPrompt] = useState('')
  const [toolJson, setToolJson] = useState('')
  const [image, setImage] = useState<File | null>(null)
  const [messages, setMessages] = useState<ChatMessageView[]>([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [stream, setStream] = useState(true)

  const loadModels = useCallback(async () => {
    const result = await api.get<ModelListResponse>('/api/v1/models')
    setModels(result.data)
    setModel((current) => current || result.data[0]?.id || '')
  }, [])

  useEffect(() => { void loadModels() }, [loadModels])
  useEffect(() => { localStorage.setItem('router.playground.apiKey', apiKey) }, [apiKey])

  const fileToDataUrl = (file: File) => new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })

  const send = async () => {
    if (!prompt.trim()) return
    if (!apiKey.trim()) { setError('Create and enter a gateway API key first.'); return }
    setBusy(true); setError('')
    const userText = prompt
    setMessages((current) => [...current, { role: 'user', content: userText }])
    setPrompt('')

    try {
      let content: unknown = userText
      if (image) {
        const dataUrl = await fileToDataUrl(image)
        content = [
          { type: 'text', text: userText },
          { type: 'image_url', image_url: { url: dataUrl } },
        ]
      }

      const isResponses = protocol === 'OPENAI_RESPONSES'
      const body: Record<string, unknown> = isResponses
        ? { model, input: content, max_output_tokens: 1024, stream: false }
        : { model, messages: [{ role: 'user', content }], max_tokens: 1024, stream: false }
      const parsedTemperature = Number(temperature)
      if (Number.isFinite(parsedTemperature)) body.temperature = parsedTemperature
      if (sessionId) body.metadata = { session_id: sessionId }
      if (thinkingEffort !== 'NONE') {
        if (isResponses) body.reasoning = { effort: thinkingEffort.toLowerCase() }
        else body.reasoning_effort = thinkingEffort.toLowerCase()
      }
      if (toolJson.trim()) body.tools = JSON.parse(toolJson)

      const path = protocol === 'OPENAI'
        ? '/v1/chat/completions'
        : protocol === 'OPENAI_RESPONSES' ? '/v1/responses' : '/v1/messages'
      const response = await fetch(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Api-Key': apiKey },
        body: JSON.stringify(body),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        throw new Error(payload?.error?.message ?? payload?.message ?? `Request failed: ${response.status}`)
      }

      const contentType = response.headers.get('content-type') ?? ''
      if (stream && contentType.includes('text/event-stream')) {
        const reader = response.body?.getReader()
        if (!reader) throw new Error('Streaming is not supported by this browser')
        const decoder = new TextDecoder()
        let buffer = ''
        let event = ''
        setMessages((current) => [...current, { role: 'assistant', content: '' }])

        const appendDelta = (delta: string) => {
          if (!delta) return
          setMessages((current) => {
            const next = [...current]
            const last = next[next.length - 1]
            if (last?.role === 'assistant') next[next.length - 1] = { ...last, content: last.content + delta }
            return next
          })
        }

        const handleData = (payload: string) => {
          if (!payload || payload === '[DONE]') return
          const parsed = JSON.parse(payload)
          if (protocol === 'OPENAI') appendDelta(parsed.choices?.[0]?.delta?.content ?? '')
          else if (protocol === 'OPENAI_RESPONSES') {
            if (parsed.type === 'response.output_text.delta') appendDelta(parsed.delta ?? '')
          } else if (parsed.type === 'content_block_delta') appendDelta(parsed.delta?.text ?? '')
        }

        while (true) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split(/\r?\n/)
          buffer = lines.pop() ?? ''
          for (const line of lines) {
            if (line.startsWith('event:')) event = line.slice(6).trim()
            else if (line.startsWith('data:')) {
              const payload = line.slice(5).trim()
              if (protocol === 'ANTHROPIC' && event !== 'content_block_delta') continue
              handleData(payload)
            }
          }
        }
      } else {
        const payload = await response.json()
        const text = protocol === 'OPENAI'
          ? payload.choices?.[0]?.message?.content ?? ''
          : protocol === 'OPENAI_RESPONSES'
            ? payload.output?.flatMap((item: { content?: Array<{ type?: string; text?: string }> }) => item.content ?? [])
                .filter((part: { type?: string }) => part.type === 'output_text')
                .map((part: { text?: string }) => part.text ?? '').join('') ?? ''
            : payload.content?.map((part: { text?: string }) => part.text ?? '').join('') ?? ''
        setMessages((current) => [...current, { role: 'assistant', content: text }])
      }
      setImage(null)
    } catch (cause) {
      setError(String(cause instanceof Error ? cause.message : cause))
    } finally { setBusy(false) }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
      <Card className="flex min-h-[680px] flex-col">
        <CardHeader title="Playground" description="Qualified model name, API key, streaming, image input and tools." />
        <div className="flex-1 space-y-3 overflow-y-auto p-4">
          {messages.length === 0 && <p className="text-xs text-muted-foreground">Send a request to inspect router behavior.</p>}
          {messages.map((message, index) => (
            <div key={index} className={`max-w-[80%] rounded-lg border px-3 py-2 text-xs ${message.role === 'user' ? 'ml-auto border-border bg-card' : 'mr-auto border-primary/20 bg-primary/8'}`}>
              <p className="mb-1 font-medium">{message.role}</p>
              <p className="whitespace-pre-wrap">{message.content}</p>
            </div>
          ))}
        </div>
        {error && <div className="px-4 pb-3"><Alert>{error}</Alert></div>}
        <div className="border-t border-border p-4">
          <textarea className={`${inputClass} h-28 resize-y py-2`} value={prompt} onChange={(event) => setPrompt(event.target.value)} placeholder="Ask a question, or attach an image for multimodal input." />
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <label className="text-xs text-muted-foreground"><input type="file" accept="image/*" onChange={(event) => setImage(event.target.files?.[0] ?? null)} /></label>
            {image && <Badge>{image.name}</Badge>}
            <Button variant="secondary" onClick={() => setMessages([])}>Clear</Button>
            <Button variant="primary" disabled={busy || !model} onClick={() => void send()}>{busy ? 'Sending' : 'Send'}</Button>
          </div>
        </div>
      </Card>

      <Card>
        <CardHeader title="Request settings" />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void send() }}>
          <Field label="Gateway API key"><input className={`${inputClass} font-mono`} required type="password" value={apiKey} onChange={(event) => setApiKey(event.target.value)} /></Field>
          <Field label="Model">
            <select className={inputClass} required value={model} onChange={(event) => setModel(event.target.value)}>
              <option value="" disabled>Select model</option>
              {models.map((item) => <option key={item.id} value={item.id}>{item.id}</option>)}
            </select>
          </Field>
          <Field label="Protocol">
            <select className={inputClass} value={protocol} onChange={(event) => setProtocol(event.target.value as Protocol)}>
              {protocols.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          <Field label="Temperature"><input className={inputClass} type="number" min="0" max="2" step="0.1" value={temperature} onChange={(event) => setTemperature(event.target.value)} /></Field>
          <Field label="Session ID (optional)"><input className={inputClass} value={sessionId} onChange={(event) => setSessionId(event.target.value)} /></Field>
          <Field label="Thinking effort">
            <select className={inputClass} value={thinkingEffort} onChange={(event) => setThinkingEffort(event.target.value as ThinkingEffort)}>
              {efforts.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          <label className="flex items-center gap-2 text-xs"><input type="checkbox" checked={stream} onChange={(event) => setStream(event.target.checked)} /> Stream response</label>
          <Field label="Tools JSON"><textarea className={`${inputClass} h-28 font-mono`} value={toolJson} onChange={(event) => setToolJson(event.target.value)} placeholder="[{&quot;type&quot;:&quot;function&quot;,...}]" /></Field>
        </form>
      </Card>
    </div>
  )
}
