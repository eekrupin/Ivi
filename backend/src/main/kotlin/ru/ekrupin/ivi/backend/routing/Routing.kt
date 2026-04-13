package ru.ekrupin.ivi.backend.routing

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ru.ekrupin.ivi.backend.auth.registerAuthRoutes
import ru.ekrupin.ivi.backend.config.AppConfig
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.health.registerHealthRoutes
import ru.ekrupin.ivi.backend.invite.registerInviteRoutes
import ru.ekrupin.ivi.backend.me.registerMeRoutes
import ru.ekrupin.ivi.backend.pet.registerPetRoutes
import ru.ekrupin.ivi.backend.photo.registerPhotoRoutes
import ru.ekrupin.ivi.backend.sync.registerSyncRoutes

fun Application.configureRouting(appConfig: AppConfig, databaseFactory: DatabaseFactory) {
    routing {
        registerHealthRoutes(appConfig, databaseFactory)
        registerAuthRoutes()
        registerMeRoutes()
        registerPetRoutes()
        registerInviteRoutes()
        registerSyncRoutes()
        registerPhotoRoutes()
    }
}
