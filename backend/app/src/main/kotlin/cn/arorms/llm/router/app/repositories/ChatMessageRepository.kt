package cn.arorms.llm.router.app.repositories

import cn.arorms.llm.router.app.entities.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    fun findByConversationKeyOrderByIdAsc(conversationKey: String): List<ChatMessage>
    fun deleteByConversationKey(conversationKey: String)
}
