import { useState } from 'react'
import type { Protocol, ThinkingEffort } from '../types'
import { Alert, Button, Card, CardHeader, Field, inputClass } from '../components/ui'

const protocols: Protocol[] = ['OPENAI', 'ANTHROPIC', 'GEMINI']
const efforts: ThinkingEffort[] = ['NONE', 'LOW', 'MEDIUM', 'HIGH']

type ChatMessageView = { role: string; content: string }

export default function PlaygroundPage() {
  const [protocol, setProtocol] = useState<Protocol>('OPENAI')
  const [model, setModel] = useState('gpt-4o-mini')
  const [sessionId, setSessionId] = useState('')
  const [thinkingEffort, setThinkingEffort] = useState<ThinkingEffort>('NONE')
  const [prompt, setPrompt] = useState('')
  const [toolJson, setToolJson] = useState('')
  const [image, setImage] = useState<File | null>(null)
  const [messages, setMessages] = useState<ChatMessageView[]>([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const fileToDataUrl = (file: File) => new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })

  const send = async () => {
    if (!prompt.trim()) return
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

      const body: Record<string, unknown> = {
        model,
        messages: [{ role: 'user', content }],
        max_tokens: 1024,
        stream: false,
      }
      if (sessionId) { body.metadata = { session_id: sessionId } }
      if (thinkingEffort !== 'NONE') { body.reasoning_effort = thinkingEffort.toLowerCase() }
      if (toolJson.trim()) { body.tools = JSON.parse(toolJson) }

      const path = protocol === 'OPENAI' ? '/v1/chat/completions' : protocol === 'ANTHROPIC' ? '/v1/messages' : `/v1beta/models/${model}:generateContent`
      const response = await fetch(path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
      const payload = await response.json()
      if (!response.ok) throw new Error(payload?.error?.message ?? payload?.message ?? `Request failed: ${response.status}`)

      const text = protocol === 'OPENAI'
        ? payload.choices?.[0]?.message?.content ?? ''
        : protocol === 'ANTHROPIC'
          ? payload.content?.map((part: { text?: string }) => part.text ?? '').join('') ?? ''
          : payload.candidates?.[0]?.content?.parts?.map((part: { text?: string }) => part.text ?? '').join('') ?? ''
      setMessages((current) => [...current, { role: 'assistant', content: text }])
      setImage(null)
    } catch (cause) {
      setError(String(cause instanceof Error ? cause.message : cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_340px]">
      <Card className="flex min-h-[620px] flex-col">
        <CardHeader title="Playground" description="Image input, tools and thinking effort are sent through the unified gateway." />
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
          <textarea className={`${inputClass} h-24 resize-y py-2`} value={prompt} onChange={(event) => setPrompt(event.target.value)} placeholder="Ask a question, or attach an image for multimodal input." />
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <label className="text-xs text-muted-foreground">
              <input type="file" accept="image/*" onChange={(event) => setImage(event.target.files?.[0] ?? null)} />
            </label>
            {image && <span className="text-xs text-muted-foreground">{image.name}</span>}
            <Button variant="primary" disabled={busy} onClick={() => void send()}>{busy ? 'Sending' : 'Send'}</Button>
          </div>
        </div>
      </Card>

      <Card>
        <CardHeader title="Request settings" />
        <form className="grid gap-3 p-4" onSubmit={(event) => { event.preventDefault(); void send() }}>
          <Field label="Protocol">
            <select className={inputClass} value={protocol} onChange={(event) => setProtocol(event.target.value as Protocol)}>
              {protocols.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          <Field label="Model"><input className={inputClass} value={model} onChange={(event) => setModel(event.target.value)} /></Field>
          <Field label="Session ID (optional)"><input className={inputClass} value={sessionId} onChange={(event) => setSessionId(event.target.value)} /></Field>
          <Field label="Thinking effort">
            <select className={inputClass} value={thinkingEffort} onChange={(event) => setThinkingEffort(event.target.value as ThinkingEffort)}>
              {efforts.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </Field>
          <Field label="Tools JSON (OpenAI format)"><textarea className={`${inputClass} h-28 font-mono`} value={toolJson} onChange={(event) => setToolJson(event.target.value)} placeholder="[{&quot;type&quot;:&quot;function&quot;,...}]" /></Field>
        </form>
      </Card>
    </div>
  )
}
