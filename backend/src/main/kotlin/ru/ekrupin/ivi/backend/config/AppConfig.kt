package ru.ekrupin.ivi.backend.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val contract: ContractConfig,
    val database: DatabaseAppConfig,
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
)
