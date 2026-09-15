package cn.arorms.llm.router.app.entities

import cn.arorms.framework.common.domain.BaseEntity
import cn.arorms.llm.router.common.enums.Protocol
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

/**
 * One upstream provider account. Multiple API protocols can share one account
 * and each protocol has its own complete POST endpoint URL.
 */
@Entity
@Table(
    name = "provider_accounts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name"])]
)
class ProviderAccount(
    @Column(nullable = false, length = 120)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 32)
    var defaultProtocol: Protocol,

    @Column(nullable = false, length = 500)
    var baseUrl: String,

    @Column(nullable = false, length = 2000)
    var apiKey: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "protocol_endpoints", nullable = false, columnDefinition = "jsonb")
    var protocolEndpoints: Map<String, String> = emptyMap(),

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(nullable = false)
    var priority: Int = 100,

    @Column(nullable = false)
    var weight: Int = 100,

    @Column(name = "balance_endpoint", length = 500)
    var balanceEndpoint: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var modelMapping: Map<String, String> = emptyMap(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var configuration: Map<String, String> = emptyMap(),

    @Column(nullable = false, length = 40)
    var status: String = "unknown",

    var balance: Double? = null,

    @Column(length = 16)
    var currency: String? = null,

    @Column(name = "balance_checked_at")
    var balanceCheckedAt: OffsetDateTime? = null
) : BaseEntity() {
    fun supports(protocol: Protocol): Boolean =
        protocolEndpoints.containsKey(protocol.name) ||
            (protocolEndpoints.isEmpty() && protocol == defaultProtocol)

    fun endpointFor(protocol: Protocol): String? =
        protocolEndpoints[protocol.name]
            ?: baseUrl.takeIf { protocolEndpoints.isEmpty() && protocol == defaultProtocol }
}
