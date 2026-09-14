package cn.arorms.llm.router.app.dto

import cn.arorms.llm.router.app.entity.ProviderAccountEntity
import cn.arorms.llm.router.app.entity.SessionEntity
import cn.arorms.llm.router.app.entity.UsageRecordEntity
import cn.arorms.llm.router.common.ProviderAccountResponse
import cn.arorms.llm.router.common.SessionResponse
import cn.arorms.llm.router.common.UsageResponse
import java.time.format.DateTimeFormatter

object Mappers {
    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun toResponse(account: ProviderAccountEntity): ProviderAccountResponse = ProviderAccountResponse(
        id = account.id ?: 0L,
        name = account.name,
        protocol = account.protocol,
        baseUrl = account.baseUrl,
        enabled = account.enabled,
        priority = account.priority,
        weight = account.weight,
        balanceEndpoint = account.balanceEndpoint,
        modelMapping = account.modelMapping,
        configuration = account.configuration,
        status = account.status,
        balance = account.balance,
        currency = account.currency,
        hasApiKey = account.apiKey.isNotBlank(),
        createdAt = account.createdAt?.toString(),
        updatedAt = account.updatedAt?.toString()
    )

    fun toResponse(session: SessionEntity): SessionResponse = SessionResponse(
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

    fun toResponse(record: UsageRecordEntity): UsageResponse = UsageResponse(
        id = record.id ?: 0L,
        sessionId = record.sessionId,
        provider = record.provider,
        accountName = record.accountName,
        model = record.model,
        protocol = record.protocol,
        inputTokens = record.inputTokens,
        outputTokens = record.outputTokens,
        totalTokens = record.totalTokens,
        costCents = record.costCents,
        latencyMs = record.latencyMs,
        status = record.status,
        createdAt = record.createdAt?.toString()
    )
}
