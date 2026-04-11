package ru.ekrupin.ivi.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.ekrupin.ivi.data.repository.LocalEventTypeRepository
import ru.ekrupin.ivi.data.repository.LocalPetEventRepository
import ru.ekrupin.ivi.data.repository.LocalPetRepository
import ru.ekrupin.ivi.data.repository.LocalReminderSettingsRepository
import ru.ekrupin.ivi.data.repository.LocalWeightRepository
import ru.ekrupin.ivi.domain.repository.EventTypeRepository
import ru.ekrupin.ivi.domain.repository.PetEventRepository
import ru.ekrupin.ivi.domain.repository.PetRepository
import ru.ekrupin.ivi.domain.repository.ReminderSettingsRepository
import ru.ekrupin.ivi.domain.repository.WeightRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindPetRepository(impl: LocalPetRepository): PetRepository
    @Binds @Singleton abstract fun bindWeightRepository(impl: LocalWeightRepository): WeightRepository
    @Binds @Singleton abstract fun bindEventTypeRepository(impl: LocalEventTypeRepository): EventTypeRepository
    @Binds @Singleton abstract fun bindPetEventRepository(impl: LocalPetEventRepository): PetEventRepository
    @Binds @Singleton abstract fun bindReminderSettingsRepository(impl: LocalReminderSettingsRepository): ReminderSettingsRepository
}
