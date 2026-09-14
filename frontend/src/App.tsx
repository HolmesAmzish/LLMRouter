import { useState } from 'react'
import AccountsPage from './pages/AccountsPage'
import ModelsPage from './pages/ModelsPage'
import SessionsPage from './pages/SessionsPage'
import UsagePage from './pages/UsagePage'
import PlaygroundPage from './pages/PlaygroundPage'

type Page = 'accounts' | 'models' | 'sessions' | 'usage' | 'playground'

const navigation: { id: Page; label: string }[] = [
  { id: 'accounts', label: 'Accounts' },
  { id: 'models', label: 'Models' },
  { id: 'sessions', label: 'Sessions' },
  { id: 'usage', label: 'Usage' },
  { id: 'playground', label: 'Playground' },
]

export default function App() {
  const [page, setPage] = useState<Page>('accounts')
  const [refreshKey, setRefreshKey] = useState(0)

  return (
    <div className="flex min-h-full">
      <aside className="hidden w-56 shrink-0 border-r border-border bg-card lg:block">
        <div className="flex h-full flex-col p-4">
          <div>
            <p className="text-sm font-semibold">LLM Router</p>
            <p className="mt-0.5 text-xs text-muted-foreground">Multi-provider gateway</p>
          </div>
          <nav className="mt-6 grid gap-1">
            {navigation.map((item) => (
              <button key={item.id} onClick={() => setPage(item.id)}
                className={`flex h-9 items-center rounded-md px-3 text-left text-xs font-medium transition ${page === item.id ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`}>
                {item.label}
              </button>
            ))}
          </nav>
          <div className="mt-auto rounded-md border border-border p-3 text-xs text-muted-foreground">
            <p>OpenAI · Anthropic · Gemini</p>
            <p className="mt-1">Protocol conversion, caching, sessions, tools and usage tracking.</p>
          </div>
        </div>
      </aside>

      <main className="min-w-0 flex-1">
        <header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-border bg-card/85 px-4 backdrop-blur">
          <div className="flex items-center gap-3">
            <select className="h-8 rounded-md border border-input bg-card px-2 text-xs lg:hidden" value={page} onChange={(event) => setPage(event.target.value as Page)}>
              {navigation.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
            </select>
            <h1 className="text-sm font-medium capitalize">{page}</h1>
          </div>
          <button className="h-8 rounded-md border border-border px-3 text-xs" onClick={() => setRefreshKey((key) => key + 1)}>Sync UI</button>
        </header>
        <div key={refreshKey} className="p-4 lg:p-6">
          {page === 'accounts' && <AccountsPage onChanged={() => setRefreshKey((key) => key + 1)} />}
          {page === 'models' && <ModelsPage />}
          {page === 'sessions' && <SessionsPage />}
          {page === 'usage' && <UsagePage />}
          {page === 'playground' && <PlaygroundPage />}
        </div>
      </main>
    </div>
  )
}
