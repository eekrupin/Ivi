package ru.ekrupin.ivi.backend.pet

import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.common.http.respondStub

fun Route.registerPetRoutes() {
    route("/v1/pets") {
        post {
            call.receiveText()
            call.respondStub("Create pet handler is not implemented yet.")
        }
        get("/current") {
            call.respondStub("Current pet handler is not implemented yet.")
        }
    }
}
