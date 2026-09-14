package cn.arorms.llm.router.app.service

import cn.arorms.llm.router.app.dto.Mappers
import cn.arorms.llm.router.app.repository.SessionRepository
import cn.arorms.llm.router.app.repository.UsageRecordRepository
import cn.arorms.llm.router.common.SessionRequest
import cn.arorms.llm.router.common.SessionResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SessionService(
    private val repository: SessionRepository,
    private val usageRepository: UsageRecordRepository
) {
    @Transactional
    fun create(request: SessionRequest): SessionResponse {
        val session = cn.arorms.llm.router.app.entity.SessionEntity(
            sessionId = UUID.randomUUID().toString().replace("-", ""),
            title = request.title,
            model = request.model,
            protocol = request.protocol,
            enabled = request.enabled
        )
        return repository.save(session).let(Mappers::toResponse)
    }

    @Transactional
    fun get(sessionId: String): SessionResponse = repository.findBySessionId(sessionId).orThrow().let(Mappers::toResponse)

    @Transactional
    fun list(): List<SessionResponse> = repository.findAll().sortedByDescending { it.updatedAt }.map { it.let(Mappers::toResponse) }

    @Transactional
    fun delete(sessionId: String) = repository.findBySessionId(sessionId).orThrow().let(repository::delete)

    private fun <T> T?.orThrow(): T = this ?: throw NoSuchElementException("Session not found")
}
