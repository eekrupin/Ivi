package ru.ekrupin.ivi.backend.sync

import io.ktor.http.HttpStatusCode
import ru.ekrupin.ivi.backend.common.error.ApiException
import java.time.Instant

data class SyncCursor(
    val source: String,
    val epochMillis: Long,
)

object SyncCursorCodec {
    fun bootstrapCursor(now: Instant = Instant.now()): String = encode("bootstrap", now)

    fun changesCursor(now: Instant = Instant.now()): String = encode("changes", now)

    fun decode(rawCursor: String): SyncCursor {
        val parts = rawCursor.split(':', limit = 2)
        if (parts.size != 2) throw invalidCursor()

        val source = parts[0]
        if (source != "bootstrap" && source != "changes") throw invalidCursor()

        val epochMillis = parts[1].toLongOrNull() ?: throw invalidCursor()
        if (epochMillis <= 0) throw invalidCursor()

        return SyncCursor(source = source, epochMillis = epochMillis)
    }

    private fun encode(source: String, now: Instant): String = "$source:${now.toEpochMilli()}"

    private fun invalidCursor(): ApiException = ApiException(
        status = HttpStatusCode.Conflict,
        code = "invalid_sync_cursor",
        message = "Cursor недействителен, требуется выполнить bootstrap заново",
    )
}
