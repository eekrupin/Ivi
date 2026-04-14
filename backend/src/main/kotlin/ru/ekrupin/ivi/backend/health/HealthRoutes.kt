package ru.ekrupin.ivi.backend.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import ru.ekrupin.ivi.backend.config.AppConfig
import ru.ekrupin.ivi.backend.db.DatabaseFactory

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val contractVersion: String,
)

fun Route.registerHealthRoutes(appConfig: AppConfig, databaseFactory: DatabaseFactory) {
    route("/health") {
        get {
            val databaseHealth = databaseFactory.health()
            val httpStatus = if (databaseHealth.connected) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

            call.respond(
                status = httpStatus,
                message = HealthResponse(
                    status = if (databaseHealth.connected) "ok" else "degraded",
                    service = "ivi-backend",
                    contractVersion = "0.1.0",
                ),
            )
        }
    }
}
