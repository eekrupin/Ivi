package ru.ekrupin.ivi.backend.me

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.auth.requireAuthenticatedUser
import ru.ekrupin.ivi.backend.pet.PetAccessService

fun Route.registerMeRoutes(petAccessService: PetAccessService) {
    route("/v1/me") {
        get {
            val currentUser = call.requireAuthenticatedUser()
            call.respond(petAccessService.getMe(currentUser.userId))
        }
    }
}
