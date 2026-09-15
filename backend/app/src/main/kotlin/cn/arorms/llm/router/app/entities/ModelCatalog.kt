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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal

/**
 * One explicit model deployment. A public model may have multiple deployments
 * when an account exposes it through more than one protocol.
 */
@Entity
@Table(
    name = "model_catalog",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["account_id", "model_id", "protocol"]),
        UniqueConstraint(columnNames = ["account_id", "public_name", "protocol"])
    ],
    indexes = [
        Index(name = "idx_model_provider_model_protocol", columnList = "provider,model_id,protocol"),
        Index(name = "idx_model_public_name", columnList = "public_name")
    ]
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

    @Column(name = "public_name", length = 340)
    var publicName: String?,

    @Column(name = "upstream_model", length = 200)
    var upstreamModel: String?,

    @Column(name = "owned_by", length = 120)
    var ownedBy: String?,

    @Column(name = "display_name", length = 200)
    var displayName: String?,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "manual", nullable = false)
    var manual: Boolean = false,

    @Column
    var priority: Int? = 100,

    @Column
    var weight: Int? = 100,

    @Column(name = "max_context_tokens")
    var maxContextTokens: Int? = null,

    @Column(name = "max_output_tokens")
    var maxOutputTokens: Int? = null,

    @Column(name = "input_cost_per_million", precision = 18, scale = 8)
    var inputCostPerMillion: BigDecimal? = null,

    @Column(name = "output_cost_per_million", precision = 18, scale = 8)
    var outputCostPerMillion: BigDecimal? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_info", columnDefinition = "jsonb")
    var modelInfo: Map<String, Any?>? = emptyMap()
) : BaseEntity()
