export type Protocol = 'OPENAI' | 'OPENAI_RESPONSES' | 'ANTHROPIC'
// export type Protocol = 'OPENAI' | 'OPENAI_RESPONSES' | 'ANTHROPIC' | 'GEMINI'
export type ThinkingEffort = 'NONE' | 'MINIMAL' | 'LOW' | 'MEDIUM' | 'HIGH'

export type ProviderAccount = {
  id: number
  name: string
  protocolEndpoints: Partial<Record<Protocol, string>>
  enabled: boolean
  priority: number
  weight: number
  balanceEndpoint: string | null
  modelMapping: Record<string, string>
  configuration: Record<string, string>
  status: string
  balance: number | null
  currency: string | null
  hasApiKey: boolean
  createdAt: string | null
  updatedAt: string | null
}

export type ModelResponse = {
  id: string
  provider: string
  model: string
  protocol: Protocol
  protocols?: Protocol[]
  ownedBy: string
  displayName?: string | null
  upstreamModel: string
  enabled: boolean
  maxContextTokens?: number | null
  maxOutputTokens?: number | null
}

export type ModelListResponse = { provider: string; objectValue?: string; data: ModelResponse[] }

export type ManualModelRequest = {
  accountId: number
  model: string
  protocol?: Protocol
  upstreamModel?: string
  displayName?: string
  ownedBy?: string
  enabled: boolean
  maxContextTokens?: number
  maxOutputTokens?: number
}

export type ApiKey = {
  id: number
  name: string
  prefix: string
  apiKey?: string | null
  enabled: boolean
  expiresAt: string | null
  lastUsedAt: string | null
  maxBudget?: number | null
  spend?: number
  rpmLimit?: number | null
  tpmLimit?: number | null
  models?: string[]
  metadata?: Record<string, string>
  createdAt: string | null
  updatedAt: string | null
}

export type SessionResponse = {
  id: string
  title: string
  model: string
  protocol: Protocol
  enabled: boolean
  messageCount: number
  inputTokens: number
  outputTokens: number
  createdAt: string | null
  updatedAt: string | null
}

export type UsageRecord = {
  id: number
  requestId?: string
  sessionId: string | null
  apiKeyId?: number | null
  apiKeyName?: string | null
  provider: string
  accountName: string | null
  model: string
  publicModel?: string
  upstreamModel?: string
  protocol: Protocol
  inputTokens: number
  outputTokens: number
  totalTokens: number
  costCents: number | null
  latencyMs: number
  firstTokenMs?: number | null
  apiBase?: string | null
  cacheHit?: boolean
  status: string
  statusCode?: number | null
  createdAt: string | null
}

export type UsagePageResponse = {
  content: UsageRecord[]
  total: number
  page: number
  size: number
  totalTokens: number
}
