package cn.arorms.llm.router.app.mappers

import cn.arorms.llm.router.app.entities.Session
import cn.arorms.llm.router.common.responses.SessionResponse

/**
 * Converts session persistence objects into API responses.
 */
object SessionMapper {
    fun toResponse(session: Session): SessionResponse = SessionResponse(
        id = session.sessionId,
        title = session.title,
        model = session.model,
        protocol = session.protocol,
        enabled = session.enabled,
        messageCount = session.messageCount,
        inputTokens = session.inputTokens,
        outputTokens = session.outputTokens,
        createdAt = session.createdAt?.toString(),
        updatedAt = session.updatedAt?.toString()
    )
}
