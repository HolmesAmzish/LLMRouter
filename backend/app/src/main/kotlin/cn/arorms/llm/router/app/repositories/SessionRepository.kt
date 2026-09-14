package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.Session
import org.springframework.data.jpa.repository.JpaRepository

interface SessionRepository : JpaRepository<Session, Long> {
    fun findBySessionId(sessionId: String): Session?
}
