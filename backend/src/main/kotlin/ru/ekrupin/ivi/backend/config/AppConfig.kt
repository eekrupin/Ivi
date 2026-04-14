package ru.ekrupin.ivi.backend.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val contract: ContractConfig,
    val database: DatabaseAppConfig,
    val auth: AuthAppConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            contract = ContractConfig(
                openApiPath = config.property("ivi.contract.openApiPath").getString(),
            ),
            database = DatabaseAppConfig(
                jdbcUrl = config.property("ivi.database.jdbcUrl").getString(),
                username = config.property("ivi.database.username").getString(),
                password = config.property("ivi.database.password").getString(),
                driverClassName = config.property("ivi.database.driverClassName").getString(),
                maximumPoolSize = config.property("ivi.database.maximumPoolSize").getString().toInt(),
            ),
            auth = AuthAppConfig(
                jwtSecret = config.property("ivi.auth.jwtSecret").getString(),
                jwtIssuer = config.property("ivi.auth.jwtIssuer").getString(),
                jwtAudience = config.property("ivi.auth.jwtAudience").getString(),
                accessTtlSeconds = config.property("ivi.auth.accessTtlSeconds").getString().toLong(),
                refreshTtlSeconds = config.property("ivi.auth.refreshTtlSeconds").getString().toLong(),
            ),
        )
    }
}

data class ContractConfig(
    val openApiPath: String,
)

data class DatabaseAppConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String,
    val maximumPoolSize: Int,
)

data class AuthAppConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val accessTtlSeconds: Long,
    val refreshTtlSeconds: Long,
)
