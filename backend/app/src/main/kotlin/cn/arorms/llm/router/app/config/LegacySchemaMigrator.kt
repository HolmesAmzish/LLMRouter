package cn.arorms.llm.router.app.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Small bootstrap migration for databases created before explicit model
 * deployments and LiteLLM-style virtual key fields were introduced.
 *
 * Hibernate's `update` mode cannot safely replace the old unique constraint or
 * backfill newly added columns, so this runner completes that transition.
 */
@Component
class LegacySchemaMigrator(private val jdbcTemplate: JdbcTemplate) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        dropOldModelUniqueConstraints()

        jdbcTemplate.update(
            """
            UPDATE model_catalog
            SET public_name = provider || '/' || model_id
            WHERE public_name IS NULL
            """.trimIndent()
        )
        jdbcTemplate.update(
            """
            UPDATE model_catalog
            SET upstream_model = model_id
            WHERE upstream_model IS NULL
            """.trimIndent()
        )

        jdbcTemplate.update("UPDATE api_keys SET key_type = 'GATEWAY' WHERE key_type IS NULL")
        jdbcTemplate.update("UPDATE api_keys SET spend = 0 WHERE spend IS NULL")
        jdbcTemplate.update("UPDATE api_keys SET models = '[]'::jsonb WHERE models IS NULL")
        jdbcTemplate.update("UPDATE api_keys SET metadata = '{}'::jsonb WHERE metadata IS NULL")

        jdbcTemplate.update(
            """
            UPDATE usage_records
            SET request_id = 'legacy-' || id::text
            WHERE request_id IS NULL
            """.trimIndent()
        )
        jdbcTemplate.update("UPDATE usage_records SET public_model = model WHERE public_model IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET upstream_model = model WHERE upstream_model IS NULL")
        jdbcTemplate.update("UPDATE usage_records SET metadata = '{}'::jsonb WHERE metadata IS NULL")
    }

    private fun dropOldModelUniqueConstraints() {
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON kcu.constraint_name = tc.constraint_name
             AND kcu.constraint_schema = tc.constraint_schema
             AND kcu.table_schema = tc.table_schema
             AND kcu.table_name = tc.table_name
            WHERE tc.table_name = 'model_catalog'
              AND tc.constraint_type = 'UNIQUE'
              AND tc.table_schema = current_schema()
            GROUP BY tc.constraint_name
            HAVING string_agg(kcu.column_name, ',' ORDER BY kcu.ordinal_position) = 'account_id,model_id'
            """.trimIndent(),
            String::class.java
        )
        constraints.forEach { constraint ->
            jdbcTemplate.execute("ALTER TABLE model_catalog DROP CONSTRAINT IF EXISTS \"$constraint\"")
        }
    }
}
