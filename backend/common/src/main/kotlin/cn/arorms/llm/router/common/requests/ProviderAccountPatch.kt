package cn.arorms.llm.router.common.requests

import cn.arorms.llm.router.common.enums.Protocol

/**
 * Partial-update payload for an upstream provider account.
 */
data class ProviderAccountPatch(
    val name: String? = null,
    val protocol: Protocol? = null,
    val baseUrl: String? = null,
    val apiKey: String? = null,
    val enabled: Boolean? = null,
    val priority: Int? = null,
    val weight: Int? = null,
    val balanceEndpoint: String? = null,
    val modelMapping: Map<String, String>? = null,
    val configuration: Map<String, String>? = null
)
