package ru.ekrupin.ivi.backend

import ru.ekrupin.ivi.backend.auth.AuthService
import ru.ekrupin.ivi.backend.auth.TokenService
import ru.ekrupin.ivi.backend.invite.InviteService
import ru.ekrupin.ivi.backend.pet.PetAccessService

data class AppDependencies(
    val authService: AuthService,
    val petAccessService: PetAccessService,
    val inviteService: InviteService,
    val tokenService: TokenService,
)
