package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.Protocol
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * Conversation session used to associate and aggregate requests.
 */
@Entity
@Table(name = "router_sessions")
class Session(
    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    var sessionId: String,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, length = 200)
    var model: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var protocol: Protocol,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "message_count", nullable = false)
    var messageCount: Int = 0,

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Long = 0,

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Long = 0
) : BaseEntity()
