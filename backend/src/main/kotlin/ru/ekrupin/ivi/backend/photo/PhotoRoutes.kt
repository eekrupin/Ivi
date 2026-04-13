package ru.ekrupin.ivi.backend.photo

import io.ktor.server.request.receiveChannel
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.common.http.respondStub

fun Route.registerPhotoRoutes() {
    route("/v1/pets") {
        put("/{petId}/photo") {
            call.receiveChannel()
            call.respondStub("Photo upload handler is not implemented yet.")
        }
        delete("/{petId}/photo") {
            call.respondStub("Photo delete handler is not implemented yet.")
        }
    }
}
