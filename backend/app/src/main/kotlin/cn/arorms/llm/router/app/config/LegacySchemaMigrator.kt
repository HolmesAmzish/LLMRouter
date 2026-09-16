package cn.arorms.llm.router.app.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Small bootstrap migration for databases created before independent model
 * pricing and provider-model bindings.
 */
@Component
class LegacySchemaMigrator(private val jdbcTemplate: JdbcTemplate) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        migrateOldModelCatalog()
        migrateApiKeys()
        migrateUsageRecords()
    }

    private fun migrateOldModelCatalog() {
        if (!tableExists("model_catalog") || !tableExists("models") || !tableExists("provider_models")) return

        jdbcTemplate.update(
            """
            INSERT INTO models
                (model_id, model_name, owned_by, enabled,
                 input_cost_per_million, output_cost_per_million, currency,
                 created_at, updated_at)
            SELECT
                mc.model_id,
                COALESCE(MIN(mc.display_name), mc.model_id),
                COALESCE(MIN(mc.owned_by), MIN(mc.provider)),
                true,
                MAX(mc.input_cost_per_million),
                MAX(mc.output_cost_per_million),
                'USD',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM model_catalog mc
            WHERE mc.model_id IS NOT NULL AND mc.model_id <> ''
            GROUP BY mc.model_id
            ON CONFLICT (model_id) DO NOTHING
            """.trimIndent()
        )

        jdbcTemplate.update(
            """
            INSERT INTO provider_models
                (provider_id, provider_name, model_id, model_name, model_definition_id, enabled,
                 created_at, updated_at)
            SELECT mc.account_id, mc.provider, mc.model_id,
                   COALESCE(mc.display_name, mc.model_id), m.id, mc.enabled,
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM model_catalog mc
            JOIN models m ON m.model_id = mc.model_id
            WHERE mc.account_id IS NOT NULL AND mc.model_id IS NOT NULL AND mc.model_id <> ''
            ON CONFLICT (provider_id, model_id) DO NOTHING
            """.trimIndent()
        )
    }

    private fun migrateApiKeys() {
        if (!tableExists("api_keys")) return
        jdbcTemplate.update("UPDATE api_keys SET key_type = 'GATEWAY' WHERE key_type IS NULL")
        jdbcTemplate.update("UPDATE api_keys SET spend = 0 WHERE spend IS NULL")
        jdbcTemplate.update("UPDATE api_keys SET models = '[]'::jsonb WHERE models IS NULL")
        jdbcTemplate.update("UPDATE api_keys SET metadata = '{}'::jsonb WHERE metadata IS NULL")
    }

    private fun migrateUsageRecords() {
        if (!tableExists("usage_records")) return
        jdbcTemplate.update(
            """
            UPDATE usage_records
            SET request_id = 'legacy-' || id::text
            WHERE request_id IS NULL
            """.trimIndent()
        )
        jdbcTemplate.update("UPDATE usage_records SET public_model = model WHERE public_model IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET upstream_model = model WHERE upstream_model IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET cache_read_tokens = 0 WHERE cache_read_tokens IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET cache_creation_tokens = 0 WHERE cache_creation_tokens IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET input_token_semantics = 'UNKNOWN' WHERE input_token_semantics IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET token_details = '{}'::jsonb WHERE token_details IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET data_source = 'UPSTREAM' WHERE data_source IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET is_streaming = false WHERE is_streaming IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET metadata = '{}'::jsonb WHERE metadata IS NULL")
    }

    private fun tableExists(name: String): Boolean = jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = ?
        """.trimIndent(),
        Long::class.java,
        name
    ) > 0
}
