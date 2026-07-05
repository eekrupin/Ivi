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
        database.addColumnIfMissing(
            table = "sync_state",
            column = "requiresBootstrap",
            definition = "INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.addColumnIfMissing(
            table = "sync_state",
            column = "configuredBaseUrl",
            definition = "TEXT",
        )
        database.addColumnIfMissing(
            table = "sync_state",
            column = "configuredAccessToken",
            definition = "TEXT",
        )
        database.addColumnIfMissing(
            table = "sync_state",
            column = "lastForegroundSyncStartedAt",
            definition = "TEXT",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.normalizeSyncableTablesForV6()
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

private fun SupportSQLiteDatabase.normalizeSyncableTablesForV6() {
    execSQL("PRAGMA foreign_keys=OFF")
    normalizePetsTableForV6()
    normalizeEventTypesTableForV6()
    normalizeWeightEntriesTableForV6()
    normalizePetEventsTableForV6()
    execSQL("PRAGMA foreign_keys=ON")
}

private fun SupportSQLiteDatabase.normalizePetsTableForV6() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS pets_new (
            id INTEGER NOT NULL,
            name TEXT NOT NULL,
            birthDate TEXT,
            photoUri TEXT,
            createdAt TEXT NOT NULL,
            updatedAt TEXT NOT NULL,
            remoteId TEXT,
            serverVersion INTEGER,
            serverUpdatedAt TEXT,
            deletedAt TEXT,
            syncState TEXT NOT NULL,
            lastSyncedAt TEXT,
            PRIMARY KEY(id)
        )
        """.trimIndent(),
    )
    execSQL(
        """
        INSERT INTO pets_new (
            id, name, birthDate, photoUri, createdAt, updatedAt, remoteId, serverVersion,
            serverUpdatedAt, deletedAt, syncState, lastSyncedAt
        )
        SELECT
            id,
            name,
            birthDate,
            photoUri,
            createdAt,
            updatedAt,
            remoteId,
            serverVersion,
            serverUpdatedAt,
            deletedAt,
            COALESCE(syncState, 'SYNCED'),
            lastSyncedAt
        FROM pets
        """.trimIndent(),
    )
    execSQL("DROP TABLE pets")
    execSQL("ALTER TABLE pets_new RENAME TO pets")
}

private fun SupportSQLiteDatabase.normalizeEventTypesTableForV6() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS event_types_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            category TEXT NOT NULL,
            defaultDurationDays INTEGER,
            isActive INTEGER NOT NULL,
            colorArgb INTEGER,
            iconKey TEXT,
            createdAt TEXT NOT NULL,
            updatedAt TEXT NOT NULL,
            remoteId TEXT,
            serverVersion INTEGER,
            serverUpdatedAt TEXT,
            deletedAt TEXT,
            syncState TEXT NOT NULL,
            lastSyncedAt TEXT
        )
        """.trimIndent(),
    )
    execSQL(
        """
        INSERT INTO event_types_new (
            id, name, category, defaultDurationDays, isActive, colorArgb, iconKey, createdAt,
            updatedAt, remoteId, serverVersion, serverUpdatedAt, deletedAt, syncState, lastSyncedAt
        )
        SELECT
            id,
            name,
            category,
            defaultDurationDays,
            isActive,
            colorArgb,
            iconKey,
            createdAt,
            updatedAt,
            remoteId,
            serverVersion,
            serverUpdatedAt,
            deletedAt,
            COALESCE(syncState, 'SYNCED'),
            lastSyncedAt
        FROM event_types
        """.trimIndent(),
    )
    execSQL("DROP TABLE event_types")
    execSQL("ALTER TABLE event_types_new RENAME TO event_types")
}

private fun SupportSQLiteDatabase.normalizeWeightEntriesTableForV6() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS weight_entries_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            petId INTEGER NOT NULL,
            date TEXT NOT NULL,
            weightGrams INTEGER NOT NULL,
            comment TEXT,
            createdAt TEXT NOT NULL,
            updatedAt TEXT NOT NULL,
            remoteId TEXT,
            serverVersion INTEGER,
            serverUpdatedAt TEXT,
            deletedAt TEXT,
            syncState TEXT NOT NULL,
            lastSyncedAt TEXT,
            FOREIGN KEY(petId) REFERENCES pets(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    execSQL(
        """
        INSERT INTO weight_entries_new (
            id, petId, date, weightGrams, comment, createdAt, updatedAt, remoteId, serverVersion,
            serverUpdatedAt, deletedAt, syncState, lastSyncedAt
        )
        SELECT
            id,
            petId,
            date,
            weightGrams,
            comment,
            createdAt,
            COALESCE(updatedAt, createdAt),
            remoteId,
            serverVersion,
            serverUpdatedAt,
            deletedAt,
            COALESCE(syncState, 'SYNCED'),
            lastSyncedAt
        FROM weight_entries
        """.trimIndent(),
    )
    execSQL("DROP TABLE weight_entries")
    execSQL("ALTER TABLE weight_entries_new RENAME TO weight_entries")
    execSQL("CREATE INDEX IF NOT EXISTS index_weight_entries_petId ON weight_entries(petId)")
    execSQL("CREATE INDEX IF NOT EXISTS index_weight_entries_date ON weight_entries(date)")
}

private fun SupportSQLiteDatabase.normalizePetEventsTableForV6() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS pet_events_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            petId INTEGER NOT NULL,
            eventTypeId INTEGER NOT NULL,
            eventDate TEXT NOT NULL,
            dueDate TEXT,
            comment TEXT,
            notificationsEnabled INTEGER NOT NULL,
            status TEXT NOT NULL,
            createdAt TEXT NOT NULL,
            updatedAt TEXT NOT NULL,
            remoteId TEXT,
            serverVersion INTEGER,
            serverUpdatedAt TEXT,
            deletedAt TEXT,
            syncState TEXT NOT NULL,
            lastSyncedAt TEXT,
            FOREIGN KEY(petId) REFERENCES pets(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(eventTypeId) REFERENCES event_types(id) ON UPDATE NO ACTION ON DELETE RESTRICT
        )
        """.trimIndent(),
    )
    execSQL(
        """
        INSERT INTO pet_events_new (
            id, petId, eventTypeId, eventDate, dueDate, comment, notificationsEnabled, status,
            createdAt, updatedAt, remoteId, serverVersion, serverUpdatedAt, deletedAt, syncState, lastSyncedAt
        )
        SELECT
            id,
            petId,
            eventTypeId,
            eventDate,
            dueDate,
            comment,
            notificationsEnabled,
            status,
            createdAt,
            updatedAt,
            remoteId,
            serverVersion,
            serverUpdatedAt,
            deletedAt,
            COALESCE(syncState, 'SYNCED'),
            lastSyncedAt
        FROM pet_events
        """.trimIndent(),
    )
    execSQL("DROP TABLE pet_events")
    execSQL("ALTER TABLE pet_events_new RENAME TO pet_events")
    execSQL("CREATE INDEX IF NOT EXISTS index_pet_events_petId ON pet_events(petId)")
    execSQL("CREATE INDEX IF NOT EXISTS index_pet_events_eventTypeId ON pet_events(eventTypeId)")
    execSQL("CREATE INDEX IF NOT EXISTS index_pet_events_eventDate ON pet_events(eventDate)")
}

private fun SupportSQLiteDatabase.addColumnIfMissing(
    table: String,
    column: String,
    definition: String,
) {
    if (!hasColumn(table, column)) {
        execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
    }
}

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info($table)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}
