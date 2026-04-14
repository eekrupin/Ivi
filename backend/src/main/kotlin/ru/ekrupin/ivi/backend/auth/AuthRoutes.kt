package ru.ekrupin.ivi.backend.auth

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.registerAuthRoutes(authService: AuthService) {
    route("/v1/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            call.respond(authService.register(request))
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            call.respond(authService.login(request))
        }
        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            call.respond(authService.refresh(request))
        }
    }
}
