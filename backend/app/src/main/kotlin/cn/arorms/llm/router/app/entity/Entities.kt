package cn.arorms.llm.router.app.entity

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.Protocol
import cn.arorms.llm.router.common.ThinkingEffort
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.OffsetDateTime

@Entity
@Table(name = "provider_accounts", uniqueConstraints = [UniqueConstraint(columnNames = ["name"])])
class ProviderAccountEntity(
    @Column(nullable = false, length = 120)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var protocol: Protocol,

    @Column(nullable = false, length = 500)
    var baseUrl: String,

    @Column(nullable = false, length = 2000)
    var apiKey: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(nullable = false)
    var priority: Int = 100,

    @Column(nullable = false)
    var weight: Int = 100,

    @Column(name = "balance_endpoint", length = 500)
    var balanceEndpoint: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var modelMapping: Map<String, String> = emptyMap(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var configuration: Map<String, String> = emptyMap(),

    @Column(nullable = false, length = 40)
    var status: String = "unknown",

    var balance: Double? = null,

    @Column(length = 16)
    var currency: String? = null,

    @Column(name = "balance_checked_at")
    var balanceCheckedAt: OffsetDateTime? = null
) : BaseEntity()

@Entity
@Table(name = "router_sessions")
class SessionEntity(
    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    var sessionId: String,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, length = 200)
    var model: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var protocol: Protocol,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "message_count", nullable = false)
    var messageCount: Int = 0,

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Long = 0,

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Long = 0
) : BaseEntity()

@Entity
@Table(name = "usage_records", indexes = [
    Index(name = "idx_usage_created_at", columnList = "created_at"),
    Index(name = "idx_usage_session_id", columnList = "session_id")
])
class UsageRecordEntity(
    @Column(name = "session_id", length = 64)
    var sessionId: String?,

    @Column(nullable = false, length = 120)
    var provider: String,

    @Column(name = "account_name", length = 120)
    var accountName: String?,

    @Column(nullable = false, length = 200)
    var model: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var protocol: Protocol,

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Long,

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Long,

    @Column(name = "total_tokens", nullable = false)
    var totalTokens: Long,

    @Column(name = "cost_cents")
    var costCents: Long?,

    @Column(name = "latency_ms", nullable = false)
    var latencyMs: Long,

    @Column(nullable = false, length = 32)
    var status: String
) : BaseEntity()

@Entity
@Table(name = "cache_entries", indexes = [Index(name = "idx_cache_fingerprint", columnList = "fingerprint", unique = true)])
class CacheEntryEntity(
    @Column(nullable = false, length = 64)
    var fingerprint: String,

    @Column(name = "session_id", length = 64)
    var sessionId: String?,

    @Column(nullable = false, length = 24)
    var protocol: Protocol,

    @Column(nullable = false, length = 200)
    var model: String,

    @Column(name = "response_json", nullable = false, columnDefinition = "text")
    var responseJson: String,

    @Column(name = "hit_count", nullable = false)
    var hitCount: Long = 0,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime
) : BaseEntity()

@Entity
@Table(name = "model_catalog", uniqueConstraints = [UniqueConstraint(columnNames = ["account_id", "model_id"])])
class ModelCatalogEntity(
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @Column(name = "model_id", nullable = false, length = 200)
    var modelId: String,

    @Column(name = "owned_by", length = 120)
    var ownedBy: String?,

    @Column(nullable = false)
    var enabled: Boolean = true
) : BaseEntity()
