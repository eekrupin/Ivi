package ru.ekrupin.ivi.backend.sync

import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.common.http.respondStub

fun Route.registerSyncRoutes() {
    route("/v1/sync") {
        get("/bootstrap") {
            call.respondStub("Sync bootstrap handler is not implemented yet.")
        }
        get("/changes") {
            call.respondStub("Sync changes handler is not implemented yet.")
        }
        post("/push") {
            call.receiveText()
            call.respondStub("Sync push handler is not implemented yet.")
        }
    }
}
