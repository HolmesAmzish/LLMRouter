package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime

/**
 * Hashed gateway API key used by external clients.
 */
@Entity
@Table(
    name = "api_keys",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name"])],
    indexes = [Index(name = "idx_api_key_hash", columnList = "key_hash", unique = true)]
)
class ApiKey(
    @Column(nullable = false, length = 120)
    var name: String,

    @Column(name = "key_hash", nullable = false, length = 64)
    var keyHash: String,

    @Column(nullable = false, length = 32)
    var prefix: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null,

    @Column(name = "last_used_at")
    var lastUsedAt: OffsetDateTime? = null
) : BaseEntity()
