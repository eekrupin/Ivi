package ru.ekrupin.ivi.data.sync.model

enum class SyncState {
    SYNCED,
    PENDING_UPLOAD,
    CONFLICT,
}

enum class SyncEntityType {
    EVENT_TYPE,
    PET_EVENT,
    WEIGHT_ENTRY,
}

enum class SyncOperation {
    UPSERT,
    DELETE,
}

enum class SyncOutboxStatus {
    PENDING,
    IN_FLIGHT,
    FAILED,
}
