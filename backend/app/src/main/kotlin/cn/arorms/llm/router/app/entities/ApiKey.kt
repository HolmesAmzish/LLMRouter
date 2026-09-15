package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.ApiKeyType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * A persisted virtual key used by external callers.
 *
 * Only the SHA-256 digest is stored. Runtime limits and model scope follow
 * LiteLLM's VerificationToken shape while remaining local to this router.
 */
@Entity
@Table(
    name = "api_keys",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name"])],
    indexes = [
        Index(name = "idx_api_key_hash", columnList = "key_hash", unique = true),
        Index(name = "idx_api_key_enabled", columnList = "enabled"),
        Index(name = "idx_api_key_expires_at", columnList = "expires_at")
    ]
)
class ApiKey(
    @Column(nullable = false, length = 120)
    var name: String,

    @Column(name = "key_hash", nullable = false, length = 64)
    var keyHash: String,

    @Column(nullable = false, length = 32)
    var prefix: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", length = 32)
    var keyType: ApiKeyType? = ApiKeyType.GATEWAY,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null,

    @Column(name = "last_used_at")
    var lastUsedAt: OffsetDateTime? = null,

    @Column(name = "max_budget", precision = 20, scale = 8)
    var maxBudget: BigDecimal? = null,

    @Column(precision = 20, scale = 8)
    var spend: BigDecimal? = BigDecimal.ZERO,

    @Column(name = "rpm_limit")
    var rpmLimit: Int? = null,

    @Column(name = "tpm_limit")
    var tpmLimit: Long? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "models", columnDefinition = "jsonb")
    var models: Set<String>? = emptySet(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, String>? = emptyMap()
) : BaseEntity()
