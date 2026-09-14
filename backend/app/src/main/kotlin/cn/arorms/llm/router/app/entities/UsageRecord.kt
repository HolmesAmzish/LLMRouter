package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.Protocol
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Persisted token usage and latency for one gateway request.
 */
@Entity
@Table(
    name = "usage_records",
    indexes = [
        Index(name = "idx_usage_created_at", columnList = "created_at"),
        Index(name = "idx_usage_session_id", columnList = "session_id")
    ]
)
class UsageRecord(
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
