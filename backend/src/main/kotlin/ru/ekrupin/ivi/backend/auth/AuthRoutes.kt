package ru.ekrupin.ivi.backend.auth

import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.common.http.respondStub

fun Route.registerAuthRoutes() {
    route("/v1/auth") {
        post("/register") {
            call.receiveText()
            call.respondStub("Auth register handler is not implemented yet.")
        }
        post("/login") {
            call.receiveText()
            call.respondStub("Auth login handler is not implemented yet.")
        }
        post("/refresh") {
            call.receiveText()
            call.respondStub("Auth refresh handler is not implemented yet.")
        }
    }
}
