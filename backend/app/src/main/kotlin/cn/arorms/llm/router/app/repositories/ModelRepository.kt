package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.Model
import org.springframework.data.jpa.repository.JpaRepository

interface ModelRepository : JpaRepository<Model, Long> {
    fun findByModelId(modelId: String): Model?
    fun existsByModelId(modelId: String): Boolean
    fun findAllByOrderByIdDesc(): List<Model>
}
