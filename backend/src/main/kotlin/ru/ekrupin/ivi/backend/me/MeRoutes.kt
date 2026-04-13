package ru.ekrupin.ivi.backend.me

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.common.http.respondStub

fun Route.registerMeRoutes() {
    route("/v1/me") {
        get {
            call.respondStub("Me handler is not implemented yet.")
        }
    }
}
