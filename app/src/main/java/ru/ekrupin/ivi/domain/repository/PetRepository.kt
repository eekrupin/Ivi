package ru.ekrupin.ivi.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.ekrupin.ivi.domain.model.Pet

interface PetRepository {
    fun observePet(): Flow<Pet?>

    suspend fun savePet(
        name: String,
        birthDate: java.time.LocalDate?,
        photoUri: String?,
    )
}
