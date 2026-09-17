import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api/api'
import { getAccessToken } from '../api/auth'
import type { ApiKey, ModelListResponse, ModelResponse, Protocol, ThinkingEffort, WebChatMessage } from '../types'
import { Alert, Badge, Button, Card, CardHeader, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'OPENAI_RESPONSES', 'ANTHROPIC']
const efforts: ThinkingEffort[] = ['NONE', 'MINIMAL', 'LOW', 'MEDIUM', 'HIGH']

type ChatMessageView = { id?: number; role: string; content: string }

export default function PlaygroundPage() {
  const [protocol, setProtocol] = useState<Protocol>('OPENAI')
  const [models, setModels] = useState<ModelResponse[]>([])
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([])
  const [apiKeyId, setApiKeyId] = useState(() => localStorage.getItem('router.playground.apiKeyId') ?? '')
  const [model, setModel] = useState('')
  const [temperature, setTemperature] = useState('0.7')
  const [thinkingEffort, setThinkingEffort] = useState<ThinkingEffort>('NONE')
  const [prompt, setPrompt] = useState('')
  const [toolJson, setToolJson] = useState('')
  const [image, setImage] = useState<File | null>(null)
  const [messages, setMessages] = useState<ChatMessageView[]>([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const loadOptions = useCallback(async () => {
    const [modelResult, keyResult, history] = await Promise.all([
      api.get<ModelListResponse>('/api/v1/models'),
      api.get<ApiKey[]>('/api/v1/api-keys'),
      api.get<WebChatMessage[]>('/api/v1/playground/history'),
    ])
    setModels(modelResult.data)
    const enabledKeys = keyResult.filter((key) => key.enabled)
    setApiKeys(enabledKeys)
    setApiKeyId((current) => enabledKeys.some((key) => String(key.id) === current)
      ? current
      : String(enabledKeys[0]?.id ?? ''))
    setMessages(history.map((message) => ({ id: message.id, role: message.role, content: message.content })))
  }, [])

  useEffect(() => { void loadOptions() }, [loadOptions])
  useEffect(() => { localStorage.setItem('router.playground.apiKeyId', apiKeyId) }, [apiKeyId])

  const selectedKey = useMemo(
    () => apiKeys.find((key) => String(key.id) === apiKeyId),
    [apiKeyId, apiKeys],
  )
  const availableModels = useMemo(() => {
    const allowed = selectedKey?.models ?? []
    return allowed.length === 0 ? models : models.filter((item) => allowed.includes(item.id))
  }, [models, selectedKey])

  useEffect(() => {
    if (!availableModels.some((item) => item.id === model)) setModel(availableModels[0]?.id ?? '')
  }, [availableModels, model])

  const fileToDataUrl = (file: File) => new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })

  const clear = async () => {
    setBusy(true); setError('')
    try {
      await api.delete('/api/v1/playground/history')
      setMessages([])
    } catch (cause) {
      setError(String(cause instanceof Error ? cause.message : cause))
    } finally { setBusy(false) }
  }

  const send = async () => {
    if (!prompt.trim()) return
    if (!selectedKey) { setError('Select an enabled gateway API key first.'); return }
    setBusy(true); setError('')
    const userText = prompt
    setMessages((current) => [...current, { role: 'user', content: userText }])
    setPrompt('')

    try {
      const imageDataUrl = image ? await fileToDataUrl(image) : undefined
      const tools = toolJson.trim() ? JSON.parse(toolJson) : []
      const request = {
        content: userText,
        model,
        protocol,
        temperature: Number.isFinite(Number(temperature)) ? Number(temperature) : undefined,
        thinkingEffort,
        imageDataUrl,
        tools,
      }
      const path = protocol === 'OPENAI'
        ? '/api/v1/playground/chat/completions'
        : protocol === 'OPENAI_RESPONSES' ? '/api/v1/playground/responses' : '/api/v1/playground/messages'
      const accessToken = await getAccessToken()
      if (!accessToken) throw new Error('Management session is not available')
      const response = await fetch(`${path}?apiKeyId=${selectedKey.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify(request),
      })

      if (!response.ok) {
        const payload = await response.json().catch(() => null)
        throw new Error(payload?.error?.message ?? payload?.message ?? `Request failed: ${response.status}`)
      }

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
      setImage(null)
    } catch (cause) {
      setError(String(cause instanceof Error ? cause.message : cause))
    } finally { setBusy(false) }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
      <Card className="flex min-h-[680px] flex-col">
        <CardHeader title="Playground" description="Persistent server-side history with streaming responses." action={<Button variant="secondary" disabled={busy} onClick={() => void clear()}>Clear history</Button>} />
        <div className="flex-1 space-y-3 overflow-y-auto p-4">
          {messages.length === 0 && <p className="text-xs text-muted-foreground">Send a request to inspect router behavior.</p>}
          {messages.map((message, index) => (
            <div key={message.id ?? index} className={`max-w-[80%] rounded-lg border px-3 py-2 text-xs ${message.role === 'user' ? 'ml-auto border-border bg-card' : 'mr-auto border-primary/20 bg-primary/8'}`}>
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
            <Button variant="primary" disabled={busy || !model || !selectedKey} onClick={() => void send()}>{busy ? 'Streaming' : 'Send'}</Button>
          </div>
        </div>
      </Card>

      <Card>
        <CardHeader title="Request settings" />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void send() }}>
          <Field label="Gateway API key">
            <select className={inputClass} required value={apiKeyId} onChange={(event) => setApiKeyId(event.target.value)}>
              <option value="" disabled>Select API key</option>
              {apiKeys.map((key) => <option key={key.id} value={key.id}>{key.name} · {key.prefix}…</option>)}
            </select>
          </Field>
          {apiKeys.length === 0 && <p className="text-xs text-warning">Create an enabled API key before using the Playground.</p>}
          <Field label="Model">
            <select className={inputClass} required value={model} onChange={(event) => setModel(event.target.value)}>
              <option value="" disabled>Select model</option>
              {availableModels.map((item) => <option key={item.id} value={item.id}>{item.id}</option>)}
            </select>
          </Field>
          <Field label="Protocol">
            <select className={inputClass} value={protocol} onChange={(event) => setProtocol(event.target.value as Protocol)}>
              {protocols.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          <Field label="Temperature"><input className={inputClass} type="number" min="0" max="2" step="0.1" value={temperature} onChange={(event) => setTemperature(event.target.value)} /></Field>
          <Field label="Thinking effort">
            <select className={inputClass} value={thinkingEffort} onChange={(event) => setThinkingEffort(event.target.value as ThinkingEffort)}>
              {efforts.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          <Field label="Tools JSON"><textarea className={`${inputClass} h-28 font-mono`} value={toolJson} onChange={(event) => setToolJson(event.target.value)} placeholder="[{&quot;type&quot;:&quot;function&quot;,...}]" /></Field>
        </form>
      </Card>
    </div>
  )
}
