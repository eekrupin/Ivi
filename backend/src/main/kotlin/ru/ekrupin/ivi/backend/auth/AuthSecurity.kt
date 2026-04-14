package ru.ekrupin.ivi.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import ru.ekrupin.ivi.backend.common.error.ApiException
import ru.ekrupin.ivi.backend.config.AuthAppConfig
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.random.Random

class PasswordHasher {
    fun hash(rawPassword: String): String = org.mindrot.jbcrypt.BCrypt.hashpw(rawPassword, org.mindrot.jbcrypt.BCrypt.gensalt())

    fun verify(rawPassword: String, passwordHash: String): Boolean = org.mindrot.jbcrypt.BCrypt.checkpw(rawPassword, passwordHash)
}

class TokenService(
    private val config: AuthAppConfig,
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun createAccessToken(userId: UUID, email: String): String {
        val now = Instant.now()
        return JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(config.accessTtlSeconds)))
            .sign(algorithm)
    }

    fun createRefreshToken(): String {
        val bytes = ByteArray(32)
        Random.Default.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun verifier() = JWT.require(algorithm)
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .build()

    fun accessTokenTtlSeconds(): Int = config.accessTtlSeconds.toInt()

    fun refreshTokenExpiry(): Instant = Instant.now().plusSeconds(config.refreshTtlSeconds)

    fun hashOpaqueToken(token: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}

data class AuthenticatedUserPrincipal(
    val userId: UUID,
    val email: String,
)

fun Application.configureAuthentication(tokenService: TokenService) {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(tokenService.verifier())
            validate { credential ->
                val subject = credential.payload.subject ?: return@validate null
                val email = credential.payload.getClaim("email")?.asString() ?: return@validate null
                val userId = runCatching { UUID.fromString(subject) }.getOrNull() ?: return@validate null
                AuthenticatedUserPrincipal(userId = userId, email = email)
            }
            challenge { _, _ ->
                throw ApiException(
                    status = HttpStatusCode.Unauthorized,
                    code = "unauthorized",
                    message = "Требуется авторизация",
                )
            }
        }
    }
}

fun io.ktor.server.application.ApplicationCall.requireAuthenticatedUser(): AuthenticatedUserPrincipal {
    return principal<AuthenticatedUserPrincipal>()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "unauthorized", "Требуется авторизация")
}
