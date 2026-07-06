package ru.ekrupin.ivi.data.sync

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.ekrupin.ivi.data.local.entity.EventTypeEntity
import ru.ekrupin.ivi.data.local.entity.PetEntity
import ru.ekrupin.ivi.data.local.entity.PetEventEntity
import ru.ekrupin.ivi.data.local.entity.WeightEntryEntity
import ru.ekrupin.ivi.domain.model.EventCategory
import ru.ekrupin.ivi.domain.model.PetEventStatus

class SyncPayloadFactoryTest {
    private val factory = SyncPayloadFactory()
    private val now = LocalDateTime.parse("2026-01-01T10:00:00")
    private val pet = PetEntity(
        id = 1,
        name = "Иви",
        birthDate = null,
        photoUri = null,
        photoRevision = null,
        createdAt = now,
        updatedAt = now,
        remoteId = "11111111-1111-1111-1111-111111111111",
    )

    @Test
    fun eventTypeUpsert_failsWhenEntityRemoteIdIsMissing() {
        val entity = eventType(remoteId = null)

        val exception = expectIllegalState {
            factory.eventTypeUpsert(entity, pet, now)
        }

        assertTrue(exception.message.orEmpty().contains("EventType"))
        assertTrue(exception.message.orEmpty().contains("local id 2"))
    }

    @Test
    fun eventTypeUpsert_failsWhenEntityRemoteIdIsBlank() {
        val entity = eventType(remoteId = " ")

        val exception = expectIllegalState {
            factory.eventTypeUpsert(entity, pet, now)
        }

        assertTrue(exception.message.orEmpty().contains("EventType"))
        assertTrue(exception.message.orEmpty().contains("local id 2"))
    }

    @Test
    fun eventTypeDelete_failsWhenEntityRemoteIdIsNotUuid() {
        val entity = eventType(remoteId = "event-type-2")

        val exception = expectIllegalState {
            factory.eventTypeDelete(entity, now)
        }

        assertTrue(exception.message.orEmpty().contains("EventType"))
        assertTrue(exception.message.orEmpty().contains("not a UUID"))
        assertTrue(exception.message.orEmpty().contains("local id 2"))
    }

    @Test
    fun eventTypeDelete_keepsValidRemoteId() {
        val remoteId = "22222222-2222-2222-2222-222222222222"
        val entity = eventType(remoteId = remoteId)

        val outbox = factory.eventTypeDelete(entity, now)

        assertEquals(remoteId, outbox.entityRemoteId)
    }

    @Test
    fun petEventUpsert_failsWhenEntityRemoteIdIsMissing() {
        val eventType = eventType(remoteId = "22222222-2222-2222-2222-222222222222")
        val entity = petEvent(eventType = eventType, remoteId = null)

        val exception = expectIllegalState {
            factory.petEventUpsert(entity, pet, eventType, now)
        }

        assertTrue(exception.message.orEmpty().contains("PetEvent"))
        assertTrue(exception.message.orEmpty().contains("local id 3"))
    }

    @Test
    fun petEventDelete_failsWhenEntityRemoteIdIsNotUuid() {
        val eventType = eventType(remoteId = "22222222-2222-2222-2222-222222222222")
        val entity = petEvent(eventType = eventType, remoteId = "pet-event-3")

        val exception = expectIllegalState {
            factory.petEventDelete(entity, now)
        }

        assertTrue(exception.message.orEmpty().contains("PetEvent"))
        assertTrue(exception.message.orEmpty().contains("not a UUID"))
        assertTrue(exception.message.orEmpty().contains("local id 3"))
    }

    @Test
    fun weightEntryUpsert_failsWhenEntityRemoteIdIsMissing() {
        val entity = WeightEntryEntity(
            id = 4,
            petId = pet.id,
            date = LocalDate.parse("2026-01-02"),
            weightGrams = 9400,
            comment = null,
            createdAt = now,
            remoteId = null,
        )

        val exception = expectIllegalState {
            factory.weightEntryUpsert(entity, pet, now)
        }

        assertTrue(exception.message.orEmpty().contains("WeightEntry"))
        assertTrue(exception.message.orEmpty().contains("local id 4"))
    }

    private fun eventType(remoteId: String?) = EventTypeEntity(
        id = 2,
        name = "Вакцинация",
        category = EventCategory.VACCINATION,
        defaultDurationDays = 365,
        isActive = true,
        colorArgb = null,
        iconKey = null,
        createdAt = now,
        updatedAt = now,
        remoteId = remoteId,
    )

    private fun petEvent(eventType: EventTypeEntity, remoteId: String?) = PetEventEntity(
        id = 3,
        petId = pet.id,
        eventTypeId = eventType.id,
        eventDate = LocalDate.parse("2026-01-02"),
        dueDate = null,
        comment = null,
        notificationsEnabled = true,
        status = PetEventStatus.ACTIVE,
        createdAt = now,
        updatedAt = now,
        remoteId = remoteId,
    )

    private fun expectIllegalState(block: () -> Unit): IllegalStateException {
        return try {
            block()
            fail("Expected IllegalStateException")
            error("unreachable")
        } catch (exception: IllegalStateException) {
            exception
        }
    }
}
