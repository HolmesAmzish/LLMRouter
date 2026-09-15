import { useState } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import AccountsPage from './pages/AccountsPage'
import ApiKeysPage from './pages/ApiKeysPage'
import DocsPage from './pages/DocsPage'
import ModelsPage from './pages/ModelsPage'
import SessionsPage from './pages/SessionsPage'
import UsagePage from './pages/UsagePage'
import PlaygroundPage from './pages/PlaygroundPage'
import { ProtectedRoute } from './components/ProtectedRoute'
import { CallbackPage } from './features/login/CallbackPage'
import { LoginPage } from './features/login/LoginPage'
import { useAuth } from './hooks/useAuth'

type Page = 'accounts' | 'models' | 'api-keys' | 'sessions' | 'usage' | 'playground' | 'docs'

const navigation: { id: Page; label: string }[] = [
  { id: 'accounts', label: 'Accounts' },
  { id: 'models', label: 'Models' },
  { id: 'api-keys', label: 'API Keys' },
  { id: 'sessions', label: 'Sessions' },
  { id: 'usage', label: 'Usage' },
  { id: 'playground', label: 'Playground' },
  { id: 'docs', label: 'Calling Docs' },
]

function AppShell() {
  const [page, setPage] = useState<Page>('accounts')
  const [refreshKey, setRefreshKey] = useState(0)
  const { user, logout } = useAuth()

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
            <p>OpenAI Chat · OpenAI Responses · Anthropic</p>
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
            <h1 className="text-sm font-medium">{page === 'api-keys' ? 'API Keys' : page === 'docs' ? 'Calling Docs' : page}</h1>
          </div>
          <div className="flex items-center gap-2">
            <button className="h-8 rounded-md border border-border px-3 text-xs" onClick={() => setRefreshKey((key) => key + 1)}>Sync UI</button>
            <div className="flex h-8 items-center gap-2 rounded-md border border-border px-2">
              <div className="grid h-5 w-5 place-items-center rounded-full bg-primary text-[10px] font-semibold text-primary-foreground">
                {(user?.username?.[0] ?? 'A').toUpperCase()}
              </div>
              <span className="hidden text-xs sm:inline">{user?.username ?? 'Admin'}</span>
              <button className="ml-1 text-xs text-muted-foreground hover:text-danger" onClick={logout}>Logout</button>
            </div>
          </div>
        </header>
        <div key={refreshKey} className="p-4 lg:p-6">
          {page === 'accounts' && <AccountsPage onChanged={() => setRefreshKey((key) => key + 1)} />}
          {page === 'models' && <ModelsPage onChanged={() => setRefreshKey((key) => key + 1)} />}
          {page === 'api-keys' && <ApiKeysPage />}
          {page === 'sessions' && <SessionsPage />}
          {page === 'usage' && <UsagePage />}
          {page === 'playground' && <PlaygroundPage />}
          {page === 'docs' && <DocsPage />}
        </div>
      </main>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/callback" element={<CallbackPage />} />
        <Route path="/*" element={<ProtectedRoute><AppShell /></ProtectedRoute>} />
      </Routes>
    </BrowserRouter>
  )
}
