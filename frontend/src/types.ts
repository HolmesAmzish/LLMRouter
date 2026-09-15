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

export type ModelResponse = { id: string; ownedBy: string; enabled: boolean }
export type ModelListResponse = { provider: string; objectValue?: string; data: ModelResponse[] }

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
  sessionId: string | null
  provider: string
  accountName: string | null
  model: string
  protocol: Protocol
  inputTokens: number
  outputTokens: number
  totalTokens: number
  costCents: number | null
  latencyMs: number
  status: string
  createdAt: string | null
}

export type UsagePageResponse = {
  content: UsageRecord[]
  total: number
  page: number
  size: number
  totalTokens: number
}
