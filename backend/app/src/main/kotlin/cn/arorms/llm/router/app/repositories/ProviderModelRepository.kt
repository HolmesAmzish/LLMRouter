package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.ProviderModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProviderModelRepository : JpaRepository<ProviderModel, Long> {
    fun findByProviderIdOrderByModelIdAsc(providerId: Long): List<ProviderModel>
    fun findByProviderIdAndModelId(providerId: Long, modelId: String): ProviderModel?
    fun existsByProviderIdAndModelId(providerId: Long, modelId: String): Boolean
    fun findByProviderNameAndModelIdAndEnabledTrue(providerName: String, modelId: String): ProviderModel?
    fun findByEnabledTrue(): List<ProviderModel>
    fun findByModelId(modelId: String): List<ProviderModel>
    fun deleteByProviderId(providerId: Long)

    @Modifying
    @Query("update ProviderModel p set p.modelDefinitionId = null where p.modelDefinitionId = :modelDefinitionId")
    fun clearModelDefinition(@Param("modelDefinitionId") modelDefinitionId: Long)
}
