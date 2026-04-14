package ru.ekrupin.ivi.backend.routing

import io.ktor.server.auth.authenticate
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ru.ekrupin.ivi.backend.AppDependencies
import ru.ekrupin.ivi.backend.auth.registerAuthRoutes
import ru.ekrupin.ivi.backend.config.AppConfig
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.health.registerHealthRoutes
import ru.ekrupin.ivi.backend.invite.registerInviteRoutes
import ru.ekrupin.ivi.backend.me.registerMeRoutes
import ru.ekrupin.ivi.backend.pet.registerPetRoutes
import ru.ekrupin.ivi.backend.photo.registerPhotoRoutes
import ru.ekrupin.ivi.backend.sync.registerSyncRoutes

fun Application.configureRouting(appConfig: AppConfig, databaseFactory: DatabaseFactory, dependencies: AppDependencies) {
    routing {
        registerHealthRoutes(appConfig, databaseFactory)
        registerAuthRoutes(dependencies.authService)
        authenticate("auth-jwt") {
            registerMeRoutes(dependencies.petAccessService)
            registerPetRoutes(dependencies.petAccessService)
            registerInviteRoutes(dependencies.inviteService)
            registerSyncRoutes(dependencies.syncBootstrapService, dependencies.syncChangesService)
        }
        registerPhotoRoutes()
    }
}
