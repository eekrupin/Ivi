package ru.ekrupin.ivi.backend.invite

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.auth.AcceptInviteRequest
import ru.ekrupin.ivi.backend.auth.CreateInviteRequest
import ru.ekrupin.ivi.backend.auth.requireAuthenticatedUser
import java.util.UUID

fun Route.registerInviteRoutes(inviteService: InviteService) {
    route("/v1") {
        post("/pets/{petId}/invites") {
            val currentUser = call.requireAuthenticatedUser()
            val petId = UUID.fromString(call.parameters["petId"])
            val request = call.receive<CreateInviteRequest>()
            call.respond(HttpStatusCode.Created, inviteService.createInvite(petId, currentUser.userId, request))
        }
        post("/invites/accept") {
            val currentUser = call.requireAuthenticatedUser()
            val request = call.receive<AcceptInviteRequest>()
            call.respond(inviteService.acceptInvite(currentUser.userId, request))
        }
    }
}
