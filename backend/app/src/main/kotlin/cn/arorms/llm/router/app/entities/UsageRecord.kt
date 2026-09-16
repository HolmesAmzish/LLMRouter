package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.Protocol
import cn.arorms.llm.router.common.enums.TokenInputSemantics
import cn.arorms.llm.router.common.enums.UsageDataSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

/**
 * One inference call. Provider and upstream model are separated from the
 * public model name so usage can be filtered by either dimension.
 */
@Entity
@Table(
    name = "usage_records",
    indexes = [
        Index(name = "idx_usage_created_at", columnList = "created_at"),
        Index(name = "idx_usage_session_id", columnList = "session_id"),
        Index(name = "idx_usage_provider_model", columnList = "provider,model"),
        Index(name = "idx_usage_api_key", columnList = "api_key_id"),
        Index(name = "idx_usage_request_id", columnList = "request_id"),
        Index(name = "idx_usage_status", columnList = "status"),
        Index(name = "idx_usage_cache", columnList = "data_source,cache_hit")
    ]
)
class UsageRecord(
    @Column(name = "request_id", length = 64)
    var requestId: String?,

    @Column(name = "session_id", length = 64)
    var sessionId: String?,

    @Column(name = "api_key_id")
    var apiKeyId: Long? = null,

    @Column(name = "api_key_name", length = 120)
    var apiKeyName: String? = null,

    @Column(nullable = false, length = 120)
    var provider: String,

    @Column(name = "account_name", length = 120)
    var accountName: String?,

    @Column(name = "model", nullable = false, length = 200)
    var model: String,

    @Column(name = "public_model", length = 340)
    var publicModel: String?,

    @Column(name = "upstream_model", length = 200)
    var upstreamModel: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var protocol: Protocol,

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Long,

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Long,

    @Column(name = "total_tokens", nullable = false)
    var totalTokens: Long,

    @Column(name = "cache_read_tokens")
    var cacheReadTokens: Long? = 0,

    @Column(name = "cache_creation_tokens")
    var cacheCreationTokens: Long? = 0,

    @Column(name = "reasoning_tokens")
    var reasoningTokens: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "input_token_semantics", length = 24)
    var inputTokenSemantics: TokenInputSemantics? = TokenInputSemantics.UNKNOWN,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_details", columnDefinition = "jsonb")
    var tokenDetails: Map<String, Long>? = emptyMap(),

    @Column(name = "latency_ms", nullable = false)
    var latencyMs: Long,

    @Column(name = "first_token_ms")
    var firstTokenMs: Long? = null,

    @Column(name = "api_base", length = 2000)
    var apiBase: String? = null,

    @Column(name = "cache_hit")
    var cacheHit: Boolean? = false,

    @Column(name = "cache_key", length = 64)
    var cacheKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", length = 24)
    var dataSource: UsageDataSource? = UsageDataSource.UPSTREAM,

    @Column(name = "is_streaming")
    var isStreaming: Boolean? = false,

    @Column(nullable = false, length = 32)
    var status: String,

    @Column(name = "status_code")
    var statusCode: Int? = null,

    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = null,

    @Column(name = "started_at")
    var startedAt: OffsetDateTime? = null,

    @Column(name = "finished_at")
    var finishedAt: OffsetDateTime? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, String>? = emptyMap()
) : BaseEntity()
