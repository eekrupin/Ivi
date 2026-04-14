package ru.ekrupin.ivi.backend

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.ekrupin.ivi.backend.auth.AuthService
import ru.ekrupin.ivi.backend.auth.PasswordHasher
import ru.ekrupin.ivi.backend.auth.TokenService
import ru.ekrupin.ivi.backend.auth.configureAuthentication
import ru.ekrupin.ivi.backend.common.error.configureErrorHandling
import ru.ekrupin.ivi.backend.config.AppConfig
import ru.ekrupin.ivi.backend.db.DatabaseConfig
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.repository.InviteRepository
import ru.ekrupin.ivi.backend.db.repository.EventTypeRepository
import ru.ekrupin.ivi.backend.db.repository.PetMembershipRepository
import ru.ekrupin.ivi.backend.db.repository.PetEventRepository
import ru.ekrupin.ivi.backend.db.repository.PetRepository
import ru.ekrupin.ivi.backend.db.repository.RefreshTokenRepository
import ru.ekrupin.ivi.backend.db.repository.UserRepository
import ru.ekrupin.ivi.backend.db.repository.WeightEntryRepository
import ru.ekrupin.ivi.backend.domain.PetDomainDataService
import ru.ekrupin.ivi.backend.invite.InviteService
import ru.ekrupin.ivi.backend.pet.PetAccessService
import ru.ekrupin.ivi.backend.routing.configureRouting

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val appConfig = AppConfig.from(environment.config)

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                explicitNulls = false
                ignoreUnknownKeys = true
            },
        )
    }
    install(StatusPages) {
        configureErrorHandling()
    }

    val tokenService = TokenService(appConfig.auth)
    configureAuthentication(tokenService)

    val databaseFactory = DatabaseFactory(
        config = DatabaseConfig(
            jdbcUrl = appConfig.database.jdbcUrl,
            username = appConfig.database.username,
            password = appConfig.database.password,
            driverClassName = appConfig.database.driverClassName,
            maximumPoolSize = appConfig.database.maximumPoolSize,
        ),
    ).initialize()

    monitor.subscribe(ApplicationStopped) {
        databaseFactory.close()
    }

    val userRepository = UserRepository(databaseFactory)
    val petRepository = PetRepository(databaseFactory)
    val petMembershipRepository = PetMembershipRepository(databaseFactory)
    val inviteRepository = InviteRepository(databaseFactory)
    val refreshTokenRepository = RefreshTokenRepository(databaseFactory)
    val eventTypeRepository = EventTypeRepository(databaseFactory)
    val petEventRepository = PetEventRepository(databaseFactory)
    val weightEntryRepository = WeightEntryRepository(databaseFactory)

    val dependencies = AppDependencies(
        authService = AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordHasher = PasswordHasher(),
            tokenService = tokenService,
        ),
        petAccessService = PetAccessService(
            userRepository = userRepository,
            petRepository = petRepository,
            petMembershipRepository = petMembershipRepository,
        ),
        inviteService = InviteService(
            petRepository = petRepository,
            petMembershipRepository = petMembershipRepository,
            inviteRepository = inviteRepository,
        ),
        petDomainDataService = PetDomainDataService(
            eventTypeRepository = eventTypeRepository,
            petEventRepository = petEventRepository,
            weightEntryRepository = weightEntryRepository,
        ),
        tokenService = tokenService,
    )

    configureRouting(appConfig, databaseFactory, dependencies)
}
