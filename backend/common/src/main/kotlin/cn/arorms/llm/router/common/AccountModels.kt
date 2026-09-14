package cn.arorms.llm.router.common

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

data class ProviderAccountPatch(
    val name: String? = null,
    val baseUrl: String? = null,
    val apiKey: String? = null,
    val enabled: Boolean? = null,
    val priority: Int? = null,
    val weight: Int? = null,
    val balanceEndpoint: String? = null,
    val modelMapping: Map<String, String>? = null,
    val configuration: Map<String, String>? = null
)

data class ProviderAccountResponse(
    val id: Long,
    val name: String,
    val protocol: Protocol,
    val baseUrl: String,
    val enabled: Boolean,
    val priority: Int,
    val weight: Int,
    val balanceEndpoint: String?,
    val modelMapping: Map<String, String>,
    val configuration: Map<String, String>,
    val status: String,
    val balance: Double?,
    val currency: String?,
    val hasApiKey: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

data class AccountBalanceResponse(
    val accountId: Long,
    val balance: Double?,
    val currency: String?,
    val status: String,
    val checkedAt: String
)

data class ModelListResponse(
    val provider: String,
    val objectValue: String = "list",
    val data: List<ModelResponse>
)

data class ModelResponse(
    val id: String,
    val ownedBy: String,
    val enabled: Boolean = true
)
