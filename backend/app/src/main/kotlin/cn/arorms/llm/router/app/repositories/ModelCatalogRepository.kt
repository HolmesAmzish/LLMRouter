package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.ModelCatalog
import org.springframework.data.jpa.repository.JpaRepository

interface ModelCatalogRepository : JpaRepository<ModelCatalog, Long> {
    fun findByAccountId(accountId: Long): List<ModelCatalog>
    fun deleteByAccountId(accountId: Long)
    fun findByEnabledTrue(): List<ModelCatalog>
}
