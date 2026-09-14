package cn.arorms.llm.router.app.services

import cn.arorms.llm.router.app.entities.Session
import cn.arorms.llm.router.app.mappers.SessionMapper
import cn.arorms.llm.router.app.repositories.SessionRepository
import cn.arorms.llm.router.common.requests.SessionRequest
import cn.arorms.llm.router.common.responses.SessionResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SessionService(
    private val repository: SessionRepository
) {
    @Transactional
    fun create(request: SessionRequest): SessionResponse {
        val session = Session(
            sessionId = UUID.randomUUID().toString().replace("-", ""),
            title = request.title,
            model = request.model,
            protocol = request.protocol,
            enabled = request.enabled
        )
        return repository.save(session).let(SessionMapper::toResponse)
    }

    @Transactional
    fun get(sessionId: String): SessionResponse = repository.findBySessionId(sessionId).orThrow().let(SessionMapper::toResponse)

    @Transactional
    fun list(): List<SessionResponse> = repository.findAll().sortedByDescending { it.updatedAt }.map(SessionMapper::toResponse)

    @Transactional
    fun delete(sessionId: String) = repository.findBySessionId(sessionId).orThrow().let(repository::delete)

    private fun <T> T?.orThrow(): T = this ?: throw NoSuchElementException("Session not found")
}
