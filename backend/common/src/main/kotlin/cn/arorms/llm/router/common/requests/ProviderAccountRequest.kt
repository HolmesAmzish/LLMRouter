package cn.arorms.llm.router.common.requests

import cn.arorms.llm.router.common.enums.Protocol

/**
 * Request payload for registering an upstream provider account.
 */
data class ProviderAccountRequest(
    val name: String,
    val protocol: Protocol,
    val baseUrl: String,
    val apiKey: String,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val weight: Int = 100,
    val balanceEndpoint: String? = null,
    val modelMapping: Map<String, String> = emptyMap(),
    val configuration: Map<String, String> = emptyMap()
)
