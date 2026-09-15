import { Card, CardHeader } from '../components/ui'

const baseUrl = typeof window === 'undefined' ? 'http://localhost:5173' : window.location.origin

const snippets = [
  {
    title: 'OpenAI Chat Completions',
    body: `curl -N '${baseUrl}/v1/chat/completions' \\
  -H 'Content-Type: application/json' \\
  -H 'X-Api-Key: sk-router-your-key' \\
  --data-raw '{
    "model": "my-provider/gpt-4o-mini",
    "messages": [{"role":"user","content":"Hello"}],
    "max_tokens": 512,
    "stream": true
  }'`,
  },
  {
    title: 'OpenAI Responses',
    body: `curl -N '${baseUrl}/v1/responses' \\
  -H 'Content-Type: application/json' \\
  -H 'X-Api-Key: sk-router-your-key' \\
  --data-raw '{
    "model": "my-provider/gpt-4o-mini",
    "input": "Hello",
    "max_output_tokens": 512,
    "stream": true
  }'`,
  },
  {
    title: 'Anthropic Messages',
    body: `curl -N '${baseUrl}/v1/messages' \\
  -H 'Content-Type: application/json' \\
  -H 'X-Api-Key: sk-router-your-key' \\
  --data-raw '{
    "model": "my-provider/claude-sonnet",
    "max_tokens": 512,
    "stream": true,
    "messages": [{"role":"user","content":"Hello"}]
  }'`,
  },
]

export default function DocsPage() {
  return (
    <div className="grid gap-4">
      <Card>
        <CardHeader title="Calling the gateway" description="Use a gateway API key and a qualified model name." />
        <div className="grid gap-3 p-4 text-xs text-muted-foreground">
          <p>Model names use <code className="rounded bg-muted px-1 py-0.5 text-foreground">&lt;provider&gt;/&lt;model&gt;</code>. There is no automatic provider fallback.</p>
          <p>The gateway accepts <code className="rounded bg-muted px-1 py-0.5 text-foreground">X-Api-Key</code> or <code className="rounded bg-muted px-1 py-0.5 text-foreground">Authorization: Bearer sk-router-...</code>.</p>
        </div>
      </Card>
      {snippets.map((snippet) => (
        <Card key={snippet.title}>
          <CardHeader title={snippet.title} />
          <pre className="overflow-x-auto p-4 text-xs leading-5">{snippet.body}</pre>
        </Card>
      ))}
    </div>
  )
}
