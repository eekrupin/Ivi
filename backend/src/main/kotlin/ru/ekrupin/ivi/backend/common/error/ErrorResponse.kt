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
    val details: List<ErrorDetail> = emptyList(),
)

@Serializable
data class ErrorDetail(
    val field: String? = null,
    val issue: String,
)

@Serializable
data class ErrorEnvelope(
    val error: ErrorResponse,
)

@Serializable
data class ConflictItem(
    val entityType: String? = null,
    val entityId: String? = null,
    val reason: String? = null,
)

@Serializable
data class ConflictEnvelope(
    val error: ErrorResponse,
    val conflicts: List<ConflictItem> = emptyList(),
)

class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val details: List<ErrorDetail> = emptyList(),
    val conflicts: List<ConflictItem> = emptyList(),
) : RuntimeException(message)

fun StatusPagesConfig.configureErrorHandling() {
    exception<ApiException> { call, cause ->
        call.respondError(
            status = cause.status,
            code = cause.code,
            message = cause.message,
            details = cause.details,
            conflicts = cause.conflicts,
        )
    }
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
    details: List<ErrorDetail> = emptyList(),
    conflicts: List<ConflictItem> = emptyList(),
) {
    val error = ErrorResponse(code = code, message = message, details = details)
    if (status == HttpStatusCode.Conflict) {
        respond(status, ConflictEnvelope(error = error, conflicts = conflicts))
    } else {
        respond(status, ErrorEnvelope(error = error))
    }
}
