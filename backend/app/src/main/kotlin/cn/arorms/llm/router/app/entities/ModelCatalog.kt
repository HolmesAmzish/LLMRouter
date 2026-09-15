package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.Protocol
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * A manually configured or synchronized model entry owned by one account.
 */
@Entity
@Table(
    name = "model_catalog",
    uniqueConstraints = [UniqueConstraint(columnNames = ["account_id", "model_id"])],
    indexes = [Index(name = "idx_model_provider_model", columnList = "provider,model_id")]
)
class ModelCatalog(
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @Column(nullable = false, length = 120)
    var provider: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var protocol: Protocol,

    @Column(name = "model_id", nullable = false, length = 200)
    var modelId: String,

    @Column(name = "owned_by", length = 120)
    var ownedBy: String?,

    @Column(name = "display_name", length = 200)
    var displayName: String?,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "manual", nullable = false)
    var manual: Boolean = false
) : BaseEntity()
