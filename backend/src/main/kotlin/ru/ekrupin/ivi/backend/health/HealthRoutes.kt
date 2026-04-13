package ru.ekrupin.ivi.backend.health

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import ru.ekrupin.ivi.backend.config.AppConfig

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val contractVersion: String,
    val contractSource: String,
)

fun Route.registerHealthRoutes(appConfig: AppConfig) {
    route("/health") {
        get {
            call.respond(
                HealthResponse(
                    status = "ok",
                    service = "ivi-backend",
                    contractVersion = "0.1.0",
                    contractSource = appConfig.contract.openApiPath,
                ),
            )
        }
    }
}
