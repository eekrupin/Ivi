package ru.ekrupin.ivi.backend.sync

import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.auth.requireAuthenticatedUser
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.common.http.respondStub

fun Route.registerSyncRoutes(syncBootstrapService: SyncBootstrapService, syncChangesService: SyncChangesService) {
    route("/v1/sync") {
        get("/bootstrap") {
            val currentUser = call.requireAuthenticatedUser()
            call.respond(syncBootstrapService.bootstrapForUser(currentUser.userId))
        }
        get("/changes") {
            val currentUser = call.requireAuthenticatedUser()
            val cursor = call.request.queryParameters["cursor"]
                ?: throw ApiException(
                    status = io.ktor.http.HttpStatusCode.BadRequest,
                    code = "missing_sync_cursor",
                    message = "Параметр cursor обязателен",
                )
            call.respond(syncChangesService.changesForUser(currentUser.userId, cursor))
        }
        post("/push") {
            call.receiveText()
            call.respondStub("Sync push handler is not implemented yet.")
        }
    }
}
