package ru.ekrupin.ivi.backend.photo

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ru.ekrupin.ivi.backend.auth.requireAuthenticatedUser
import ru.ekrupin.ivi.backend.common.error.ApiException
import java.util.UUID

fun Route.registerPhotoRoutes(photoService: PhotoService) {
    route("/v1/pets") {
        get("/{petId}/photo") {
            val currentUser = call.requireAuthenticatedUser()
            val petId = call.requirePetId()
            val revision = call.request.queryParameters["revision"]
            val photo = photoService.downloadPhoto(currentUser.userId, petId, revision)
            call.respondBytes(
                bytes = photo.data,
                contentType = ContentType.parse(photo.contentType),
                status = HttpStatusCode.OK,
            )
        }
        put("/{petId}/photo") {
            val currentUser = call.requireAuthenticatedUser()
            val petId = call.requirePetId()
            val contentType = call.request.header(HttpHeaders.ContentType)
                ?.substringBefore(';')
                ?.trim()
                .orEmpty()
            val body = call.receive<ByteArray>()
            call.respond(photoService.uploadPhoto(currentUser.userId, petId, contentType, body))
        }
        delete("/{petId}/photo") {
            val currentUser = call.requireAuthenticatedUser()
            val petId = call.requirePetId()
            call.respond(photoService.deletePhoto(currentUser.userId, petId))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.requirePetId(): UUID {
    val rawPetId = parameters["petId"]
    return runCatching { UUID.fromString(rawPetId) }.getOrElse {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_pet_id", "Некорректный идентификатор питомца")
    }
}
