package ru.ekrupin.ivi.backend.db

import org.slf4j.LoggerFactory

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)

class DatabaseFactory(
    private val config: DatabaseConfig,
) {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun initialize() {
        logger.info(
            "Database integration is not enabled yet. Prepared config for url={}, user={}",
            config.jdbcUrl,
            config.username,
        )
    }
}
