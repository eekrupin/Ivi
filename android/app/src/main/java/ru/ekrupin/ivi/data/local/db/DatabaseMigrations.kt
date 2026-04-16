package ru.ekrupin.ivi.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE pets ADD COLUMN remoteId TEXT")
        database.execSQL("ALTER TABLE pets ADD COLUMN serverVersion INTEGER")
        database.execSQL("ALTER TABLE pets ADD COLUMN serverUpdatedAt TEXT")
        database.execSQL("ALTER TABLE pets ADD COLUMN deletedAt TEXT")
        database.execSQL("ALTER TABLE pets ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
        database.execSQL("ALTER TABLE pets ADD COLUMN lastSyncedAt TEXT")

        database.execSQL("ALTER TABLE event_types ADD COLUMN remoteId TEXT")
        database.execSQL("ALTER TABLE event_types ADD COLUMN serverVersion INTEGER")
        database.execSQL("ALTER TABLE event_types ADD COLUMN serverUpdatedAt TEXT")
        database.execSQL("ALTER TABLE event_types ADD COLUMN deletedAt TEXT")
        database.execSQL("ALTER TABLE event_types ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
        database.execSQL("ALTER TABLE event_types ADD COLUMN lastSyncedAt TEXT")

        database.execSQL("ALTER TABLE pet_events ADD COLUMN remoteId TEXT")
        database.execSQL("ALTER TABLE pet_events ADD COLUMN serverVersion INTEGER")
        database.execSQL("ALTER TABLE pet_events ADD COLUMN serverUpdatedAt TEXT")
        database.execSQL("ALTER TABLE pet_events ADD COLUMN deletedAt TEXT")
        database.execSQL("ALTER TABLE pet_events ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
        database.execSQL("ALTER TABLE pet_events ADD COLUMN lastSyncedAt TEXT")

        database.execSQL("ALTER TABLE weight_entries ADD COLUMN updatedAt TEXT")
        database.execSQL("UPDATE weight_entries SET updatedAt = createdAt WHERE updatedAt IS NULL")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN remoteId TEXT")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN serverVersion INTEGER")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN serverUpdatedAt TEXT")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN deletedAt TEXT")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'")
        database.execSQL("ALTER TABLE weight_entries ADD COLUMN lastSyncedAt TEXT")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_outbox (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityType TEXT NOT NULL,
                entityLocalId INTEGER NOT NULL,
                entityRemoteId TEXT NOT NULL,
                operation TEXT NOT NULL,
                payloadJson TEXT,
                baseVersion INTEGER,
                clientMutationId TEXT NOT NULL,
                status TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_status ON sync_outbox(status)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_entityType_entityLocalId ON sync_outbox(entityType, entityLocalId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_entityRemoteId ON sync_outbox(entityRemoteId)")

        seedRemoteIds(database, table = "pets")
        seedRemoteIds(database, table = "event_types")
        seedRemoteIds(database, table = "pet_events")
        seedRemoteIds(database, table = "weight_entries")
    }

    private fun seedRemoteIds(database: SupportSQLiteDatabase, table: String) {
        database.execSQL(
            """
            UPDATE $table
            SET remoteId = lower(
                hex(randomblob(4)) || '-' ||
                hex(randomblob(2)) || '-' ||
                '4' || substr(hex(randomblob(2)), 2) || '-' ||
                substr('89ab', abs(random()) % 4 + 1, 1) || substr(hex(randomblob(2)), 2) || '-' ||
                hex(randomblob(6))
            )
            WHERE remoteId IS NULL
            """.trimIndent(),
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_users (
                remoteId TEXT NOT NULL PRIMARY KEY,
                email TEXT NOT NULL,
                displayName TEXT NOT NULL,
                serverVersion INTEGER NOT NULL,
                serverUpdatedAt TEXT NOT NULL,
                deletedAt TEXT,
                createdAt TEXT NOT NULL,
                lastSyncedAt TEXT NOT NULL
            )
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_pet_memberships (
                remoteId TEXT NOT NULL PRIMARY KEY,
                petRemoteId TEXT NOT NULL,
                userRemoteId TEXT NOT NULL,
                role TEXT NOT NULL,
                status TEXT NOT NULL,
                serverVersion INTEGER NOT NULL,
                serverUpdatedAt TEXT NOT NULL,
                deletedAt TEXT,
                createdAt TEXT NOT NULL,
                lastSyncedAt TEXT NOT NULL
            )
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_state (
                id INTEGER NOT NULL PRIMARY KEY,
                cursor TEXT,
                lastBootstrapAt TEXT,
                lastChangesAt TEXT,
                lastSuccessfulReadAt TEXT,
                requiresBootstrap INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sync_state ADD COLUMN requiresBootstrap INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sync_state ADD COLUMN configuredBaseUrl TEXT")
        database.execSQL("ALTER TABLE sync_state ADD COLUMN configuredAccessToken TEXT")
        database.execSQL("ALTER TABLE sync_state ADD COLUMN lastForegroundSyncStartedAt TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_conflicts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityType TEXT NOT NULL,
                entityLocalId INTEGER NOT NULL,
                entityRemoteId TEXT NOT NULL,
                clientMutationId TEXT NOT NULL,
                reason TEXT NOT NULL,
                serverVersion INTEGER NOT NULL,
                serverRecordJson TEXT,
                conflictedAt TEXT NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_conflicts_entityType_entityLocalId ON sync_conflicts(entityType, entityLocalId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_conflicts_clientMutationId ON sync_conflicts(clientMutationId)")
    }
}
