package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.Currency
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal

@Entity
@Table(
    name = "models",
    uniqueConstraints = [UniqueConstraint(columnNames = ["model_id"])],
    indexes = [Index(name = "idx_models_enabled", columnList = "enabled")]
)
class Model(
    @Column(name = "model_id", nullable = false, length = 200)
    var modelId: String,
    @Column(name = "model_name", nullable = false, length = 200)
    var modelName: String,
    @Column(name = "owned_by", length = 120)
    var ownedBy: String? = null,
    @Column(nullable = false)
    var enabled: Boolean = true,
    @Column(name = "input_cost_per_million", precision = 20, scale = 8)
    var inputCostPerMillion: BigDecimal? = null,
    @Column(name = "output_cost_per_million", precision = 20, scale = 8)
    var outputCostPerMillion: BigDecimal? = null,
    @Column(name = "cache_read_cost_per_million", precision = 20, scale = 8)
    var cacheReadCostPerMillion: BigDecimal? = null,
    @Column(name = "cache_creation_cost_per_million", precision = 20, scale = 8)
    var cacheCreationCostPerMillion: BigDecimal? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    var currency: Currency = Currency.USD
) : BaseEntity()
