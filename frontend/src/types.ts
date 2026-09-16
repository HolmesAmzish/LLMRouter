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
  models: ProviderModel[]
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
  modelName: string
  displayName?: string | null
  upstreamModel: string
  enabled: boolean
}

export type ModelPrice = {
  id: number
  modelId: string
  modelName: string
  ownedBy: string | null
  enabled: boolean
  inputCostPerMillion: number | null
  outputCostPerMillion: number | null
  cacheReadCostPerMillion: number | null
  cacheCreationCostPerMillion: number | null
  currency: string
  createdAt: string | null
  updatedAt: string | null
}

export type ModelPriceRequest = {
  modelId: string
  modelName: string
  ownedBy?: string
  enabled: boolean
  inputCostPerMillion?: number | null
  outputCostPerMillion?: number | null
  cacheReadCostPerMillion?: number | null
  cacheCreationCostPerMillion?: number | null
  currency: string
}

export type ProviderModel = {
  id: number
  providerId: number
  providerName: string
  modelId: string
  modelName: string
  modelPriceId: number | null
  priced: boolean
  enabled: boolean
}

export type ProviderModelRequest = {
  modelId: string
  modelName?: string
  modelPriceId?: number | null
  enabled: boolean
}

export type ModelListResponse = { provider: string; objectValue?: string; data: ModelResponse[] }

export type ProviderAccountRequest = {
  name: string
  protocolEndpoints: Partial<Record<Protocol, string>>
  apiKey?: string
  enabled: boolean
  priority: number
  weight: number
  balanceEndpoint?: string
  modelMapping?: Record<string, string>
  configuration?: Record<string, string>
  models: ProviderModelRequest[]
}

export type ProviderAccountPatch = {
  name?: string
  protocolEndpoints?: Partial<Record<Protocol, string>>
  apiKey?: string
  enabled?: boolean
  priority?: number
  weight?: number
  balanceEndpoint?: string
  modelMapping?: Record<string, string>
  configuration?: Record<string, string>
  models?: ProviderModelRequest[]
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
  requestId: string
  sessionId: string | null
  apiKeyId: number | null
  apiKeyName: string | null
  provider: string
  accountName: string | null
  model: string
  publicModel: string
  upstreamModel: string
  protocol: Protocol
  inputTokens: number
  outputTokens: number
  totalTokens: number
  cacheReadTokens: number
  cacheCreationTokens: number
  reasoningTokens?: number | null
  realTotalTokens: number
  inputTokenSemantics: 'UNKNOWN' | 'TOTAL' | 'FRESH'
  isStreaming: boolean
  latencyMs: number
  firstTokenMs: number | null
  apiBase: string | null
  cacheHit: boolean
  dataSource: 'UPSTREAM' | 'ROUTER_CACHE'
  status: string
  statusCode: number | null
  createdAt: string | null
}

export type UsagePageResponse = {
  content: UsageRecord[]
  total: number
  page: number
  size: number
  totalTokens: number
}
