package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.Protocol
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Cached inference response keyed by a canonical request fingerprint.
 */
@Entity
@Table(
    name = "cache_entries",
    indexes = [Index(name = "idx_cache_fingerprint", columnList = "fingerprint", unique = true)]
)
class CacheEntry(
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
