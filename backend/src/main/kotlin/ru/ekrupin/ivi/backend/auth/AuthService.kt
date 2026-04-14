package ru.ekrupin.ivi.backend.auth

import io.ktor.http.HttpStatusCode
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.common.error.ErrorDetail
import ru.ekrupin.ivi.backend.db.model.UserRecord
import ru.ekrupin.ivi.backend.db.repository.RefreshTokenRepository
import ru.ekrupin.ivi.backend.db.repository.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
) {
    fun register(request: RegisterRequest): AuthResponse {
        validateRegisterRequest(request)
        if (userRepository.findByEmail(request.email.normalizeEmail()) != null) {
            throw ApiException(
                status = HttpStatusCode.Conflict,
                code = "email_exists",
                message = "Пользователь с таким email уже существует",
            )
        }

        val user = userRepository.create(
            email = request.email.normalizeEmail(),
            passwordHash = passwordHasher.hash(request.password),
            displayName = request.displayName.trim(),
        )
        return issueAuthResponse(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email.normalizeEmail())
            ?: throw invalidCredentials()

        if (!passwordHasher.verify(request.password, user.passwordHash)) {
            throw invalidCredentials()
        }

        return issueAuthResponse(user)
    }

    fun refresh(request: RefreshRequest): AuthResponse {
        if (request.refreshToken.isBlank()) {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_refresh_token", "Refresh token обязателен")
        }

        val tokenHash = tokenService.hashOpaqueToken(request.refreshToken)
        val refreshToken = refreshTokenRepository.findActiveByHash(tokenHash)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "invalid_refresh_token", "Refresh token недействителен")

        if (refreshToken.expiresAt.isBefore(java.time.Instant.now())) {
            refreshTokenRepository.revoke(tokenHash)
            throw ApiException(HttpStatusCode.Unauthorized, "refresh_token_expired", "Refresh token истек")
        }

        val user = userRepository.findById(refreshToken.userId)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "user_not_found", "Пользователь не найден")

        refreshTokenRepository.revoke(tokenHash)
        return issueAuthResponse(user)
    }

    private fun issueAuthResponse(user: UserRecord): AuthResponse {
        val accessToken = tokenService.createAccessToken(user.id, user.email)
        val refreshToken = tokenService.createRefreshToken()
        refreshTokenRepository.create(
            userId = user.id,
            tokenHash = tokenService.hashOpaqueToken(refreshToken),
            expiresAtEpochMillis = tokenService.refreshTokenExpiry().toEpochMilli(),
        )
        return AuthResponse(
            tokens = AuthTokenPairResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresInSeconds = tokenService.accessTokenTtlSeconds(),
            ),
            user = user.toUserProfileResponse(),
        )
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        val details = buildList {
            if (request.email.isBlank()) add(ErrorDetail(field = "email", issue = "Email обязателен"))
            if (request.password.length < 8) add(ErrorDetail(field = "password", issue = "Пароль должен быть не короче 8 символов"))
            if (request.displayName.isBlank()) add(ErrorDetail(field = "displayName", issue = "Имя обязательно"))
        }
        if (details.isNotEmpty()) {
            throw ApiException(
                status = HttpStatusCode.BadRequest,
                code = "invalid_register_request",
                message = "Некорректные данные регистрации",
                details = details,
            )
        }
    }

    private fun invalidCredentials(): ApiException = ApiException(
        status = HttpStatusCode.Unauthorized,
        code = "invalid_credentials",
        message = "Неверный email или пароль",
    )

    private fun String.normalizeEmail(): String = trim().lowercase()
}
