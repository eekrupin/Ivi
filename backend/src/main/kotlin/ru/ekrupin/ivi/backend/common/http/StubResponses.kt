package ru.ekrupin.ivi.backend.common.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

@Serializable
data class StubResponse(
    val status: String = "stub",
    val message: String,
    val contractSource: String,
)

suspend fun ApplicationCall.respondStub(message: String) {
    respond(
        status = HttpStatusCode.NotImplemented,
        message = StubResponse(
            message = message,
            contractSource = "api/src/main.tsp",
        ),
    )
}
