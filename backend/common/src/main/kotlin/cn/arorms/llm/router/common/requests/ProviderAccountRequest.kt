package cn.arorms.llm.router.common.requests

import cn.arorms.llm.router.common.enums.Protocol

/**
 * Request payload for registering one upstream provider with one or more
 * protocol-specific base URLs.
 */
data class ProviderAccountRequest(
    val name: String,
    val protocolEndpoints: Map<Protocol, String>,
    val apiKey: String,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val weight: Int = 100,
    val balanceEndpoint: String? = null,
    val modelMapping: Map<String, String> = emptyMap(),
    val configuration: Map<String, String> = emptyMap()
)
