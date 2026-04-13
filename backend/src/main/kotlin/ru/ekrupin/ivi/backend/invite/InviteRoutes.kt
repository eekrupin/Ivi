package ru.ekrupin.ivi.backend.invite

import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.common.http.respondStub

fun Route.registerInviteRoutes() {
    route("/v1") {
        post("/pets/{petId}/invites") {
            call.receiveText()
            call.respondStub("Create invite handler is not implemented yet.")
        }
        post("/invites/accept") {
            call.receiveText()
            call.respondStub("Accept invite handler is not implemented yet.")
        }
    }
}
