package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "provider_models",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider_id", "model_id"])],
    indexes = [
        Index(name = "idx_provider_models_model_id", columnList = "model_id"),
        Index(name = "idx_provider_models_provider_model", columnList = "provider_name,model_id"),
        Index(name = "idx_provider_models_definition", columnList = "model_definition_id")
    ]
)
class ProviderModel(
    @Column(name = "provider_id", nullable = false)
    var providerId: Long,
    @Column(name = "provider_name", nullable = false, length = 120)
    var providerName: String,
    @Column(name = "model_id", nullable = false, length = 200)
    var modelId: String,
    @Column(name = "model_name", nullable = false, length = 200)
    var modelName: String,
    @Column(name = "model_definition_id")
    var modelDefinitionId: Long? = null,
    @Column(nullable = false)
    var enabled: Boolean = true
) : BaseEntity()
