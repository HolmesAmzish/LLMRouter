package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * Model catalog entry synchronized from one provider account.
 */
@Entity
@Table(
    name = "model_catalog",
    uniqueConstraints = [UniqueConstraint(columnNames = ["account_id", "model_id"])]
)
class ModelCatalog(
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @Column(name = "model_id", nullable = false, length = 200)
    var modelId: String,

    @Column(name = "owned_by", length = 120)
    var ownedBy: String?,

    @Column(nullable = false)
    var enabled: Boolean = true
) : BaseEntity()
