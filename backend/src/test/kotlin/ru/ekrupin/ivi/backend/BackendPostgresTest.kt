package ru.ekrupin.ivi.backend

import org.jetbrains.exposed.sql.deleteAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import ru.ekrupin.ivi.backend.db.DatabaseConfig
import ru.ekrupin.ivi.backend.db.DatabaseFactory
import ru.ekrupin.ivi.backend.db.schema.EventTypesTable
import ru.ekrupin.ivi.backend.db.schema.InvitesTable
import ru.ekrupin.ivi.backend.db.schema.PetEventsTable
import ru.ekrupin.ivi.backend.db.schema.PetMembershipsTable
import ru.ekrupin.ivi.backend.db.schema.PetPhotosTable
import ru.ekrupin.ivi.backend.db.schema.PetsTable
import ru.ekrupin.ivi.backend.db.schema.RefreshTokensTable
import ru.ekrupin.ivi.backend.db.schema.UsersTable
import ru.ekrupin.ivi.backend.db.schema.WeightEntriesTable

abstract class BackendPostgresTest {
    protected lateinit var databaseFactory: DatabaseFactory

    @BeforeEach
    fun cleanDatabase() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker недоступен для backend PostgreSQL tests")
        if (!postgres.isRunning) {
            postgres.start()
        }
        databaseFactory = sharedDatabaseFactory
        databaseFactory.dbQuery {
            WeightEntriesTable.deleteAll()
            PetEventsTable.deleteAll()
            EventTypesTable.deleteAll()
            PetPhotosTable.deleteAll()
            InvitesTable.deleteAll()
            PetMembershipsTable.deleteAll()
            PetsTable.deleteAll()
            RefreshTokensTable.deleteAll()
            UsersTable.deleteAll()
        }
    }

    companion object {
        @JvmField
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        private val sharedDatabaseFactory: DatabaseFactory by lazy {
            DatabaseFactory(
                DatabaseConfig(
                    jdbcUrl = postgres.jdbcUrl,
                    username = postgres.username,
                    password = postgres.password,
                    driverClassName = postgres.driverClassName,
                    maximumPoolSize = 4,
                ),
            ).initialize()
        }
    }
}
