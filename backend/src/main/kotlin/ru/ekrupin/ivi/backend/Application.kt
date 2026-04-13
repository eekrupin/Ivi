package ru.ekrupin.ivi.backend

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.ekrupin.ivi.backend.common.error.ErrorResponse
import ru.ekrupin.ivi.backend.common.error.configureErrorHandling
import ru.ekrupin.ivi.backend.config.AppConfig
import ru.ekrupin.ivi.backend.db.DatabaseConfig
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.routing.configureRouting

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val appConfig = AppConfig.from(environment.config)

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                explicitNulls = false
                ignoreUnknownKeys = true
            },
        )
    }
    install(StatusPages) {
        configureErrorHandling()
    }

    val databaseFactory = DatabaseFactory(
        config = DatabaseConfig(
            jdbcUrl = appConfig.database.jdbcUrl,
            username = appConfig.database.username,
            password = appConfig.database.password,
        ),
    )
    databaseFactory.initialize()

    configureRouting(appConfig)
}
