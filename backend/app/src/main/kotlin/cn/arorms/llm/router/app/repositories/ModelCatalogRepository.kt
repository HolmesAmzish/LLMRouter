package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.ModelCatalog
import cn.arorms.llm.router.common.enums.Protocol
import org.springframework.data.jpa.repository.JpaRepository

interface ModelCatalogRepository : JpaRepository<ModelCatalog, Long> {
    fun findByAccountId(accountId: Long): List<ModelCatalog>
    fun deleteByAccountId(accountId: Long)
    fun findByEnabledTrue(): List<ModelCatalog>
    fun findFirstByProviderAndModelIdAndProtocolAndEnabledTrue(
        provider: String,
        modelId: String,
        protocol: Protocol
    ): ModelCatalog?
    fun findFirstByProviderAndModelIdAndEnabledTrueOrderByPriorityAscIdAsc(
        provider: String,
        modelId: String
    ): ModelCatalog?
    fun existsByAccountIdAndModelIdAndProtocol(
        accountId: Long,
        modelId: String,
        protocol: Protocol
    ): Boolean
    fun findByAccountIdAndProtocol(accountId: Long, protocol: Protocol): List<ModelCatalog>
}
