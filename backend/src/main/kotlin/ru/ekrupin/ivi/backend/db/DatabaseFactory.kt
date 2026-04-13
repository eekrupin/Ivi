package ru.ekrupin.ivi.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.Closeable

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String,
    val maximumPoolSize: Int,
)

data class DatabaseHealth(
    val connected: Boolean,
    val schemaVersion: String?,
)

class DatabaseFactory(
    private val config: DatabaseConfig,
) : Closeable {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    private lateinit var dataSource: HikariDataSource
    private lateinit var flyway: Flyway
    private lateinit var database: Database

    fun initialize(): DatabaseFactory {
        dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                username = config.username
                password = config.password
                driverClassName = config.driverClassName
                maximumPoolSize = config.maximumPoolSize
                isAutoCommit = false
                validate()
            },
        )

        flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .locations("classpath:db/migration")
            .load()

        val migrationResult = flyway.migrate()
        database = Database.connect(dataSource)

        logger.info(
            "PostgreSQL connected at {}. Flyway current version={}, migrationsExecuted={}",
            config.jdbcUrl,
            flyway.info().current()?.version?.toString() ?: "none",
            migrationResult.migrationsExecuted,
        )

        return this
    }

    fun dbQuery(block: () -> Unit) {
        transaction(database) {
            block()
        }
    }

    fun <T> dbQueryResult(block: () -> T): T = transaction(database) {
        block()
    }

    fun health(): DatabaseHealth {
        return try {
            dbQueryResult { org.jetbrains.exposed.sql.transactions.TransactionManager.current() }
            DatabaseHealth(
                connected = true,
                schemaVersion = flyway.info().current()?.version?.toString(),
            )
        } catch (exception: Exception) {
            logger.error("Database health check failed", exception)
            DatabaseHealth(
                connected = false,
                schemaVersion = null,
            )
        }
    }

    override fun close() {
        runCatching { dataSource.close() }
    }
}
