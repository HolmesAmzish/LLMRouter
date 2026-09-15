package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.ApiKey
import org.springframework.data.jpa.repository.JpaRepository

interface ApiKeyRepository : JpaRepository<ApiKey, Long> {
    fun findByKeyHash(keyHash: String): ApiKey?
    fun findAllByOrderByIdDesc(): List<ApiKey>
}
