package ru.ekrupin.ivi.backend.common.error

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)

fun StatusPagesConfig.configureErrorHandling() {
    exception<Throwable> { call, cause ->
        call.respondError(
            status = HttpStatusCode.InternalServerError,
            code = "internal_error",
            message = cause.message ?: "Internal server error",
        )
    }
}

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String,
) {
    respond(status, ErrorResponse(code = code, message = message))
}
