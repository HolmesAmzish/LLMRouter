package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.ChatMessageStatus
import cn.arorms.llm.router.common.enums.Protocol
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Persistent chat history for web-based chat interfaces.
 */
@Entity
@Table(
    name = "chat_messages",
    indexes = [
        Index(name = "idx_chat_messages_conversation", columnList = "conversation_key,id"),
        Index(name = "idx_chat_messages_created_at", columnList = "created_at")
    ]
)
class ChatMessage(
    @Column(name = "conversation_key", nullable = false, length = 80)
    var conversationKey: String,
    @Column(nullable = false, length = 24)
    var role: String,
    @Column(nullable = false, columnDefinition = "text")
    var content: String,
    @Column(length = 200)
    var model: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    var protocol: Protocol? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var status: ChatMessageStatus = ChatMessageStatus.COMPLETED,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any?>? = emptyMap()
) : BaseEntity()
