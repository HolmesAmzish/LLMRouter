package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.ProviderAccount
import org.springframework.data.jpa.repository.JpaRepository

interface ProviderAccountRepository : JpaRepository<ProviderAccount, Long> {
    fun findByEnabledTrueOrderByPriorityAscIdAsc(): List<ProviderAccount>
}
