package ru.ekrupin.ivi.data.pet.remote

import java.time.LocalDate
import java.time.LocalDateTime

data class RemotePetAccessPet(
    val id: String,
    val version: Long,
    val updatedAt: LocalDateTime,
    val name: String,
    val birthDate: LocalDate?,
    val createdAt: LocalDateTime,
)

data class RemotePetInvite(
    val code: String,
    val petId: String,
)

interface PetAccessRemoteDataSource {
    suspend fun getCurrentPet(baseUrl: String, accessToken: String): RemotePetAccessPet
    suspend fun createPet(baseUrl: String, accessToken: String, name: String, birthDate: LocalDate?): RemotePetAccessPet
    suspend fun createInvite(baseUrl: String, accessToken: String, petId: String): RemotePetInvite
    suspend fun acceptInvite(baseUrl: String, accessToken: String, code: String): RemotePetAccessPet
}
