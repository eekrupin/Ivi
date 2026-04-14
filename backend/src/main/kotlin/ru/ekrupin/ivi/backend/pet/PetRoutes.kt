package ru.ekrupin.ivi.backend.pet

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.auth.CreatePetRequest
import ru.ekrupin.ivi.backend.auth.requireAuthenticatedUser

fun Route.registerPetRoutes(petAccessService: PetAccessService) {
    route("/v1/pets") {
        post {
            val currentUser = call.requireAuthenticatedUser()
            val request = call.receive<CreatePetRequest>()
            call.respond(HttpStatusCode.Created, petAccessService.createPet(currentUser.userId, request))
        }
        get("/current") {
            val currentUser = call.requireAuthenticatedUser()
            call.respond(petAccessService.getCurrentPet(currentUser.userId))
        }
    }
}
